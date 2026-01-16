package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"smsconsumer/internal/db"
	"smsconsumer/internal/handlers"
	"smsconsumer/internal/kafka"
	"smsconsumer/internal/repositories"
	"smsconsumer/internal/services"
)

func main() {
	ctx1, cancel1 := context.WithCancel(context.Background())
	ctx2, cancel2 := context.WithCancel(context.Background())
	defer cancel1()
	defer cancel2()

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, os.Interrupt, syscall.SIGTERM)

	mongoURI := os.Getenv("MONGO_URI")
	if mongoURI == "" {
		mongoURI = "mongodb://localhost:27017"
	}

	kafkaBrokersEnv := os.Getenv("KAFKA_BROKERS")
	if kafkaBrokersEnv == "" {
		kafkaBrokersEnv = "kafka:9092"
	}
	brokers := strings.Split(kafkaBrokersEnv, ",")

	client, err := db.ConnectMongo(mongoURI)
	if err != nil {
		log.Fatalf("mongo connect error: %v", err)
	}
	defer func() {
		_ = client.Disconnect(context.Background())
	}()

	database := client.Database("smsdb")
	repo := repositories.NewSmsRepository(database)
	repo2 := repositories.NewUserRepository(database)
	svc := services.NewSmsService(repo)
	svc2 := services.NewUserService(repo2)
	handler := handlers.NewSmsHandler(svc)
	userHandler := handlers.NewUserHandler(svc2)

	smsConsumer := kafka.NewSmsConsumer(brokers, "sms-topic", "sms-store-group-v2", svc)
	userConsumer := kafka.NewUserConsumer(brokers, "user-topic", "user-store-group-v2", svc2)
	smsConsumer.SmsStart(ctx1)
	userConsumer.UserStart(ctx2)
	defer func() {
		if err := smsConsumer.Close(); err != nil {
			log.Printf("smsConsumer close error: %v", err)
		}
		if err := userConsumer.Close(); err != nil {
			log.Printf("userConsumer close error: %v", err)
		}
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("/v1/user/", handler.GetUserMessages)
	mux.HandleFunc("/v1/users/", userHandler.GetUsersByStatus)

	server := &http.Server{
		Addr:         ":8081",
		Handler:      mux,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	go func() {
		log.Println("SMS Store running on :8081")
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("listen: %v", err)
		}
	}()

	<-sigs
	log.Println("shutdown initiated")

	cancel1()
	cancel2()

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer shutdownCancel()
	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("server shutdown error: %v", err)
	}

	log.Println("shutdown complete")
}
package kafka

import (
	"context"
	"encoding/json"
	"log"
	"smsconsumer/internal/models"
	"smsconsumer/internal/services"
	"time"

	"github.com/segmentio/kafka-go"
)

type SmsConsumer struct {
	reader *kafka.Reader
	svc    *services.SmsService
	topic  string
}

type UserConsumer struct {
	reader *kafka.Reader
	svc    *services.UserService
	topic  string
}

type SmsEvent struct {
	MobileNumber string  `json:"mobileNumber"`
	Message      string  `json:"message"`
	Status       string  `json:"status"`
}

type UserEvent struct {
	MobileNumber string  `json:"mobileNumber"`
	Status       string  `json:"status"`
}

func NewSmsConsumer(brokers[]string, topic string, groupID string, svc *services.SmsService) *SmsConsumer {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:      brokers,
		Topic:        topic,
		GroupID:      groupID,
		StartOffset:  kafka.FirstOffset,
		MinBytes:     1,
		MaxBytes:     10e6,
		MaxWait:      1 * time.Second,
	})
	return &SmsConsumer{
		reader: r,
		svc: svc,
		topic: topic,
	}
}

func NewUserConsumer(brokers[]string, topic string, groupID string, svc *services.UserService) *UserConsumer {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:      brokers,
		Topic:        topic,
		GroupID:      groupID,
		StartOffset:  kafka.FirstOffset,
		MinBytes:     1,
		MaxBytes:     10e6,
		MaxWait:      1 * time.Second,
	})
	return &UserConsumer{
		reader: r,
		svc: svc,
		topic: topic,
	}
}

func (c *SmsConsumer) Close() error {
	return c.reader.Close()
}

func (c *UserConsumer) Close() error {
	return c.reader.Close()
}

func (c *SmsConsumer) SmsStart(ctx context.Context) {
	log.Printf("kafka consumer starting, topic=%s", c.topic)
	go func() {
		for {
			m, err := c.reader.FetchMessage(ctx)
			if err != nil {
				if ctx.Err() != nil {
					log.Printf("kafka consumer context closed: %v", ctx.Err())
					return
				}
				log.Printf("kafka fetch error: %v", err)
				time.Sleep(time.Second)
				continue
			}

			var ev SmsEvent
			if err := json.Unmarshal(m.Value, &ev); err != nil {
				log.Printf("failed to unmarshal kafka message: %v; msg=%s", err, string(m.Value))
				if err := c.reader.CommitMessages(ctx, m); err != nil {
					log.Printf("failed to commit message: %v", err)
				}
				continue
			}

			rec := models.SmsRecord{
				MobileNumber: ev.MobileNumber,
				Message:      ev.Message,
				Status:       ev.Status,
			}

			if _, err := c.svc.SaveSMS(ctx, rec); err != nil {
				log.Printf("failed to save sms record for mobile=%s : %v", ev.MobileNumber, err)
				time.Sleep(time.Second)
				continue
			}

			if err := c.reader.CommitMessages(ctx, m); err != nil {
				log.Printf("failed to commit message: %v", err)
			} else {
				log.Printf("processed kafka message for mobile=%s partition=%d offset=%d", ev.MobileNumber, m.Partition, m.Offset)
			}
		}
	}()
}

func (c *UserConsumer) UserStart(ctx context.Context) {
	log.Printf("kafka consumer starting, topic=%s", c.topic)
	go func() {
		for {
			m, err := c.reader.FetchMessage(ctx)
			if err != nil {
				if ctx.Err() != nil {
					log.Printf("kafka consumer context closed: %v", ctx.Err())
					return
				}
				log.Printf("kafka fetch error: %v", err)
				time.Sleep(time.Second)
				continue
			}

			var ev UserEvent
			if err := json.Unmarshal(m.Value, &ev); err != nil {
				log.Printf("failed to unmarshal kafka message: %v; msg=%s", err, string(m.Value))
				if err := c.reader.CommitMessages(ctx, m); err != nil {
					log.Printf("failed to commit message: %v", err)
				}
				continue
			}

			if err := c.svc.UpdateUserStatus(ctx, ev.MobileNumber, ev.Status); err != nil {
				log.Printf("failed to save sms record for mobile=%s : %v", ev.MobileNumber, err)
				time.Sleep(time.Second)
				continue
			}

			if err := c.reader.CommitMessages(ctx, m); err != nil {
				log.Printf("failed to commit message: %v", err)
			} else {
				log.Printf("processed kafka message for mobile=%s partition=%d offset=%d", ev.MobileNumber, m.Partition, m.Offset)
			}
		}
	}()
}
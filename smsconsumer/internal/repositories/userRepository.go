package repositories

import (
	"context"
	"smsconsumer/internal/models"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type UserRepository struct {
	collection *mongo.Collection
}

func NewUserRepository(db *mongo.Database) *UserRepository {
	return &UserRepository{
		collection: db.Collection("users"),
	}
}

func (r *UserRepository) UpdateUserStatus(ctx context.Context, mobileNumber string, status string) error {
	filter := bson.M{"mobile_number": mobileNumber}
	update := bson.M{
		"$set": bson.M{
			"status":    status,
		},
	}
	opts := options.Update().SetUpsert(true)
	_, err := r.collection.UpdateOne(ctx, filter, update, opts)
	return err
}

func (r *UserRepository) GetByStatus(ctx context.Context, status string) ([]models.UserRecord, error) {
    filter := bson.M{"status": status}

	cursor, err := r.collection.Find(ctx, filter)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var records []models.UserRecord
	if err := cursor.All(ctx, &records); err != nil {
		return nil, err
	}

	return records, nil
}
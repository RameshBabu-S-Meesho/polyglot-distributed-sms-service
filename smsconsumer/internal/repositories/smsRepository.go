package repositories

import (
	"context"
	"smsconsumer/internal/models"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type SmsRepository struct {
	collection *mongo.Collection
}

func NewSmsRepository(db *mongo.Database) *SmsRepository {
	return &SmsRepository{
		collection: db.Collection("messages"),
	}
}

func (r *SmsRepository) GetByMobileNumber(ctx context.Context, mobileNumber string) ([]models.SmsRecord, error) {
	filter := bson.M{"mobile_number": mobileNumber}

	cursor, err := r.collection.Find(ctx, filter)
	if err != nil {
		return nil, err
	}
	defer cursor.Close(ctx)

	var records []models.SmsRecord
	if err := cursor.All(ctx, &records); err != nil {
		return nil, err
	}

	return records, nil
}

func (r *SmsRepository) Insert(ctx context.Context, rec models.SmsRecord) (primitive.ObjectID, error) {
	res, err := r.collection.InsertOne(ctx, rec)
	if err != nil {
		return primitive.NilObjectID, err
	}
	oid, ok := res.InsertedID.(primitive.ObjectID)
	if !ok {
		return primitive.NilObjectID, nil
	}
	return oid, nil
}
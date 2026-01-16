package models

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SmsRecord struct {
	ID             primitive.ObjectID    `bson:"_id,omitempty" json:"id"`
	MobileNumber   string                `bson:"mobile_number" json:"mobileNumber"`
	Message        string                `bson:"message" json:"message"`
	Status         string                `bson:"status" json:"status"`
}

type UserRecord struct {
    MobileNumber   string                `bson:"mobile_number" json:"mobileNumber"`
    Status         string                `bson:"status" json:"status"`
}
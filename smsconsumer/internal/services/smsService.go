package services

import (
	"context"
	"smsconsumer/internal/models"
	"smsconsumer/internal/repositories"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SmsService struct {
	repo *repositories.SmsRepository
}

func NewSmsService(repo *repositories.SmsRepository) *SmsService {
	return &SmsService{repo: repo}
}

func (s *SmsService) GetUserMessages(
	ctx context.Context,
	mobileNumber string,
) ([]models.SmsRecord, error) {

	return s.repo.GetByMobileNumber(ctx, mobileNumber)
}

func (s *SmsService) SaveSMS(ctx context.Context, rec models.SmsRecord) (primitive.ObjectID, error) {
	return s.repo.Insert(ctx, rec)
}
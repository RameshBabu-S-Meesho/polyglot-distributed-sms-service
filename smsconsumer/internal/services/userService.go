package services

import (
	"context"
	"smsconsumer/internal/models"
	"smsconsumer/internal/repositories"
)

type UserService struct {
	repo *repositories.UserRepository
}

func NewUserService(repo *repositories.UserRepository) *UserService {
	return &UserService{repo: repo}
}

func (s *UserService) GetByStatus(
	ctx context.Context,
	status string,
) ([]models.UserRecord, error) {
	return s.repo.GetByStatus(ctx, status)
}

func (s *UserService) UpdateUserStatus(
	ctx context.Context,
	mobileNumber string,
	status string,
) (error) {
	return s.repo.UpdateUserStatus(ctx, mobileNumber, status)
}
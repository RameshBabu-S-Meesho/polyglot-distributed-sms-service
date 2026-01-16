package handlers

import (
	"encoding/json"
	"net/http"
	"smsconsumer/internal/services"
	"strings"
)

type UserHandler struct {
	services *services.UserService
}

func NewUserHandler(serv *services.UserService) *UserHandler {
	return &UserHandler{services: serv}
}

func (h *UserHandler) GetUsersByStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Expected: /v1/users/{requiredStatus}/filter
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) != 5 || parts[4] != "filter" {
		http.NotFound(w, r)
		return
	}

	requiredStatus := parts[3]
	if !isValidStatus(&requiredStatus){
		http.Error(w, "invalid status: must be either blocked or unblocked", http.StatusBadRequest)
        return
	}

	records, err := h.services.GetByStatus(r.Context(), requiredStatus)
	if err != nil {
		http.Error(w, "internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(records)
}

func isValidStatus(s *string) bool {
	if *s == "BLOCKED" || *s == "blocked" {
		*s = "BLOCKED"
		return true
	}
	if *s == "UNBLOCKED" || *s == "unblocked" {
		*s = "UNBLOCKED"
		return true
	}
	return false
}
package handlers

import (
	"encoding/json"
	"net/http"
	"smsconsumer/internal/services"
	"strings"
)

type SmsHandler struct {
	services *services.SmsService
}

func NewSmsHandler(serv *services.SmsService) *SmsHandler {
	return &SmsHandler{services: serv}
}

func (h *SmsHandler) GetUserMessages(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Expected: /v1/user/{mobileNumber}/messages
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) != 5 || parts[4] != "messages" {
		http.NotFound(w, r)
		return
	}

	mobileNumber := parts[3]

	if !isValidMobileNumber(mobileNumber) {
        http.Error(w, "invalid mobile number: must be exactly 10 digits", http.StatusBadRequest)
        return
    }

	records, err := h.services.GetUserMessages(r.Context(), mobileNumber)
	if err != nil {
		http.Error(w, "internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(records)
}

func isValidMobileNumber(s string) bool {
    if len(s) != 10 {
        return false
    }
    for _, r := range s {
        if r < '0' || r > '9' {
            return false
        }
    }
    return true
}
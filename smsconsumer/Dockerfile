FROM golang:1.25 AS builder
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .
# build the server binary
RUN CGO_ENABLED=0 GOOS=linux go build -o /sms-store ./cmd/server

# final image
FROM alpine:3.18
RUN apk add --no-cache ca-certificates
COPY --from=builder /sms-store /usr/local/bin/sms-store
EXPOSE 8081
ENTRYPOINT ["/usr/local/bin/sms-store"]
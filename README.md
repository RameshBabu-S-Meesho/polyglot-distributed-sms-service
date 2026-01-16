# SMS Messaging Platform (Microservices)

This repository contains an event-driven SMS messaging system composed of two microservices and the necessary infrastructure. It is designed to demonstrate asynchronous processing using a **Spring Boot Producer** and a **Go Consumer** orchestrated via **Docker Compose**.

## 📂 Repository Structure

```text
.
├── smssender/         # Java Spring Boot Service (Producer)
├── smsconsumer/       # Go Lang Service (Consumer)
├── docker-compose.yml # Orchestration configuration
└── README.md          # Project Documentation

```

## 🛠 Tech Stack

* **Producer:** Java 17+, Spring Boot (Exposes Send/Block APIs)
* **Consumer:** Go (Golang) (Processes Kafka events & updates DB)
* **Message Broker:** Apache Kafka (KRaft mode)
* **Cache:** Redis (Used for blocklist validation)
* **Database:** MongoDB (Stores message logs and user status)

## 🚀 Quick Start

You can bring up the entire environment (Apps + Infrastructure) with a single command.

### Prerequisites

* Docker Desktop or Docker Engine installed.
* Docker Compose installed.

### Run the Application

Run the following command in the root directory:

```bash
docker-compose up --build -d

```

This will:

1. Build the **Java** and **Go** Docker images.
2. Start **Kafka**, **Redis**, and **MongoDB**.
3. Start the **smssender** and **smsconsumer** containers in detached mode.

### Stop the Application

To stop and remove the containers:

```bash
docker-compose down

```

*(To remove data volumes as well, add the `-v` flag)*

## 🔌 Services & Ports

Once running, the services are available at the following ports:

| Service | Port | Description |
| --- | --- | --- |
| **SMS Sender (Java)** | `8080` | Accepts SMS requests and manages blocklists. |
| **SMS Consumer (Go)** | `8081` | Read-only API to fetch message history and status. |
| **Redis** | `6379` | Cache storage. |
| **MongoDB** | `27017` | Persistent storage. |
| **Kafka** | `9092` | Message broker. |

## 🧪 How to Test

You can use `curl` or Postman to interact with the API.

### 1. Send an SMS

This pushes a message to Kafka.

```bash
curl -X POST http://localhost:8080/v1/sms/send \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "9998887773", "message": "Hello from Docker!"}'

```

### 2. Check Message Logs

Retrieve the message stored by the Go consumer in MongoDB.

```bash
curl http://localhost:8081/v1/user/9998887773/messages

```

### 3. Block a User

Add a number to the Redis blocklist.

```bash
curl -X POST http://localhost:8080/v1/sms/block \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "9998887773"}'

```

### 4. Verify Block

Attempt to send a message again; it should be rejected immediately.

```bash
curl -X POST http://localhost:8080/v1/sms/send \
  -H "Content-Type: application/json" \
  -d '{"mobileNumber": "9998887773", "message": "This should fail"}'

```

## ⚙️ Configuration

The `docker-compose.yml` pre-configures the environment variables for you:

* **Kafka:** Configured in KRaft mode (no Zookeeper).
* **Database Credentials:** `admin` / `password`.
* **Network:** All services run on the default bridge network created by Compose, allowing them to communicate via service names (`kafka`, `redis`, `mongodb`).
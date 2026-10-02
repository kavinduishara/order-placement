# Microservices-Based Order Processing System

A backend application for processing store orders using **Spring Boot microservices** and **Apache Kafka** for asynchronous, event-driven communication.

The system consists of two independent services:

* **Order Service** — receives and manages customer orders.
* **Inventory Service** — validates product availability and updates inventory.

The services communicate asynchronously through **Apache Kafka**, rather than making direct synchronous HTTP calls.

## Architecture

```text
                    POST /orders
                         │
                         ▼
                ┌─────────────────┐
                │  Order Service  │
                │   Spring Boot   │
                └────────┬────────┘
                         │
                         │ InventoryReservationRequestedEvent
                         ▼
                  ┌──────────────┐
                  │ Apache Kafka │
                  └──────┬───────┘
                         │
                         ▼
              ┌────────────────────┐
              │ Inventory Service  │
              │    Spring Boot     │
              └─────────┬──────────┘
                        │
                 Check availability
                        │
              ┌─────────┴─────────┐
              │                   │
              ▼                   ▼
       Stock available      Insufficient stock
              │                   │
              ▼                   ▼
       Reserve/update       Return failure
          inventory          response
              │                   │
              └─────────┬─────────┘
                        │
                        ▼
                     Kafka
                        │
                        ▼
                ┌───────────────┐
                │ Order Service │
                └───────────────┘
                        │
                        ▼
                 Order confirmed
```

## How It Works

1. A client sends an order request to the **Order Service**.
2. The Order Service creates the order and publishes an inventory reservation event to Kafka.
3. The **Inventory Service** consumes the event asynchronously.
4. The Inventory Service checks whether the requested products and quantities are available.
5. If sufficient stock is available, the inventory is updated and a successful response event is published.
6. If there is insufficient stock, the Inventory Service publishes a failure response.
7. The Order Service consumes the inventory response and updates the order accordingly.

This demonstrates an **event-driven communication pattern** between independently running microservices.

## Services

### Order Service

Responsible for:

* Receiving order requests
* Creating orders
* Publishing inventory reservation requests
* Consuming inventory responses
* Updating the order based on the inventory result

### Inventory Service

Responsible for:

* Consuming inventory reservation requests
* Validating product availability
* Updating stock after successful validation
* Publishing inventory success or failure events

## Technology Stack

* **Java**
* **Spring Boot**
* **Spring Kafka**
* **Apache Kafka**
* **PostgreSQL**
* **Docker & Docker Compose**
* **Maven**

## Project Structure

```text
order-processing/
├── order-service/
│   └── ...
├── inventory-service/
│   └── ...
├── docker-compose.yml
└── README.md
```

## Running the Application

### Prerequisites

Make sure the following are installed:

* Java
* Maven
* Docker
* Docker Compose

### 1. Start Kafka

From the project root:

```bash
docker-compose up -d
```

This starts the Kafka infrastructure required by the services.

You can verify the containers with:

```bash
docker ps
```

### 2. Start the Services

Start the **Order Service** and **Inventory Service** separately using your IDE or Maven.

For example:

```bash
./mvnw spring-boot:run
```

or, if Maven is installed globally:

```bash
mvn spring-boot:run
```

Run the command from each service's directory.

### 3. Place an Order

Send an order request to the Order Service's order endpoint.

The Order Service publishes an inventory reservation event to Kafka. The Inventory Service then consumes the event and checks the requested stock.

If the inventory is available, the stock is updated and the order can proceed. If the requested quantity is unavailable, the inventory service returns a failure event and the order is handled accordingly.

## Kafka Communication

The services communicate through Kafka topics instead of directly calling each other.

Example event flow:

```text
Order Service
     │
     │ InventoryReservationRequestedEvent
     ▼
inventory-reservation-requested
     │
     ▼
Inventory Service
     │
     │ Inventory response event
     ▼
Order Service
```

Using Kafka allows the services to communicate asynchronously and remain independently deployable.

## Key Concepts Demonstrated

* Microservices architecture
* Event-driven architecture
* Asynchronous communication
* Apache Kafka producers and consumers
* Kafka topics and consumer groups
* JSON event serialization/deserialization
* Inventory validation
* Database persistence with PostgreSQL
* Dockerized infrastructure
* Spring Boot REST APIs

# Real-Time Social Media Sentiment Analysis

A robust, scalable data pipeline designed to ingest, process, and classify the sentiment of live social media data using machine learning. This project demonstrates an end-to-end architecture encompassing message brokering, stream processing, distributed machine learning, and a modern Spring Boot backend.

## Table of Contents
- [Features](#features)
- [Architecture and Data Flow](#architecture-and-data-flow)
- [Core Technologies](#core-technologies)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)
- [Usage and Testing](#usage-and-testing)
- [Troubleshooting](#troubleshooting)

## Features

- **Real-Time Data Ingestion:** Highly available messaging with Apache Kafka handling raw incoming streams.
- **Distributed Machine Learning:** Batch training of text sentiment classifiers utilizing PySpark MLlib (Tokenizer, IDF, CountVectorizer, Logistic Regression).
- **Stream Processing:** PySpark Structured Streaming for real-time model inference and prediction.
- **Robust Backend:** A Spring Boot 3 API written in Java 21 to act as the primary interface, bridging event consumers, producers, and the database.
- **Optimized Storage:** PostgreSQL integration through Hibernate 6, utilizing native JSONB column mapping for high performance on semi-structured data.
- **Containerized Environments:** Fully managed local test and development environment orchestrated via Docker Compose.

## Architecture and Data Flow

The system is built on an event-driven architecture relying on Apache Kafka to decouple data ingestion from data processing.

1. **Data Ingestion**: Raw tweets or social media posts are ingested via REST APIs on the Spring Boot backend or external shell scripts, and pushed to the Kafka `tweets.raw` topic.
2. **Model Training (Batch Job)**: A PySpark batch job (`spark-training`) initializes on startup. It reads a historical dataset from the host machine, executes a text-processing pipeline, trains a Logistic Regression classifier, and persists the model locally to a shared Docker volume.
3. **Stream Processing**: A PySpark Structured Streaming job (`spark-streaming`) continuously listens to `tweets.raw`. It loads the previously trained model, cleans the incoming text streams, predicts the sentiment mapping, and publishes the scored results to the `tweets.processed` topic.
4. **Persistence & Serving**: The Spring Boot application consumes the parsed predictions from `tweets.processed` and persists them into a PostgreSQL database for reporting and visualization.

## Core Technologies

- **Backend Platform**: Java 21, Spring Boot 3.x, Hibernate 6
- **Message Broker**: Apache Kafka
- **Big Data / ML**: Apache Spark 3.5.1, PySpark, Spark MLlib, Spark Structured Streaming
- **Database**: PostgreSQL 16
- **Infrastructure**: Docker, Docker Compose

## Project Structure

```text
.
├── data/                       # Local volume mapped to Spark containers
│   ├── twitter/                # Directory for training (.csv) datasets
│   └── spark_sentiment_model/  # Exported PySpark ML Pipeline Model
├── scripts/                    # Shell scripts for testing and DB management
│   ├── test/
│   │   └── send_test_tweets.sh # Script to inject mock tweets into Kafka
│   └── postgres/               # DB Helper scripts
├── spark/
│   ├── streaming/              # Application script & Dockerfile for real-time inference
│   └── training/               # Application script & Dockerfile for batch training
├── src/                        # Spring Boot backend source code
├── docker-compose.yml          # Container orchestration configuration
└── pom.xml                     # Maven dependencies
```

## Getting Started

Follow these steps to deploy the application on your local machine.

### Prerequisites

- **Docker & Docker Compose**: Required for standing up the data cluster.
- **Java 21 JDK**: Required for compiling the API locally.
- **Apache Maven**: Required for building and running the Spring application.
- Minimal 8GB of RAM allocated to Docker.

### Installation & Setup

**1. Prepare the Dataset**
The PySpark training job requires baseline dataset batches to train the model upon startup.
- Place your historical training data in `data/twitter/twitter_training.csv`.
- Place validation data in `data/twitter/twitter_validation.csv`.
- Ensure CSV files have NO header and strictly contain exactly 4 columns: `id`, `entity`, `sentiment`, `text`.

**2. Configure File Permissions**
The Spark containers run under a non-root user (`spark`) that needs write access to the host machine's mapped volume to compile its Ivy caches and export the ML model to your host. Run:
```bash
chmod 777 -R data/
```

**3. Boot the Infrastructure**
Deploy the core backbone services (Postgres, Kafka, Spark Master, Spark Worker, and Spark Python scripts) using Docker Compose:
```bash
sudo docker compose up --build -d
```

**4. Verify Model Training Pipeline**
The training job takes several minutes to complete based on hardware. Follow the container logs to ensure the ML pipeline generates and saves the model without path errors:
```bash
docker logs -f spark-training
```
When you see `Model saved successfully.` at the bottom of the logs, the stream processing container will be able to start parsing events.

**5. Start the Spring Boot Backend**
Launch the Java API backend which establishes Kafka consumers:
```bash
./mvnw spring-boot:run
```

## Usage and Testing

Once all containers and the backend rest API are running, test the data movement.

**Simulate Live Traffic**
Execute the provided testing script to submit a series of mock social media strings onto the raw Kafka topic. 
```bash
./scripts/test/send_test_tweets.sh
```

**Observe Data Throughput**
1. Check the `spark-streaming` container logs. It will continuously map the incoming data strings to the pre-loaded ML model and evaluate a sentiment label (Positive, Negative, Neutral).
2. Monitor your Spring Boot application terminal window to see the `tweets.processed` payloads received from Kafka and automatically converted to Java object entities and pushed to Postgres.

## Troubleshooting

- **Error: Input path does not exist (spark_sentiment_model)**
  If the `spark-streaming` container is crash-looping with this stack trace, it means the `spark-training` container hasn't finished its job saving the model to the `./data` volume. Allow `spark-training` to finish, then restart the streaming job.
- **Connection/Port Conflicts**
  Verify that ports `5435` (Postgres override), `9092` (Kafka), `8080-8081` (Spark Web UIs) are fully available on your localhost.
- **Dependency Missing in Spark**
  Ensure you are building Docker images cleanly (`docker compose build --no-cache`) if changes were made to DOCKERFILE permission strings involving `/home/spark/.ivy2`.

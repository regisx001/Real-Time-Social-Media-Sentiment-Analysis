# Roadmap

This document outlines the current state of the project and planned future enhancements.

## Phase 1: Core Data Pipeline (Completed)
- Setup Docker Compose infrastructure (PostgreSQL, Kafka, Spark Cluster).
- Implement Spring Boot application with Kafka Producer and Consumer configurations.
- Develop PySpark ML Pipeline for batch training on the initial dataset.
- Develop PySpark Structured Streaming job to handle predictions on the fly.
- Connect streaming outputs back into Kafka and consume them via the Spring Backend.
- Map data effectively using Hibernate 6 and native JSON types.

## Phase 2: Analytics and Dashboarding
- Build a real-time web dashboard using WebSockets or Server-Sent Events (SSE) to display incoming tweet sentiments.
- Implement aggregated API endpoints (e.g., sentiment over the last hour) in Spring Boot.
- Integrate a frontend framework (React, Vue.js, or Angular) to visualize data charts.

## Phase 3: Machine Learning Improvements
- Replace basic Logistic Regression with more advanced NLP models (e.g., Hugging Face Transformers, BERT) running on PySpark or a dedicated inference API.
- Implement continuous learning and model retraining strategies.
- Track ML experiments using MLflow or similar tooling.

## Phase 4: Scalability and Cloud Deployment
- Migrate Docker Compose setup to Kubernetes using Helm charts.
- Configure cloud-native managed services: Amazon MSK (Kafka), Amazon RDS (PostgreSQL), and Amazon EMR or Databricks (Spark).
- Add robust monitoring and alerting with Prometheus and Grafana.

## Phase 5: CI/CD and Testing
- Establish GitHub Actions for automated unit and integration testing.
- Implement end-to-end load testing for the streaming pipeline.
- Automate Docker image builds and registry pushes.
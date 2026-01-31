# System Architecture

## Overview
Quantiq is a decoupled algorithmic trading platform designed for high-performance data processing and reliable trade execution. The system is split into two primary components: a Python-based data engine and a Spring Boot/Kotlin core backend.

## Components

### 1. quantiq-core (Spring Boot / Kotlin)
- **Role**: Core business logic, User management, Portfolio tracking, and Trade execution.
- **Port**: `10010` (mapped to internal `8080`)
- **Key Responsibilities**:
    - Managing KIS API interactions and token lifecycle.
    - Providing REST API for front-end/mobile clients.
    - Handling user-specific trade configurations and auto-trading logic.
    - Listening for analysis completion events via Kafka to trigger trades.

### 2. quantiq-data-engine (Python)
- **Role**: Data collection, Technical analysis, and Sentiment processing.
- **Port**: `10020` (mapped to internal `8000`)
- **Key Responsibilities**:
    - Scraping/Fetching market data (FRED, Yahoo Finance).
    - Running technical indicators (Bollinger Bands, RSI, etc.).
    - Processing news sentiment analysis.
    - Publishing analysis results to Kafka topics.

### 3. Middleware & Infra
- **Kafka**: Asynchronous event bus for triggering analysis and trades.
- **MongoDB**: Storage for flexible analytical data (Recommendations, Sentiment).
- **PostgreSQL**: (Planned) Source of truth for identity and financial transactions.

## Event Flow
1. **Trigger**: User or Job triggers analysis via `quantiq-core`.
2. **Collect**: `quantiq-data-engine` receives a Kafka request, fetches data, and analyzes it.
3. **Notify**: `quantiq-data-engine` publishes a "Completion" event to Kafka.
4. **Execute**: `quantiq-core` consumes the event and executes trading logic if conditions are met.

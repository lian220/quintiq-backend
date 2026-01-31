# Database Design

## Polyglot Persistence
Quantiq utilizes a polyglot persistence strategy to balance the need for relational integrity with the flexibility required for analytical data.

## Data Mapping

| Storage | Entity | Reason |
| :--- | :--- | :--- |
| **PostgreSQL** | Users, KisTokens, TradingConfig | Transactional data requiring high consistency and ACID compliance. |
| **MongoDB** | StockMaster, Recommendations, SentimentAnalysis | Flexible, document-based storage for varied analytical results and master data. |

## Schema Design

### MongoDB Collections
- `stock_master`: Global list of tickers and metadata.
- `stock_recommendation`: Consolidated signals (Buy/Sell/Wait).
- `stock_analysis`: Detailed technical and sentiment payloads.
- `economic_data`: Macro-economic indicators (FRED/YF).

### PostgreSQL Tables (Planned)
- `users`: User identity and profile.
- `kis_tokens`: API access and refresh tokens.
- `trading_configs`: User-specific parameters (trailing stop, budget).
- `account_balances`: Snapshots of overseas/local holdings.

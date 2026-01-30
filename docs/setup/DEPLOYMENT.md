# Deployment & Operations

## Local Deployment
The entire stack is managed via Docker Compose.

### Port Mapping
| Service | Internal Port | Host Port |
| :--- | :--- | :--- |
| Spring Core | 8080 | **10010** |
| Python Engine | 8000 | **10020** |
| MongoDB | 27017 | 27017 |
| Kafka | 9092 | 9092 |

### Commands
- **Start All**: `docker-compose up -d`
- **Rebuild**: `docker-compose up --build -d`
- **Logs**: `docker-compose logs -f`
- **Stop**: `docker-compose down`

## Configuration
All configuration is driven by the `.env` file in the root directory. Ensure all mandatory keys (`FRED_API_KEY`, `KIS_APP_KEY`, etc.) are populated before starting.

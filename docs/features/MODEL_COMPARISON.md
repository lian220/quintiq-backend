# predict.py 모델 로직 비교 (GitLab vs Local)

## 🎯 결론부터

**ML 모델 로직과 알고리즘은 완전히 동일합니다!**

유일한 차이점은 **데이터베이스 백엔드**뿐입니다:
- GitLab: Supabase (PostgreSQL)
- Local: MongoDB

---

## 📊 상세 비교

### 1. 모델 아키텍처

| 구성 요소 | GitLab | Local | 동일? |
|----------|--------|-------|-------|
| **모델 타입** | Transformer (Dual-Input) | Transformer (Dual-Input) | ✅ 동일 |
| **Attention Heads** | 8 | 8 | ✅ 동일 |
| **Hidden Dimension** | 256 | 256 | ✅ 동일 |
| **Encoder Blocks** | 4 per stream | 4 per stream | ✅ 동일 |
| **Dropout Rate** | 0.1 (attention), 0.2 (dense) | 0.1 (attention), 0.2 (dense) | ✅ 동일 |

### 2. 입력 데이터

| 파라미터 | GitLab | Local | 동일? |
|---------|--------|-------|-------|
| **Lookback Window** | 90 days | 90 days | ✅ 동일 |
| **Stock Features** | 27 (주식 + ETF) | 27 (target_columns) | ✅ 동일 |
| **Economic Features** | 37 (FRED + 기타) | 37 (economic_features) | ✅ 동일 |
| **Input Shape (Stock)** | (90, 27) | (90, 27) | ✅ 동일 |
| **Input Shape (Econ)** | (90, 37) | (90, 37) | ✅ 동일 |

### 3. 학습 파라미터

| 파라미터 | GitLab | Local | 동일? |
|---------|--------|-------|-------|
| **Epochs** | 50 | 50 | ✅ 동일 |
| **Batch Size** | 32 | 32 | ✅ 동일 |
| **Learning Rate** | 0.0001 | 0.0001 | ✅ 동일 |
| **Optimizer** | Adam | Adam | ✅ 동일 |
| **Loss Function** | MSE | MSE | ✅ 동일 |
| **Metric** | MAE | MAE | ✅ 동일 |

### 4. 예측 설정

| 파라미터 | GitLab | Local | 동일? |
|---------|--------|-------|-------|
| **Forecast Horizon** | 14 days ahead | 14 days ahead | ✅ 동일 |
| **Output Size** | 27 (주식 예측) | 27 (target_columns) | ✅ 동일 |
| **Prediction Method** | 90일 데이터로 14일 후 예측 | 90일 데이터로 14일 후 예측 | ✅ 동일 |

---

## 🏗️ 모델 구조 비교

### GitLab 버전

```python
# 동일한 Transformer Encoder
def transformer_encoder(inputs, num_heads=8, ff_dim=256, dropout=0.1):
    attention_output = MultiHeadAttention(num_heads=num_heads, key_dim=inputs.shape[-1])(inputs, inputs)
    attention_output = Dropout(dropout)(attention_output)
    # ... Layer Normalization, FFN, etc.

# 동일한 Dual-Input 구조
stock_input = Input(shape=(90, 27))
econ_input = Input(shape=(90, 37))

# 4개 Encoder 블록
for _ in range(4):
    stock_encoded = transformer_encoder(stock_encoded, num_heads=8, ff_dim=256)
    econ_encoded = transformer_encoder(econ_encoded, num_heads=8, ff_dim=256)

# 동일한 Fusion
merged = Add()([stock_dense, econ_dense])
output = Dense(27)(pooled)  # 27개 주식 예측
```

### Local 버전

```python
# Line 441-453: 동일한 Transformer Encoder
def transformer_encoder(inputs, num_heads, ff_dim, dropout=0.1):
    attention_output = MultiHeadAttention(num_heads=num_heads, key_dim=inputs.shape[-1])(inputs, inputs)
    attention_output = Dropout(dropout)(attention_output)
    # ... Layer Normalization, FFN, etc.

# Line 456-476: 동일한 Dual-Input 구조
stock_input = Input(shape=stock_shape)  # (90, 27)
econ_input = Input(shape=econ_shape)    # (90, 37)

# 4개 Encoder 블록
for _ in range(4):
    stock_encoded = transformer_encoder(stock_encoded, num_heads=num_heads, ff_dim=ff_dim)
    econ_encoded = transformer_encoder(econ_encoded, num_heads=num_heads, ff_dim=ff_dim)

# 동일한 Fusion
merged = Add()([stock_dense, econ_dense])
output = Dense(target_size)(pooled)  # target_size=27
```

**→ 코드 구조 거의 동일!**

---

## 🔄 데이터 처리 흐름

### GitLab 버전

```python
# 1. Supabase에서 데이터 조회
response = supabase.table("economic_and_stock_data").select("*").execute()
df = pd.DataFrame(response.data)

# 2. 정규화
scaler = MinMaxScaler()
data_scaled = scaler.fit_transform(df[all_features])

# 3. 시퀀스 생성 (90일 lookback)
for i in range(90, len(data_scaled) - 14):
    X_stock_seq = data_scaled[stock_features][i-90:i]
    X_econ_seq = data_scaled[econ_features][i-90:i]
    y = data_scaled[stock_features][i+14]  # 14일 후 예측

# 4. 모델 학습
model.fit([X_stock, X_econ], y, epochs=50, batch_size=32)

# 5. 예측 후 Supabase에 저장
supabase.table("predicted_stocks").insert(predictions).execute()
```

### Local 버전

```python
# 1. MongoDB에서 데이터 조회
cursor = db.daily_stock_data.find().sort("date", 1)
df = convert_to_dataframe(cursor)  # fred_indicators, yfinance_indicators 펼치기

# 2. 정규화
scaler = MinMaxScaler()
data_scaled = scaler.fit_transform(data[all_features])

# 3. 시퀀스 생성 (90일 lookback) - Line 698-711
lookback = 90
for i in range(lookback, len(data_scaled) - forecast_horizon):
    X_stock_seq = data_scaled[target_columns].iloc[i-lookback:i].to_numpy()
    X_econ_seq = data_scaled[economic_features].iloc[i-lookback:i].to_numpy()
    y = data_scaled[target_columns].iloc[i+forecast_horizon].to_numpy()

# 4. 모델 학습 - Line 738
model.fit([X_stock_train, X_econ_train], y_train, epochs=50, batch_size=32)

# 5. 예측 후 MongoDB에 저장
db.stock_predictions.bulk_write(prediction_updates)
```

**→ 데이터 처리 로직 동일! DB 접근 방식만 다름**

---

## 📦 저장되는 예측 결과

### GitLab 버전 → Supabase 테이블

```python
# predicted_stocks 테이블
{
  "날짜": "2026-01-31",
  "애플_actual": 150.25,
  "애플_predicted": 152.30,
  "마이크로소프트_actual": 380.50,
  "마이크로소프트_predicted": 378.90,
  # ... 27개 주식 각각 actual/predicted
}
```

### Local 버전 → MongoDB 컬렉션

```javascript
// stock_predictions 컬렉션
{
  _id: ObjectId("..."),
  date: "2026-01-31",
  stock_name: "애플",
  actual_price: 150.25,
  predicted_price: 152.30,
  error: 2.05,
  error_pct: 1.36
}
// 각 주식별 개별 문서
```

**→ 저장 구조만 다름 (테이블 vs 컬렉션)**

---

## 🎯 최종 결론

### ✅ 동일한 것들

1. **ML 모델 아키텍처** - Transformer, Dual-Input, 8 heads, 256 dim
2. **학습 파라미터** - 50 epochs, batch 32, lr 0.0001
3. **데이터 처리 로직** - 90일 lookback, 14일 예측, MinMaxScaler
4. **입력 데이터** - 27 주식 + 37 경제지표
5. **예측 방식** - 시계열 시퀀스 기반 예측

### ❌ 다른 것

**오직 데이터베이스 백엔드만 다름:**
- GitLab: Supabase (PostgreSQL) → 테이블 구조
- Local: MongoDB → 문서 구조

---

## 💡 실무적 의미

### GitLab 버전을 참고할 수 있는 부분

✅ **모델 아키텍처 개선**이 있다면 동일하게 적용 가능
✅ **하이퍼파라미터 튜닝** 결과 공유 가능
✅ **전처리 로직** 개선사항 적용 가능

### GitLab 버전을 직접 사용할 수 없는 이유

❌ **DB 타입이 다름** (PostgreSQL vs MongoDB)
❌ **인프라 변경 필요** (Supabase 계정, 마이그레이션 등)
❌ **현재 Local 버전이 이미 검증됨** (22,002개 문서)

---

## 🚀 Quantiq의 방향

**Local stock-trading 구조 (MongoDB - daily_stock_data)를 따르면:**

1. ✅ 검증된 ML 모델 그대로 사용
2. ✅ 동일한 예측 성능
3. ✅ MongoDB 인프라 활용
4. ✅ predict.py 즉시 사용 가능

**현재 작업 (옵션 1) 계속 진행하는 것이 정답입니다!** 🎯

# Phase 2: 경쟁 기능 상세 스펙

커뮤니티, 교육, 포트폴리오 최적화, 커스텀 전략

## 📋 개요

**기간**: 12주 (3개월)
**목표**: 차별화된 사용자 경험 구축
**팀 규모**: 11명 (기존 8명 + 새로 3명)
**주요 산출물**: 커뮤니티 플랫폼 + 교육 시스템 + 포트폴리오 최적화

---

## 1️⃣ Phase 2-1: 커뮤니티 플랫폼 구축

### 기능 요구사항

#### 1.1 사용자 프로필 & 팔로우
```
✅ 사용자 프로필
  - 기본 정보 (닉네임, 사진, 자기소개)
  - 투자 성과 (총 수익률, 거래 통계)
  - 포트폴리오 공개 여부 설정
  - 팔로워 수, 팔로잉 수

✅ 팔로우 기능
  - 투자자 팔로우/언팔로우
  - 팔로워 관리
  - 차단 기능
  - 팔로우 추천 (유사 전략)

공개 범위:
- 공개 (모두 볼 수 있음)
- 팔로워만 (팔로워만 볼 수 있음)
- 비공개 (자신만 볼 수 있음)
```

#### 1.2 타임라인 & 피드
```
✅ 타임라인
  - 팔로우 중인 사용자의 거래 활동
  - 전략 공유
  - 의견/인사이트 게시
  - 시장 분석 포스트

✅ 피드 정렬
  - 최신순 (기본)
  - 인기순 (좋아요 + 댓글)
  - 실시간 거래 피드

✅ 콘텐츠 유형
  - 텍스트 + 이미지
  - 차트 스냅샷
  - 거래 신호
  - 분석 리포트
```

#### 1.3 전략 공유 & 평가
```
✅ 전략 공유
  - 자신의 거래 전략 공개
  - 전략 상세 설명
  - 백테스트 결과 공개
  - 성과 지표 표시

✅ 평가 시스템
  - 별점 평가 (1-5별)
  - 댓글 작성
  - 좋아요
  - 공유

✅ 전략 랭킹
  - 성과 기반 랭킹
  - 인기도 기반 랭킹
  - 신규 전략 추천
  - 카테고리별 탑 전략
```

#### 1.4 커뮤니티 상호작용
```
✅ 댓글
  - 게시물별 댓글
  - 댓글에 대한 답글
  - 댓글 좋아요
  - 댓글 신고

✅ 메시지
  - 1:1 비공개 메시지
  - 메시지 검색
  - 메시지 정렬

✅ 알림
  - 팔로워가 새 게시물 올림
  - 내 게시물에 댓글
  - 댓글에 답글 달림
  - 메시지 받음
```

### 기술 스펙

#### 데이터베이스 스키마
```sql
-- 사용자 프로필
CREATE TABLE user_profiles (
  id UUID PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  bio TEXT,
  profile_image_url VARCHAR(255),
  total_return DECIMAL(10,2),
  trade_count INT,
  win_rate DECIMAL(5,2),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 팔로우 관계
CREATE TABLE follows (
  follower_id UUID NOT NULL,
  following_id UUID NOT NULL,
  created_at TIMESTAMP,
  PRIMARY KEY (follower_id, following_id)
);

-- 게시물
CREATE TABLE posts (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES user_profiles(id),
  title VARCHAR(200),
  content TEXT,
  images JSON,
  strategy_data JSON,
  visibility ENUM('public', 'followers', 'private'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 댓글
CREATE TABLE comments (
  id UUID PRIMARY KEY,
  post_id UUID NOT NULL REFERENCES posts(id),
  user_id UUID NOT NULL REFERENCES user_profiles(id),
  parent_comment_id UUID REFERENCES comments(id),
  content TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

-- 좋아요
CREATE TABLE likes (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  post_id UUID REFERENCES posts(id),
  comment_id UUID REFERENCES comments(id),
  created_at TIMESTAMP,
  UNIQUE (user_id, post_id),
  UNIQUE (user_id, comment_id)
);

-- 메시지
CREATE TABLE messages (
  id UUID PRIMARY KEY,
  sender_id UUID NOT NULL,
  recipient_id UUID NOT NULL,
  content TEXT,
  read BOOLEAN DEFAULT false,
  created_at TIMESTAMP
);
```

#### 인덱싱 전략
```
High Priority:
- posts(user_id, created_at) - 사용자 피드 조회
- comments(post_id, created_at) - 댓글 조회
- follows(follower_id, following_id) - 팔로우 관계
- likes(post_id) - 좋아요 수 집계
- messages(recipient_id, read) - 미읽음 메시지

Medium Priority:
- user_profiles(username) - 사용자 검색
- posts(created_at) - 최신 게시물
- comments(user_id) - 사용자 댓글
```

#### API 엔드포인트
```
프로필 관리:
GET    /api/users/{userId}/profile
PUT    /api/users/{userId}/profile
GET    /api/users/{userId}/stats

팔로우:
POST   /api/users/{userId}/follow
DELETE /api/users/{userId}/follow
GET    /api/users/{userId}/followers
GET    /api/users/{userId}/following

게시물:
POST   /api/posts
GET    /api/posts (피드)
GET    /api/posts/{postId}
PUT    /api/posts/{postId}
DELETE /api/posts/{postId}
GET    /api/posts/user/{userId}

댓글:
POST   /api/posts/{postId}/comments
GET    /api/posts/{postId}/comments
PUT    /api/comments/{commentId}
DELETE /api/comments/{commentId}

좋아요:
POST   /api/posts/{postId}/like
DELETE /api/posts/{postId}/like
GET    /api/posts/{postId}/likes

메시지:
POST   /api/messages
GET    /api/messages
GET    /api/messages/{conversationId}
```

---

## 2️⃣ Phase 2-2: 초보자 교육 & 온보딩 프로그램

### 기능 요구사항

#### 2.1 차트게임 시스템
```
✅ 게임 메커니즘
  - 과거 차트 제시 (랜덤 5년 구간)
  - 플레이어가 매수/매도 시점 선택
  - 실제 수익률 계산
  - 최종 성과 평가

✅ 게임 모드
  - 데일리 챌린지 (매일 새로운 차트)
  - 자유 모드 (아무때나 플레이)
  - 챔피언 모드 (리더보드)
  - 튜토리얼 모드 (설명 포함)

✅ 보상 시스템
  - 포인트 획득 (성과 기반)
  - 뱃지 획득 (마일스톤)
  - 친구 도전
  - 주간/월간 랭킹

✅ 성과 추적
  - 개인 최고 성과
  - 평균 수익률
  - 거래 횟수
  - 승률
```

#### 2.2 교육 콘텐츠
```
✅ 비디오 교육
  - 기술적 분석 기초 (10 videos)
  - 위험 관리 (8 videos)
  - 자동매매 이해 (6 videos)
  - 포트폴리오 관리 (6 videos)

각 영상:
- 길이: 5-15분
- 자막: 한국어/영어
- 자료 다운로드: 가능
- 퀴즈: 각 영상 후

✅ 인터랙티브 튜토리얼
  - 클릭하며 배우기
  - 실시간 예제
  - 즉시 피드백
  - 진도율 표시

✅ 가이드 문서
  - 초급자 가이드 (10 pages)
  - 중급자 가이드 (15 pages)
  - 고급 전략 (20 pages)
  - FAQ (100+ 항목)
```

#### 2.3 실시간 세미나
```
✅ 라이브 세미나
  - 주간 라이브 방송 (목요일 19:00)
  - 실시간 Q&A
  - 화면 공유
  - 녹화 제공 (나중에 시청)

✅ 세미나 주제
  - 주간 시장 분석
  - 전략 공개 설명
  - 리스크 관리 심화
  - 사용자 질문 대답

✅ 인증 시스템
  - 수료증 발급
  - 학습 시간 기록
  - 뱃지 획득
  - 수료자 인증 배지
```

### 기술 스펙

#### 차트게임 기술
```
Frontend:
- React + TradingView Lightweight Charts
- Canvas (성능 최적화)
- Redux (게임 상태 관리)

Backend:
- 게임 상태 저장 (PostgreSQL)
- 성과 계산 엔진
- 랭킹 시스템 (Redis)
- 매치메이킹 (친구 도전)

Data:
- 25년 가격 데이터
- 5년 구간 랜덤 생성
- 슬리페이지 포함 (현실적 시뮬)
```

#### 비디오 스트리밍
```
호스팅:
- AWS S3 (비디오 저장)
- CloudFront (배포)
- MediaConvert (인코딩)

플레이어:
- HLS (Adaptive Bitrate)
- 720p/1080p 지원
- 자막 (.vtt)
- 재생 속도 조절

분석:
- 시청 시간 추적
- 드롭 오프 포인트 분석
- 상호작용 추적
```

#### 라이브 스트리밍
```
Platform:
- OBS 스튜디오 (캡처)
- YouTube Live / Twitch
- 기록 보관 (YouTube)

Features:
- 실시간 채팅 (Moderated)
- 투표/Q&A 위젯
- 화면 공유
- 스피커 확대 보기
```

---

## 3️⃣ Phase 2-3: 포트폴리오 최적화 & 자산배분

### 기능 요구사항

#### 3.1 자산배분 알고리즘
```
✅ 위험 프로필 기반 제안
  - Conservative (보수적): 80% 채권 + 20% 주식
  - Moderate (중간): 60% 주식 + 40% 채권
  - Aggressive (공격적): 90% 주식 + 10% 현금
  - Custom: 사용자 정의

✅ 최적화 방법
  - Modern Portfolio Theory (Markowitz)
  - 최소 분산 포트폴리오
  - 최대 샤프 비율
  - 위험 균등 배분

✅ 제약 조건
  - 최소/최대 포지션 크기
  - 섹터 다양화
  - 자산 클래스 제한
  - 레버리지 제한

입력 매개변수:
- 목표 수익률
- 허용 최대 손실
- 투자 기간
- 위험 회피도
```

#### 3.2 자동 리밸런싱
```
✅ 리밸런싱 방식
  - 월간 리밸런싱 (고정)
  - 분기별 리밸런싱
  - 임계값 기반 (편차 > 5%)
  - 자동 vs 수동

✅ 리밸런싱 프로세스
  1. 목표 배분 재계산
  2. 현재 배분 vs 목표 배분 비교
  3. 편차 확인
  4. 거래 주문 생성
  5. 체결 후 기록 저장

✅ 비용 최적화
  - 최소 거래 비용 고려
  - 세금 영향 고려
  - 거래량 한도 고려
```

#### 3.3 세금 최적화
```
✅ 세금 계산
  - 양도소득세 (22%)
  - 기본공제 (250만원)
  - 보유 기간별 차등 세율 (추후 개선)
  - 손실 활용 (손실 공제)

✅ 세금 최적화 제안
  - 손실 실현 (Tax Loss Harvesting)
  - 이익 구간 관리
  - 보유 기간 최적화
  - 자산 이동 추천

예시:
실현 손실 = 100만원
절감 세금 = 100만원 × 22% = 22만원
```

#### 3.4 포트폴리오 분석
```
✅ 상관계수 분석
  - 자산 간 상관계수 계산
  - 히트맵 시각화
  - 다중공선성 확인
  - 다양화 효과 분석

✅ 스트레스 테스트
  - 시나리오 분석
  - 역사적 위기 재현
  - 극단 상황 시뮬레이션
  - 최악의 경우 손실 추정

✅ 성과 분석
  - 벤치마크 비교
  - 수익/손실 분해
  - 회피 회귀 분석
  - 스타일 분석
```

### 기술 스펙

#### 알고리즘 구현
```python
# 최대 샤프 비율 포트폴리오
from scipy.optimize import minimize

def portfolio_stats(weights, mean_returns, cov_matrix):
    portfolio_return = np.sum(mean_returns * weights)
    portfolio_std = np.sqrt(np.dot(weights, np.dot(cov_matrix, weights)))
    sharpe_ratio = portfolio_return / portfolio_std
    return portfolio_return, portfolio_std, sharpe_ratio

# 최적화
result = minimize(
    lambda w: -portfolio_stats(w, mean_returns, cov_matrix)[2],
    x0=equal_weights,
    constraints=constraints,
    bounds=bounds,
    method='SLSQP'
)
```

#### 데이터 요구사항
```
입력 데이터:
- 자산별 일일 수익률 (3-5년)
- 거래량 데이터
- 비용 (수수료, 세금)
- 자산 특성 (섹터, 유동성 등)

계산:
- 기대 수익률 (평균)
- 공분산 행렬 (상관계수)
- 포트폴리오 최적화
- 리밸런싱 거래
```

---

## 4️⃣ Phase 2-4: 커스텀 전략 구축 기능

### 기능 요구사항

#### 4.1 비주얼 전략 빌더
```
✅ 노드 기반 인터페이스
  - 지표 노드 (RSI, MACD 등)
  - 조건 노드 (if/else)
  - 액션 노드 (매수/매도)
  - 드래그&드롭 편집

✅ 전략 구성 요소
  1. 데이터 입력 (가격, 거래량)
  2. 지표 계산 (50+)
  3. 신호 생성 (조건 로직)
  4. 주문 실행 (거래)
  5. 위험 관리 (손절/익절)

✅ 예시 전략
  - 간단 이동평균 (SMA Crossover)
  - RSI 오버바이 (RSI > 70)
  - 볼린저 밴드 (가격 > 상단)
  - 복합 전략 (여러 조건)
```

#### 4.2 스크립팅 인터페이스
```
✅ Python 기반 코딩
  - Pandas 지원 (데이터 분석)
  - NumPy 지원 (수치 계산)
  - TA-Lib 지원 (기술 지표)
  - 제한된 라이브러리만 허용 (보안)

✅ 기본 템플릿
def strategy(data):
    # data: OHLCV 데이터
    # 지표 계산
    data['SMA20'] = data['Close'].rolling(20).mean()
    data['SMA50'] = data['Close'].rolling(50).mean()

    # 신호 생성
    data['Signal'] = np.where(
        data['SMA20'] > data['SMA50'], 1, -1
    )

    return data

✅ 실행 샌드박스
  - 제한된 리소스 사용
  - 타임아웃 보호 (5초)
  - 무한 루프 감지
  - 메모리 제한
```

#### 4.3 백테스팅 엔진
```
✅ 사용자 전략 백테스팅
  - 자동 실행
  - 성과 계산
  - 상세 리포트
  - 시각화

✅ 결과물
  - CAGR, MDD, Sharpe
  - 거래 기록 (모두 열람 가능)
  - 수익 곡선 차트
  - 드로다운 분석
  - 월별 수익률
```

#### 4.4 전략 공유 & 평가
```
✅ 전략 게시
  - 전략 저장
  - 공개/비공개
  - 설명 작성
  - 백테스트 결과 공시

✅ 사용자 평가
  - 별점 평가
  - 댓글
  - 포크 (복제 후 수정)
  - 사용 권한 요청

✅ 버전 관리
  - 전략 버전 관리
  - 변경 이력 추적
  - 이전 버전 복원
  - 기여자 표시
```

### 기술 스펙

#### 비주얼 빌더
```
Frontend:
- React Flow (노드 에디터)
- React Hooks (상태 관리)
- React DnD (드래그&드롭)
- Recharts (차트)

Backend:
- 전략 DAG (Directed Acyclic Graph)
- 그래프 검증 (순환 참조 확인)
- 전략 컴파일 (중간 코드 생성)
```

#### 스크립팅 샌드박스
```
Execution:
- RestrictedPython (제한된 Python 실행)
- Docker Container (격리)
- Resource Limit (CPU, Memory)
- Timeout (5초)

Allowed Libraries:
- pandas
- numpy
- ta-lib (기술 지표)

Forbidden:
- os, sys, subprocess
- network libraries
- file operations
```

---

## 📊 성공 기준

### Phase 2-1: 커뮤니티
- [ ] 월간 활성 사용자 > 10,000
- [ ] 월간 게시물 > 50,000
- [ ] 피드 응답시간 < 500ms
- [ ] 댓글 실시간 전달 < 1초

### Phase 2-2: 교육
- [ ] 영상 콘텐츠 50+ 개 완성
- [ ] 사용자 평균 시청 시간 > 10시간/월
- [ ] 차트게임 일일 활성 사용자 > 5,000
- [ ] 세미나 평균 참석자 > 1,000

### Phase 2-3: 포트폴리오 최적화
- [ ] 자산배분 제안 정확도 > 85%
- [ ] 리밸런싱 성공률 > 98%
- [ ] 세금 절감 액 > 100만원/년
- [ ] 사용 사용자 > 5,000

### Phase 2-4: 커스텀 전략
- [ ] 공유된 전략 > 1,000개
- [ ] 전략 포크 > 10,000회
- [ ] 사용자 만족도 > 4.3/5.0
- [ ] 평균 백테스트 시간 < 10초

---

**마지막 업데이트**: 2025-01-29
**상태**: 상세 스펙 작성 완료

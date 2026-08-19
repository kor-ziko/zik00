# 개발 architecture

Layered Architecture

개요
MVC보다 한 단계 발전된, 실무에서 가장 널리 쓰이는 구조입니다. Back-end 도메인에서 가장 기본적인 구조입니다.

Controller: HTTP 요청/응답 처리
Service: 비즈니스 로직 담당
Repository: 데이터 접근 (DB, JPA 등)

com.example.demo
├── controller
├── service
├── repository
└── domain (또는 entity)
장점

책임이 명확하게 분리되어 유지보수 용이
테스트 코드 작성이 용이
규모 확장에 유리

단점

단순 CRUD 프로젝트에서는 계층이 과도할 수 있음 (계층은 많으나 Controller나 Service에서 역할이 단순 전달밖에 없는 상황 발생)
비즈니스 로직이 복잡해지면 service 계층이 비대해짐

https://ggobugi.tistory.com/269

# JPA

그냥 sql query문 안쓰고 get,set 함수로 db 저장, 조회를 할 수 있게 도와주는 친구다.

또한, prepare statement(그냥 쿼리문 미리 디버깅해놓는다는 거임) 그래서 sqli도 어느정도 방지가 된다.

# dto

그릇에 담고 옮겨주는 친구다. 그래서 받아도 되는 값만 받아서 사용하는 등 불필요한 정보를 제외하고 줄 수 있다. 

# MYSQL Version

8.0.46
https://dev.mysql.com/downloads/installer/

# MYSQL 설정

datasource:
url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/shop?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
username: ${SPRING_DATASOURCE_USERNAME:root}
password: ${SPRING_DATASOURCE_PASSWORD:1q2w3e}

이거 너희들 서버에 맞춰서 바꿔 줘야함.

# 관리자 페이지 아이디 비번
admin / admin1234!

로컬 관리자 페이지: http://127.0.0.1:5173/admin/login
사용자 사이트와 관리자 세션 쿠키가 섞이지 않도록 관리자 페이지는 127.0.0.1 주소를 사용한다.

# OAuth 관련
google OAuth 설정 방법
https://goldenrabbit.co.kr/articles/o4WsLGIBrgPikDI5ZA8M

kakao OAuth 설정 방법
https://built.tistory.com/147

LINE OAuth 설정 방법


.gitignore을 통해서 업로드 안되게 해놓았으므로 .env파일을 생성해서 아래 포멧을 넣고 각 data를 넣어주면 된다. 포멧은 아래 작성햊둠

# redis 설정
설치 방법 wsl에다가 하는거
https://chooobb.tistory.com/33

Spring Boot 실행 전에 Redis가 `localhost:6379`에서 실행 중이어야 한다.

```
properties
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
# 운영 환경에서 인증을 사용하는 경우에만 설정
REDIS_PASSWORD=
```

확인 명령은 `redis-cli ping`이며 `PONG`이 반환되어야 한다. Refresh Token 원문은 HttpOnly 쿠키에만 저장한다. Redis는 Refresh Token 해시, Token Family, 사용 완료 토큰과 Access Token `jti` 블랙리스트를 TTL과 함께 관리한다.

# 외부 상품 검색 설정

상품 검색은 SerpApi Google Shopping 결과를 사용한다. 상품을 선택하면 원본 판매처의 JSON-LD와 메타데이터를 우선 읽고, 로컬 Ollama가 원본 페이지에서 확인되는 옵션·상세 설명·리뷰를 보완한다. 검색 결과는 24시간, 상품 상세과 허용된 이미지 주소는 7일 동안 Redis에 저장한다.

SerpApi 무료 플랜을 보호하기 위해 Account API로 남은 호출량을 확인하며 기본적으로 마지막 10회는 사용하지 않고 남겨 둔다. 같은 검색어와 카테고리는 Redis 캐시가 유지되는 동안 다시 과금 호출하지 않는다.

```properties
PRODUCT_SEARCH_PROVIDER=serpapi
SERPAPI_API_KEY={your}
PRODUCT_SEARCH_CACHE_TTL=P1D
PRODUCT_DETAIL_CACHE_TTL=P7D
SERPAPI_QUOTA_RESERVE=10

PRODUCT_AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b
```

Ollama 모델 준비 명령은 `ollama pull qwen2.5:7b`이다.

# 일본 결제 설정

클라이언트 주문서는 SB Payment Service의 링크형 결제를 사용하며 결제 통화는 JPY이다. 사용자는 주문서에서 계약된 결제수단을 먼저 선택하고, SBPS는 선택한 결제수단의 보안 결제화면을 바로 표시한다. 결제 결과는 브라우저 응답만 신뢰하지 않고 SBPS의 해시 서명을 백엔드에서 검증한다.

```properties
SBPS_ENABLED=true
SBPS_MERCHANT_ID=
SBPS_SERVICE_ID=
SBPS_HASH_KEY=
SBPS_REQUEST_URL=

# SBPS에서 계약 및 승인된 결제수단만 쉼표로 등록한다.
# credit3d2=3D Secure 카드, paypay=PayPay, paypal=PayPal
SBPS_PAYMENT_METHODS=credit3d2,paypay,paypal

# 외부에서 HTTPS로 접근 가능한 백엔드 주소. localhost는 사용할 수 없다.
SBPS_CALLBACK_BASE_URL=https://api.example.com

# 결제 후 돌아갈 클라이언트 주소
SBPS_CLIENT_BASE_URL=https://www.example.com

# 한국 원화를 일본 엔으로 바꾸는 운영 적용환율. 비우면 세관 고시환율로 자동 계산한다.
PAYMENT_KRW_TO_JPY_RATE=

# 자동 운영환율에 적용할 환율 변동 보호계수. 1.03은 3%를 더 적용한다.
PAYMENT_AUTOMATIC_RATE_MARKUP=1.03

# 일본 관세청 자료 확인 주기. 운영 환경에서는 최소 하루 한 번 확인한다.
JAPAN_CUSTOMS_REFRESH_INTERVAL=PT24H

# 앱 시작 후 첫 관세청 자료 확인까지 기다리는 시간
JAPAN_CUSTOMS_INITIAL_REFRESH_DELAY=PT1M

# 관세청 자료를 처음 받기 전 임시 고시환율. 비워두면 관부가세를 미계산 처리한다.
JAPAN_CUSTOMS_KRW_TO_JPY_RATE=

# 일본 세관의 수입 소비세 계산 기준 페이지
JAPAN_CUSTOMS_CONSUMPTION_TAX_URL=https://www.customs.go.jp/english/c-answer_e/imtsukan/1111_e.htm

# 구매대행 수수료율. 예: 5%는 0.05
SHOP_AGENCY_FEE_RATE=0

# 원본 페이지에서 국내 배송비를 확인하지 못했을 때 적용할 예상값(원)
SHOP_DEFAULT_DOMESTIC_SHIPPING_FEE_KRW=3000
```

`SBPS_MERCHANT_ID`, `SBPS_SERVICE_ID`, `SBPS_HASH_KEY`, `SBPS_REQUEST_URL`은 SBPS가 시험 또는 운영 환경 구축 후 제공하는 값을 사용한다. `SBPS_CALLBACK_BASE_URL`의 `/api/payment/sbps/callback`과 `/api/payment/sbps/return/*` 경로는 SBPS 서버에서 접근 가능해야 한다. 원화 상품은 `PAYMENT_KRW_TO_JPY_RATE`를 이용해 상품 단가부터 엔화로 올림 환산하며, 필수 설정이 없으면 실제 결제를 시작하지 않는다.

상품 상세의 운영환율은 `PAYMENT_KRW_TO_JPY_RATE`가 있으면 그 값을 우선 사용한다. 비어 있으면 매일 확인한 일본 세관 고시환율에 `PAYMENT_AUTOMATIC_RATE_MARKUP`을 적용해 자동 계산한다. 관부가세 계산용 환율, 간이세율, 일본 소비세율과 공식 출처는 상품 상세에서 저장 자료가 없을 때 즉시 확인하고 앱 시작 1분 후부터 24시간마다 갱신한다. 최신본은 Redis에 캐시하며 날짜별 이력은 `japan_customs_snapshots` 테이블에 저장한다. 공식 사이트 확인에 실패하면 DB의 마지막 정상 자료를 유지하고, 36시간 이상 지난 자료는 화면에 마지막 저장 자료로 표시한다. 테이블은 `db/customs-schema.sql`로 앱 시작 시 안전하게 생성한다.

관세 품목은 `PRODUCT_AI_PROVIDER=ollama`이고 Ollama가 실행 중이면 AI가 HS 코드 후보와 간이세율 그룹을 제안한다. AI 결과는 신뢰도 0.70 이상일 때만 사용하며 실제 금액은 DB에 저장된 공식 세율로 계산한다. AI가 꺼져 있거나 응답이 불확실하면 규칙 분류로 대체한다. 니트, 신발, 가죽제품처럼 일반세율과 상세 재질 확인이 필요한 품목은 AI가 임의 세율을 만들지 않고 통관 후 확정 대상으로 남긴다.

국내 배송비는 원본 페이지의 구조화 데이터와 배송 문구에서 먼저 가져오고 Ollama가 활성화된 경우 AI 추출로 보완한다. 확인하지 못하면 `SHOP_DEFAULT_DOMESTIC_SHIPPING_FEE_KRW`를 예상값으로 적용한다. 국제배송비는 상품명과 카테고리의 예상 중량 및 우체국 EMS 일본 공개요금으로 범위를 계산하고 그 중간값을 최초 결제에 포함한다. 계산 가능한 예상 관세와 일본 소비세도 별도 항목으로 최초 결제에 포함하며, 국제배송비에는 관부가세가 포함되지 않는다. 입고 및 통관 후 확정액과의 차이는 추가 청구하거나 환불한다.

# .env 파일 format
```
# Google Cloud Console OAuth 2.0 Client credentials
GOOGLE_CLIENT_ID={your}
GOOGLE_CLIENT_SECRET={your}

# OAuth KAKAO
KAKAO_CLIENT_ID={your}
KAKAO_CLIENT_SECRET={your}
KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao

# OAuth LINE
LINE_CHANNEL_ID={your}
LINE_CHANNEL_SECRET={your}
LINE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/line

# Spring datasource
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=1q2w3e

# JWT RS256 key pair (PKCS#8 private key / X.509 public key)-> dev용 일단 고정해서 사용
JWT_PRIVATE_KEY=MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC4m3pWT3o4pQ1oBF7OKjl6MrHa/1xR/RMPJob6gxyG0UAB1F6sdgwR+l42I9vm1nOU85pUCI5kSN85C94PiTK5QagMkqT+c26PtreuKkuMcLoTxUIvGFd06cNWAW+ZQmOrp1czOD3F2ib6hmqtiqAZQdS0SR9WkjhplZaaXcllO6ddGaU6d5FtxKJzLtMhe/fyUVvQdIP5nfxCUAdaB0JIuVs5UFiVLxSJmaI8bZnb3YbotMfXumIynFqfm5etuROXKKWYwBdnco3mH9S8x9ZSrNmJRkcYwBYz9NuInFlgJx0DXbReVh05ollJVbVzzjK36bwXbsreaJynd+cP+qN5AgMBAAECggEAH+kcSgwd++mn+hl7S9bnrZRSwyDOFAwTfdRyk54TUcic8FQF83jKWFc3btNfATWDsDU1sZ3zAynHkERZY8UbBAPx6Q52P9ezPltev9fmzEV2HNHFwX9LKtX3oofIAy2DLhpUi/GGWAuAHmEa0pm6V/NVsb5MEPpgjvvBFzjsBbfwtjSe3Mzom+foZVZF3+mLi0fPl+MpV37eRpLwFcpcQb7R6gcjTMMQBiAmIqsCpqU1IY3fP50qrMl/V8kAacGp9IUz9SRXkuMsxywVSVZKcJ49C8LnhKVTQRP9GPTHNgDRrTgz1xNpnqZ5A/G1d6MpjgTRtm77pa5ks4bu7bS/+wKBgQD5XlHyp55ThNUClU4uYE2xDBfM0MWy6UHZheSdcb1vHUlWdR3n4XO5h7WdvJ03LS4p4VPh727kNROFAxXsmb6SkY3SmvM4g+/Y0BX2Y49LliL7kbn0JJ/DNBT5kd13Op85NhKTA3wXpiWbLZFe5kRBR15BLZAiHZxPBNNrI8ZCnwKBgQC9hET9a2jAbZix59P75izgenhcZcwCB6qRGBJc+ep7v1bDCz5abD8g52y3rpO22x2Z0o5QHiLgkxsealX1rdiALn33mny2DJZ/1nCiIIJ/ntF8wD2wPItwvZEgB7gt6TGeUAZf+oXC3aPd+S0DcP6rCCglXRSh5BVuwRn1yjO65wKBgAiDBK8Qs6HMLe0ppYKFvQEnFYTjN2xU8+9114jcQggR6tftpXitxGJ31GYu8vrYKvQ5AcD7iEB341Ot+EicMtoT59Bhg53ROxWefLFtUo3U8Rq/Laa8vUtPcsvWA8Y3FQpB4z5rZwfznuL+GjEwEPJdekUlRf80HYrA8mp8zR6nAoGBAK0b38S30tRKh7qhrwGeC4n0dHK5zXJhPE0SdtuUsLneWcsJPyhrTfKpUaYqgeT6GvzB1pqaHHOUC6ZprSEfbe4QCaIc7COCNgqJfxKorWsTT/kqxG5xr1EI50IhqvvM0TTxlhEfjgUkXSVKEPLeyNDDP6B5eHo0E9u56ROZYk0rAoGBAKdwsNdWoMXrr4HQ9LzEEzDZo9xjw12Elajb4ESUMcMqrymtiaxlPGmH2D05qpu2ezFgamjrDRvnO8JO/JuZ4TzFTsjedRWbLZmURhQgSAW0o78ihndN03aIm4Mo/aM9ezInItR9CkQ8ELAdeEi688IkVCKXBJs94EV6okR9G1Iw
JWT_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuJt6Vk96OKUNaARezio5ejKx2v9cUf0TDyaG+oMchtFAAdRerHYMEfpeNiPb5tZzlPOaVAiOZEjfOQveD4kyuUGoDJKk/nNuj7a3ripLjHC6E8VCLxhXdOnDVgFvmUJjq6dXMzg9xdom+oZqrYqgGUHUtEkfVpI4aZWWml3JZTunXRmlOneRbcSicy7TIXv38lFb0HSD+Z38QlAHWgdCSLlbOVBYlS8UiZmiPG2Z292G6LTH17piMpxan5uXrbkTlyilmMAXZ3KN5h/UvMfWUqzZiUZHGMAWM/TbiJxZYCcdA120XlYdOaJZSVW1c84yt+m8F27K3micp3fnD/qjeQIDAQAB

# Redis refresh token store
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0

# data aes-256-gcm -> dev용 일단 고정해서 사용
PII_ENCRYPTION_KEYS=v1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
PII_CURRENT_KEY_VERSION=v1
PII_MIGRATE_PLAINTEXT_ON_STARTUP=trues

# 회원가입 완료 메일 (Gmail은 계정 비밀번호가 아닌 앱 비밀번호 사용)

```

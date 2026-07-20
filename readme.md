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

# OAuth 관련
설정 방법
https://goldenrabbit.co.kr/articles/o4WsLGIBrgPikDI5ZA8M

.env파일 format이다. .gitignore을 통해서 업로드 안되게 해놓았으므로 .env파일을 생성해서 아래 포멧을 넣고 각 data를 넣어주면 된다.
```
Google Cloud Console OAuth 2.0 Client credentials
GOOGLE_CLIENT_ID=change_user_client_id
GOOGLE_CLIENT_SECRET=change_secret
```

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

# .env 파일 format
```
# Google Cloud Console OAuth 2.0 Client credentials
GOOGLE_CLIENT_ID={your}
GOOGLE_CLIENT_SECRET={your}

# Spring datasource
SPRING_DATASOURCE_USERNAME={your}
SPRING_DATASOURCE_PASSWORD={your}

# JWT RS256 key pair (PKCS#8 private key / X.509 public key)
JWT_PRIVATE_KEY={your}
JWT_PUBLIC_KEY={your}

# Redis refresh token store
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=0
```

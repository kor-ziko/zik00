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
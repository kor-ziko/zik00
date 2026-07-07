# Shop

Spring Boot MVC + Thymeleaf mypage sample.

## Data Access

The project now uses MySQL through Spring Data JPA.

- `controller`: handles HTTP requests and view routing.
- `service`: owns business logic and transaction boundaries.
- `repository`: uses `JpaRepository` interfaces for database access.
- `domain`: JPA entities mapped to MySQL tables.
- `dto`: request/view objects used between controller, service, and templates.

## Tables

The previous CSV names are mapped as table names with the `test_` prefix removed.

- `test_user.csv` -> `user`
- `test_addresses.csv` -> `addresses`
- `test_coupon.csv` -> `coupon`
- `test_buylist.csv` -> `buylist`
- `test_inquiries.csv` -> `inquiries`
- `test_inquiry_comments.csv` -> `inquiry_comments`

## Configuration

Default datasource values are in `src/main/resources/application.yaml`.
Override them with environment variables when needed:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

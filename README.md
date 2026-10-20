# Task Manager

RESTful-сервис управления задачами на **Spring Boot 2.7** (совместим с **Java 11**) с аутентификацией по **JWT**, ролями `USER`/`ADMIN`, хранением паролей через **BCrypt**, базой **H2**, документацией **Swagger/OpenAPI** и тестами **JUnit 5 + Mockito**.

## Стек

- Java 11, Spring Boot 2.7.18, Gradle 8.10.2
- Spring Web, Spring Data JPA (Hibernate 5.6)
- Spring Security 5.7 + JWT (jjwt 0.12.6)
- H2 (in-memory), Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, Spring MockMvc

## Запуск

Приложение поднимется на `http://localhost:8080`.

### База данных на выбор: H2 или PostgreSQL

По умолчанию используется **H2** (in-memory, данные сбрасываются при остановке). Переключение на **PostgreSQL** — через Spring-профиль.

```bash
# H2 (по умолчанию)
./gradlew bootRun

# PostgreSQL
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

### PostgreSQL через Docker (docker-compose)

В репозитории есть `docker-compose.yml` — поднимает Postgres под значения по умолчанию:

```bash
docker compose up -d        # старт БД
docker compose logs -f postgres   # логи
docker compose down         # остановить (данные в volume сохраняются)
docker compose down -v      # остановить и удалить данные
```

Запуск приложения на Postgres:

```bash
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

Параметры PostgreSQL настраиваются через переменные окружения (есть значения по умолчанию):

| Переменная | По умолчанию |
|------------|--------------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/taskmanager` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `password` |

```bash
POSTGRES_URL=jdbc:postgresql://localhost:5432/taskdb \
POSTGRES_USER=app \
POSTGRES_PASSWORD=secret \
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

- H2: `spring.jpa.hibernate.ddl-auto=create-drop` (таблицы создаются/удаляются, H2 Console включена).
- PostgreSQL: `ddl-auto=update` (таблицы создаются при первом запуске, данные сохраняются между запусками).

### Запуск на Windows

Вместо `./gradlew` используется `gradlew.bat` (он есть в репозитории).

```bat
:: Windows (Command Prompt)
gradlew.bat bootRun

:: с профилем PostgreSQL
gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

PowerShell:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Задать переменные окружения для Postgres в PowerShell:

```powershell
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/taskmanager"
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="password"
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

Примечания:
- Docker Compose (`docker compose up -d`) работает на Windows одинаково (WSL2/Docker Desktop).
- curl в Windows (PowerShell): для однотипных запросов удобнее Swagger UI: http://localhost:8080/swagger-ui.html

| Что | URL |
|-----|-----|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 Console | http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:taskdb`, user `sa`, пустой пароль) |

### Дефолтные пользователи (DatabaseInitializer)

| Логин | Пароль | Роль |
|-------|--------|------|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | USER |

## Аутентификация

1. `POST /api/auth/register` — создать пользователя (`username`, `email`, `password`).
2. `POST /api/auth/login` — получить JWT.
3. Добавить заголовок к защищённым запросам: `Authorization: Bearer <token>`.

Токен содержит `username`, `role` и `expiration` (по умолчанию 24 часа). Настраивается в `application.properties`:
`app.jwt.secret`, `app.jwt.expiration-ms`.

### Пример запроса на вход

**Запрос:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

**Ответ (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcyOTM1MzQ1NiwicmV4cCI6MTcyOTQ0MDAwMH0.xxxx",
  "type": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresIn": 86400000
}
```

**Использование токена в защищённых запросах:**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl http://localhost:8080/api/tasks -H "Authorization: Bearer $TOKEN"
```

При неверном пароле ответ будет `400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid username or password",
  "path": "/api/auth/login",
  "timestamp": "2026-10-19T19:30:00",
  "fieldErrors": null
}
```

## API

### AuthController

| Метод | Путь | Действие | Доступ |
|-------|------|----------|--------|
| `POST` | `/api/auth/register` | Регистрация | Публичный |
| `POST` | `/api/auth/login` | Вход (JWT) | Публичный |

### TaskController

| Метод | Путь | Действие | Доступ |
|-------|------|----------|--------|
| `POST` | `/api/tasks` | Создать задачу | USER, ADMIN |
| `GET` | `/api/tasks/{id}` | Получить задачу | USER, ADMIN |
| `PUT` | `/api/tasks/{id}` | Обновить задачу | Только автор или ADMIN |
| `DELETE` | `/api/tasks/{id}` | Удалить задачу | Только автор или ADMIN |
| `GET` | `/api/tasks` | Список задач с фильтрами | USER, ADMIN |

Фильтрация (комбинируется): `?status=TODO`, `?assigneeId=1`, `?authorId=2`.

### Примеры запросов

```bash
# Предполагаем, что TOKEN получен из /api/auth/login (см. выше)

# ─── Аутентификация ───

# POST /api/auth/register — регистрация (публичный)
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'

# POST /api/auth/login — вход, получение JWT (публичный)
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"john","password":"secret123"}'

# ─── Задачи (все требуют Authorization: Bearer <token>) ───

# POST /api/tasks — создать задачу
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Написать отчёт","description":"Квартальный отчёт","status":"TODO","priority":"MEDIUM","assigneeId":2}'

# GET /api/tasks/{id} — получить задачу по ID
curl http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer $TOKEN"

# PUT /api/tasks/{id} — обновить задачу (только автор или ADMIN)
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Отчёт обновлён","description":"Новая версия","status":"IN_PROGRESS","priority":"HIGH","assigneeId":2}'

# DELETE /api/tasks/{id} — удалить задачу (только автор или ADMIN), ответ 204
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer $TOKEN"

# GET /api/tasks — список задач с фильтрами
curl "http://localhost:8080/api/tasks" -H "Authorization: Bearer $TOKEN"

# Фильтрация:
# по статусу
curl "http://localhost:8080/api/tasks?status=IN_PROGRESS" -H "Authorization: Bearer $TOKEN"
# по исполнителю
curl "http://localhost:8080/api/tasks?assigneeId=2" -H "Authorization: Bearer $TOKEN"
# по автору
curl "http://localhost:8080/api/tasks?authorId=1" -H "Authorization: Bearer $TOKEN"
# фильтры комбинируются
curl "http://localhost:8080/api/tasks?status=TODO&authorId=1" -H "Authorization: Bearer $TOKEN"
```

### Формат ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task not found (id=999)",
  "path": "/api/tasks/999",
  "timestamp": "2026-10-19T14:00:00",
  "fieldErrors": null
}
```

- `400` — валидация / конфликт (дубликат username/email)
- `401` — нет/невалидный токен
- `403` — доступ запрещён (не автор и не админ)
- `404` — задача/исполнитель не найдены

## Тесты

```bash
./gradlew test
```

14 тестов: unit-тесты сервисов (Mockito) и интеграционные тесты REST-потока (MockMvc + реальный JWT).

## Структура проекта (Spring Boot + DTO)
```
src/main/java/com/dvp/taskmanager/
├── model/          # JPA-сущности: User, Task, Role, TaskStatus, TaskPriority
├── repository/     # Spring Data: UserRepository, TaskRepository
├── dto/            # RegisterCommand, Credentials, TaskCommand, TaskSearchQuery,
│                   # UserView, TaskView, TokenPayload, ApiError
├── service/        # AuthService, TaskService (бизнес-логика + маппинг → DTO)
├── controller/     # AuthController, TaskController
├── security/       # TokenService (JWT), JwtTokenFilter, AppUserDetailsService, AuthenticatedUserResolver
├── exception/      # BadRequestException, ForbiddenException, NotFoundException, ApiExceptionAdvice
└── config/         # SecurityConfiguration, JwtProperties, OpenApiConfiguration,
                    # DatabaseInitializer, RestAuthenticationEntryPoint, RestAccessDeniedHandler
```
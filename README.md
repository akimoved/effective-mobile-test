# 🚀 Система Управления Банковскими Картами

## 🏗️ Архитектура проекта

```plaintext
src/main/java/com/example/bankcards/
├── config/          # Конфигурация приложения
├── controller/      # REST контроллеры
├── dto/            # Data Transfer Objects
├── entity/         # JPA сущности
├── exception/      # Обработка исключений
├── repository/     # Репозитории для работы с БД
├── security/       # Настройки безопасности
├── service/        # Бизнес-логика
└── util/           # Утилитарные классы
```

## 🔧 Созданные компоненты

### JPA Сущности
- `User` - пользователи системы
- `Role` - роли пользователей
- `Card` - банковские карты
- `Transaction` - транзакции между картами

### Enum классы
- `CardStatus` - статусы карт (ACTIVE, BLOCKED, EXPIRED)
- `RoleName` - роли (ADMIN, USER)
- `TransactionStatus` - статусы транзакций

### База данных
- Liquibase миграции в YAML формате
- Автоматическое создание схемы БД
- Начальные данные (роли и тестовые пользователи)

## 💡 Технологии

Java 21, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Liquibase, Docker, JWT, Swagger (OpenAPI)

## 🚀 Запуск приложения

### 📋 Предварительные требования

- Java 17+
- Maven 3.6+
- Docker и Docker Compose

### ⚙️ Настройка переменных окружения

Создайте файл `.env` в корне проекта со следующими переменными:

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5433
DB_NAME=bankcards
DB_USERNAME=bankcards_user
DB_PASSWORD=bankcards_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_at_least_32_characters_long
JWT_EXPIRATION=86400000

# Application Configuration
SERVER_PORT=8080
```

### 🗄️ Варианты развертывания

#### Вариант 1: Полное развертывание через Docker Compose (рекомендуемый)

Запустите все сервисы (PostgreSQL + приложение) в Docker:

```bash
docker-compose up -d
```

Приложение будет доступно на порту 8080, PostgreSQL на порту 5433.

#### Вариант 2: Только база данных в Docker

Запустите только PostgreSQL в Docker контейнере:

```bash
docker-compose up -d postgres
mvn spring-boot:run
```

#### Вариант 3: Использование локального PostgreSQL

Создайте базу данных и пользователя в локальном PostgreSQL:

```sql
CREATE DATABASE bankcards;
CREATE USER bankcards_user WITH PASSWORD 'bankcards_password';
GRANT ALL PRIVILEGES ON DATABASE bankcards TO bankcards_user;
```

Обновите `.env` файл:

```env
DB_PORT=5432
```

Запустите приложение:

```bash
mvn spring-boot:run
```

### 📚 Доступ к документации API

После запуска приложения Swagger UI будет доступен по адресу:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### 👤 Тестовые пользователи

- **Администратор:** username: `admin`, password: `admin123`
- **Пользователь:** username: `user`, password: `user123`

## 📝 Описание проделанной работы

- Приложение разработано для управления банковскими картами, включая создание, просмотр, переводы.
- Архитектура соблюдает SOLID принципы, имеет разделение уровней, следуют принципам чистого кода.
- Несмотря на завершение основных функций, некоторые возможности, такие как пополнение баланса или хэш-сервис для возможности сравнения зашифрованных номеров банковских карт.
- Планируемые улучшения: OAuth2 аутентификация, логирование с помощью аспектов, оптимизация запросов, возможность работы с разными валютами.

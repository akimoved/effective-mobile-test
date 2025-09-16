# 🚀 Система Управления Банковскими Картами

## 📝 Описание задачи

Разработать backend-приложение на Java (Spring Boot) для управления банковскими картами:

- Создание и управление картами
- Просмотр карт
- Переводы между своими картами

## 💳 Атрибуты карты

- Номер карты (зашифрован, отображается маской: `**** **** **** 1234`)
- Владелец
- Срок действия
- Статус: Активна, Заблокирована, Истек срок
- Баланс

## 🧾 Требования

### ✅ Аутентификация и авторизация
- Spring Security + JWT
- Роли: `ADMIN` и `USER`

### ✅ Возможности

**Администратор:**
- Создаёт, блокирует, активирует, удаляет карты
- Управляет пользователями
- Видит все карты

**Пользователь:**
- Просматривает свои карты (поиск + пагинация)
- Запрашивает блокировку карты
- Делает переводы между своими картами
- Смотрит баланс

### ✅ API
- CRUD для карт
- Переводы между своими картами
- Фильтрация и постраничная выдача
- Валидация и сообщения об ошибках

### ✅ Безопасность
- Шифрование данных
- Ролевой доступ
- Маскирование номеров карт

### ✅ Работа с БД
- PostgreSQL
- Миграции через Liquibase

### ✅ Документация
- Swagger UI / OpenAPI
- README.md с инструкцией запуска

### ✅ Развёртывание и тестирование
- Docker Compose для dev-среды
- Liquibase миграции
- Юнит-тесты ключевой бизнес-логики

## 💡 Технологии

Java 17+, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Liquibase, Docker, JWT, Swagger (OpenAPI)

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

#### Вариант 1: Полное развертывание через Docker Compose (рекомендуется)

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

## 📊 Оценка

- Соответствие требованиям
- Чистота архитектуры и кода
- Безопасность
- Обработка ошибок
- Покрытие тестами
- ООП и уровни абстракции

## 📤 Формат сдачи

Весь код и изменения принимаются только через git-репозиторий с открытым доступом к проекту. Отправка файлов в любом виде не принимается.

## 🏗️ Архитектура проекта

```
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

## 💻 IntelliJ IDEA Конфигурации

### External Tools (Внешние инструменты)

В меню **Tools → External Tools** доступны команды:

- **Docker Compose Up** - Запуск всех сервисов
- **Docker Compose Down** - Остановка всех сервисов  
- **Docker Compose PostgreSQL Only** - Запуск только PostgreSQL
- **Docker Compose Logs** - Просмотр логов приложения
- **Docker Compose Build** - Пересборка образа приложения

### Run Configurations (Конфигурации запуска)

- **Spring Boot: Local** - Локальный запуск с подключением к PostgreSQL в контейнере

### Рекомендуемый workflow для разработки

1. Запустите **Docker Compose PostgreSQL Only** (только база данных)
2. Запустите **Spring Boot: Local** для отладки приложения
3. Используйте **Docker Compose Logs** для просмотра логов

Подробные инструкции см. в файле `.idea/README_IDEA_Configurations.md`

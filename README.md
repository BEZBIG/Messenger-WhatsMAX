<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Ktor-2.3.12-087CFA?logo=ktor&logoColor=white" alt="Ktor" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-2024.02-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Firebase-Auth%20%2B%20FCM-FFCA28?logo=firebase&logoColor=black" alt="Firebase" />
  <img src="https://img.shields.io/badge/MinIO-S3_Storage-C72E49?logo=minio&logoColor=white" alt="MinIO" />
</p>

# WhatsMAX

Полнофункциональный мессенджер на Kotlin — клон Telegram/WhatsApp с серверной частью на **Ktor** и Android-клиентом на **Jetpack Compose**.

---

## Возможности

- **Личные и групповые чаты** — создание, поиск, удаление
- **Сообщения в реальном времени** через WebSocket
- **Каналы** (Telegram-style) — посты, комментарии, подписки
- **Отправка медиа** — фото, файлы, голосовые сообщения с waveform
- **Реакции** на сообщения, посты и комментарии
- **Ответ / редактирование / удаление** сообщений
- **Аудио- и видеозвонки** (WebRTC signaling)
- **Push-уведомления** через Firebase Cloud Messaging
- **Онлайн/офлайн статусы** и индикатор набора текста
- **Статусы прочтения** (галочки)
- **Поиск** пользователей и каналов (trigram-индексы)
- **Профили** с аватарами, bio, username
- **Офлайн-кэш** через Room
- **Тёмная тема** (Material 3)

---

## Технологический стек

| Слой | Технологии |
|------|-----------|
| **Backend** | Ktor 2.3.12, Exposed 0.46 (ORM), PostgreSQL 16, HikariCP, Firebase Admin SDK, MinIO (S3), Redis pub/sub, kotlinx.serialization |
| **Android** | Jetpack Compose (BOM 2024.02), Hilt 2.50, Firebase Auth + FCM, Retrofit 2.9, OkHttp 4.12, Room 2.6, Coil 2.5, Stream WebRTC, CameraX, Coroutines + Flow, Navigation Compose |
| **Инфраструктура** | Docker Compose (Postgres + MinIO + Redis + Backend), Gradle 8.4, KSP |

---

## Архитектура

### Backend

```
backend/src/main/kotlin/com/whatsmax/
├── Application.kt              — точка входа Ktor
├── plugins/                    — Database, Auth, WebSocket, CORS, RateLimit, Routing
├── routes/                     — REST-маршруты (Auth, User, Chat, Message, Channel, File, Reaction)
├── data/
│   ├── database/tables/        — Exposed-таблицы PostgreSQL
│   └── repositories/           — реализации репозиториев
├── domain/
│   ├── models/                 — доменные модели и DTO
│   └── repositories/           — интерфейсы репозиториев
├── websocket/                  — WebSocketManager + RedisBroker
└── utils/                      — FirebaseAdmin, StorageService (MinIO), Validation, MimeDetector
```

### Android (Clean Architecture)

```
android/app/src/main/kotlin/com/whatsmax/
├── presentation/               — UI: Compose-экраны + ViewModels
│   ├── auth/                       login, register
│   ├── home/                       список чатов
│   ├── chat/                       переписка (пузыри, reply, edit, media, голосовые)
│   ├── channel/                    каналы (список, лента, комментарии, реакции)
│   ├── profile/                    свой профиль, профиль пользователя
│   ├── call/                       аудио/видеозвонок (WebRTC + CameraX)
│   └── voice/                      запись и плеер голосовых
├── domain/                     — бизнес-логика (Use Cases, интерфейсы, модели)
├── data/                       — реализации (Retrofit, WebSocket, Room, FCM, DTO)
└── di/                         — Hilt-модули (Network, Repository, Database)
```

```
Presentation  →  Domain  ←  Data
     ↓              ↑         ↑
  ViewModel      Use Cases  Repositories
  + Compose      + Models   + API + WS + DB
```

> `domain` не зависит ни от чего — presentation и data зависят только от domain.

---

## Быстрый старт

### Предварительные требования

- **JDK 17**
- **PostgreSQL 16** (или Docker)
- **Android Studio Hedgehog+** (для Android-клиента)
- **Firebase-проект** с включённым Email/Password Authentication

### 1. Клонирование

```bash
git clone https://github.com/<your-username>/Messenger-WhatsMAX.git
cd Messenger-WhatsMAX
```

### 2. Firebase

1. Создайте проект в [Firebase Console](https://console.firebase.google.com)
2. Включите **Authentication > Email/Password**
3. **Backend**: `Project Settings > Service accounts > Generate new private key`
   — сохраните как `backend/firebase-service-account.json`
4. **Android**: `Project Settings > General > Add Android app`
   — Package: `com.whatsmax.messenger`
   — скачайте `google-services.json` в `android/app/`

### 3. Запуск через Docker (рекомендуется)

Один файл поднимает весь стек — PostgreSQL, MinIO, Redis и backend:

```bash
cd backend
docker compose up -d --build
```

| Сервис | URL |
|--------|-----|
| Backend API | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |
| PostgreSQL | localhost:5432 |

Проверка:
```bash
curl http://localhost:8080/auth/me
# Ожидаемый ответ: 401 (нет токена) — значит сервер работает
```

### 4. Запуск без Docker (вручную)

<details>
<summary>Развернуть инструкцию</summary>

#### PostgreSQL

```sql
CREATE DATABASE whatsmax;
-- пользователь postgres с паролем из application.conf (по умолчанию 08965)
```

#### MinIO (для файлов/фото)

Скачайте [MinIO](https://min.io/download) и запустите:
```bash
minio server ./data --console-address ":9001"
```

Создайте bucket:
```bash
mc alias set local http://localhost:9000 whatsmax_admin whatsmax_secret_change_me
mc mb local/whatsmax-files
mc anonymous set download local/whatsmax-files
```

#### Backend

```bash
cd backend
./gradlew run
```

Сервер запустится на `http://localhost:8080`.

</details>

### 5. Запуск Android

1. Откройте папку `android/` в Android Studio
2. Убедитесь, что `google-services.json` в `android/app/`
3. Настройте адрес сервера в `app/build.gradle.kts`:

```kotlin
// Эмулятор (10.0.2.2 = localhost хоста)
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
buildConfigField("String", "WS_URL",   "\"ws://10.0.2.2:8080/ws\"")

// Реальное устройство — IP вашего компьютера:
buildConfigField("String", "BASE_URL", "\"http://192.168.1.X:8080\"")
buildConfigField("String", "WS_URL",   "\"ws://192.168.1.X:8080/ws\"")
```

4. **Run** (Shift+F10)

---

## Переменные окружения

| Переменная | Описание | По умолчанию |
|-----------|----------|-------------|
| `PORT` | Порт HTTP-сервера | `8080` |
| `DATABASE_URL` | JDBC-строка PostgreSQL | `jdbc:postgresql://localhost:5432/whatsmax` |
| `DATABASE_USER` | Пользователь БД | `postgres` |
| `DATABASE_PASSWORD` | Пароль БД | `08965` |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Путь к JSON-ключу Firebase | `./firebase-service-account.json` |
| `MINIO_ENDPOINT` | S3 API endpoint | `http://localhost:9000` |
| `MINIO_PUBLIC_ENDPOINT` | Публичный endpoint для клиентов | = `MINIO_ENDPOINT` |
| `MINIO_ACCESS_KEY` | Логин MinIO | `whatsmax_admin` |
| `MINIO_SECRET_KEY` | Пароль MinIO | `whatsmax_secret_change_me` |
| `REDIS_ENABLED` | Включить Redis pub/sub | `false` |
| `REDIS_URL` | URL Redis | `redis://localhost:6379` |
| `CORS_ALLOWED_HOSTS` | Разрешённые origins (через запятую) | пусто = anyHost |

---

## API

Все запросы требуют заголовок `Authorization: Bearer <Firebase_ID_Token>`.

| Метод | Endpoint | Описание |
|-------|---------|---------|
| `POST` | `/auth/register` | Регистрация профиля |
| `GET` | `/auth/me` | Текущий пользователь |
| `POST` | `/auth/sign-out` | Выход (инвалидация кеша) |
| `GET` | `/users/search?q=...` | Поиск пользователей |
| `GET` | `/users/{uid}` | Профиль по UID |
| `PUT` | `/users/me` | Обновление профиля |
| `POST` | `/chats` | Создать чат (direct/group) |
| `GET` | `/chats` | Мои чаты |
| `GET` | `/chats/{id}/messages` | История сообщений |
| `POST` | `/chats/{id}/messages` | Отправить сообщение |
| `PUT` | `/chats/{id}/messages/{msgId}` | Редактировать |
| `DELETE` | `/chats/{id}/messages/{msgId}` | Удалить |
| `POST` | `/channels` | Создать канал |
| `GET` | `/channels/search?q=...` | Поиск каналов |
| `POST` | `/channels/{id}/subscribe` | Подписаться |
| `POST` | `/channels/{id}/messages` | Опубликовать пост |
| `POST` | `/channels/{id}/messages/{msgId}/comments` | Комментарий |
| `GET/PUT/DELETE` | `/reactions/{type}/{entityId}` | Реакции |
| `POST` | `/files/upload` | Загрузка файла (multipart) |
| `WS` | `/ws` | WebSocket (auth первым кадром) |

### WebSocket-события

Подключение: `ws://host:8080/ws` — токен отправляется первым JSON-кадром `{"type":"auth","payload":"<token>"}`.

**Входящие (сервер -> клиент):**
```
new_message, message_edited, message_deleted, message_read
user_online, user_offline
call_offer, call_answer, call_ice, call_end
```

**Исходящие (клиент -> сервер):**
```
auth, user_typing, message_read
call_offer, call_answer, call_ice, call_end
```

---

## Частые проблемы

| Проблема | Решение |
|---------|---------|
| `Connection refused` на эмуляторе | Используйте `10.0.2.2` вместо `localhost` |
| `401 Unauthorized` на все запросы | Проверьте `firebase-service-account.json` в `backend/` |
| `Database connection failed` | Убедитесь что PostgreSQL запущен (`pg_isready`) |
| Google Sign-In не работает | Добавьте SHA-1 debug-ключа в Firebase Console |
| Файлы не загружаются | Проверьте что MinIO запущен и bucket создан |

---

## Лицензия

Учебный проект. Свободное использование.

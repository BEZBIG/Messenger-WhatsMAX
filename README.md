# WhatsMAX — Мессенджер (Клиент-Серверное Приложение)

> **Telegram/WhatsApp clone** на Kotlin
> Backend: **Ktor** + **PostgreSQL** | Android: **Jetpack Compose** + **Clean Architecture** + **Firebase Auth**

---

## Архитектура

```
Messenger-WhatsMAX/
├── backend/          ← Ktor сервер (порт 8080)
│   └── src/main/kotlin/com/whatsmax/
│       ├── Application.kt         ← точка входа
│       ├── plugins/               ← Database, Auth, WebSocket, CORS, Routing
│       ├── data/
│       │   ├── database/tables/   ← Exposed ORM таблицы PostgreSQL
│       │   └── repositories/      ← реализации репозиториев
│       ├── domain/
│       │   ├── models/            ← доменные модели + DTO
│       │   └── repositories/      ← интерфейсы репозиториев
│       ├── routes/                ← HTTP-маршруты (Auth, Chat, Message, Channel, File)
│       ├── websocket/             ← WebSocketManager (real-time)
│       └── utils/                 ← FirebaseAdmin
│
└── android/          ← Android приложение (Jetpack Compose)
    └── app/src/main/kotlin/com/whatsmax/
        ├── domain/
        │   ├── model/             ← доменные модели
        │   ├── repository/        ← интерфейсы репозиториев
        │   └── usecase/           ← Use Cases (auth/chat/message/channel/user)
        ├── data/
        │   ├── remote/api/        ← Retrofit ApiService + AuthInterceptor
        │   ├── remote/websocket/  ← WebSocketClient (OkHttp)
        │   ├── remote/dto/        ← Data Transfer Objects
        │   ├── remote/fcm/        ← Firebase Push-уведомления
        │   ├── local/             ← Room кэш
        │   └── repository/        ← реализации репозиториев
        ├── di/                    ← Hilt модули (Network, Repository, Database)
        └── presentation/
            ├── auth/              ← Login, Register экраны
            ├── home/              ← Список чатов
            ├── chat/              ← Экран переписки (пузыри, reply, edit, delete)
            ├── profile/           ← Профиль пользователя
            ├── channel/           ← Каналы (список + лента)
            ├── call/              ← Звонок / Видеозвонок (WebRTC)
            └── theme/             ← Material3 тема
```

---

## Функционал

| Функция                          | Статус |
|----------------------------------|--------|
| Авторизация (Firebase Email)     | ✅     |
| Регистрация                      | ✅     |
| Личные чаты 1-на-1               | ✅     |
| Групповые чаты                   | ✅     |
| Real-time сообщения (WebSocket)  | ✅     |
| Отправка файлов/фото             | ✅     |
| Статусы онлайн/офлайн            | ✅     |
| Ответ на сообщение               | ✅     |
| Редактирование сообщений         | ✅     |
| Удаление сообщений               | ✅     |
| Статус "прочитано" (галочки)     | ✅     |
| Каналы (как в Telegram)          | ✅     |
| Поиск пользователей/каналов      | ✅     |
| Push-уведомления (FCM)           | ✅     |
| Звонки (WebRTC signaling)        | ✅     |
| Видеозвонки (WebRTC signaling)   | ✅     |
| Офлайн-кэш (Room)                | ✅     |
| Создание групп                   | ✅     |

---

## Быстрый старт

### Шаг 1 — Настройка PostgreSQL

```sql
-- Установите PostgreSQL, затем:
CREATE DATABASE whatsmax;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE whatsmax TO postgres;
```

Если пользователь уже существует, просто создайте базу.

---

### Шаг 2 — Настройка Firebase

1. Перейдите на [console.firebase.google.com](https://console.firebase.google.com)
2. Создайте новый проект **WhatsMAX**
3. Включите **Authentication → Email/Password**
4. **Для бэкенда**: `Project Settings → Service accounts → Generate new private key`
   - Сохраните файл как `backend/firebase-service-account.json`
5. **Для Android**: `Project Settings → General → Add Android app`
   - Package name: `com.whatsmax.messenger`
   - Скачайте `google-services.json`
   - Поместите в `android/app/google-services.json`

---

### Шаг 3 — Запуск бэкенда

```bash
cd Messenger-WhatsMAX/backend

# Убедитесь, что PostgreSQL запущен
# Создайте uploads/ папку (создаётся автоматически)

# Запустить в dev-режиме:
./gradlew run

# Или собрать fat-jar и запустить:
./gradlew buildFatJar
java -jar build/libs/whatsmax-backend.jar
```

**Сервер запустится на:** `http://localhost:8080`

Проверка (должен вернуть 401):
```bash
curl http://localhost:8080/auth/me
```

---

### Шаг 4 — Запуск Android

1. Откройте папку `android/` в **Android Studio Hedgehog** или новее
2. Убедитесь, что `google-services.json` находится в `android/app/`
3. В `app/build.gradle.kts` проверьте IP-адрес сервера:
   ```kotlin
   // Эмулятор — 10.0.2.2 это localhost
   buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")

   // Реальное устройство — укажите IP вашего компьютера:
   buildConfigField("String", "BASE_URL", "\"http://192.168.1.X:8080\"")
   ```
4. Нажмите **Run** (Shift+F10) или `./gradlew installDebug`

---

### Шаг 5 — Тестирование бэкенда

```bash
cd backend

# Запустить все тесты:
./gradlew test

# Тест API вручную — сначала получите Firebase ID Token через клиент
# Затем:
TOKEN="ваш_firebase_id_token"

# Регистрация пользователя:
curl -X POST http://localhost:8080/auth/register \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","displayName":"Test User"}'

# Получить свой профиль:
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Создать чат:
curl -X POST http://localhost:8080/chats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"direct","memberUids":["другой_uid"]}'

# Поиск пользователей:
curl "http://localhost:8080/users/search?q=test" \
  -H "Authorization: Bearer $TOKEN"

# Поиск каналов:
curl "http://localhost:8080/channels/search?q=новости" \
  -H "Authorization: Bearer $TOKEN"
```

---

### Шаг 6 — Тестирование Android

#### Юнит-тесты (ViewModel):
```bash
cd android
./gradlew test
```

#### Инструментальные тесты:
```bash
./gradlew connectedAndroidTest
```

#### Запустить тесты конкретного модуля:
```bash
./gradlew :app:test --tests "com.whatsmax.*"
```

---

## API Reference (основные эндпоинты)

Все запросы (кроме регистрации) требуют: `Authorization: Bearer <Firebase_ID_Token>`

| Метод  | URL                                    | Описание                      |
|--------|----------------------------------------|-------------------------------|
| POST   | /auth/register                         | Создать профиль               |
| GET    | /auth/me                               | Мой профиль                   |
| GET    | /users/search?q=...                    | Поиск пользователей           |
| POST   | /chats                                 | Создать чат                   |
| GET    | /chats                                 | Мои чаты                      |
| GET    | /chats/{id}/messages                   | История сообщений             |
| POST   | /chats/{id}/messages                   | Отправить сообщение           |
| POST   | /files/upload                          | Загрузить файл (multipart)    |
| GET    | /channels/search?q=...                 | Поиск каналов                 |
| POST   | /channels                              | Создать канал                 |
| POST   | /channels/{id}/subscribe               | Подписаться на канал          |
| WS     | ws://localhost:8080/ws?token=...       | WebSocket соединение          |

---

## WebSocket Events

Подключение: `ws://localhost:8080/ws?token=<Firebase_ID_Token>`

### Входящие (сервер → клиент):
```json
{"type":"new_message",     "payload":"<MessageJSON>"}
{"type":"message_edited",  "payload":"<MessageJSON>"}
{"type":"message_deleted", "payload":"{\"messageId\":\"...\"}"}
{"type":"message_read",    "payload":"{\"messageId\":\"...\",\"userId\":\"...\"}"}
{"type":"user_online",     "payload":"{\"uid\":\"...\"}"}
{"type":"user_offline",    "payload":"{\"uid\":\"...\"}"}
{"type":"call_offer",      "payload":"<CallSignalJSON>"}
{"type":"call_answer",     "payload":"<CallSignalJSON>"}
{"type":"call_ice",        "payload":"<CallSignalJSON>"}
```

### Исходящие (клиент → сервер):
```json
{"type":"user_typing",  "payload":"{\"chatId\":\"...\",\"isTyping\":true}"}
{"type":"message_read", "payload":"{\"chatId\":\"...\",\"messageId\":\"...\"}"}
{"type":"call_offer",   "payload":"<CallSignalJSON>"}
```

---

## Переменные окружения (бэкенд)

| Переменная                    | По умолчанию                          |
|-------------------------------|---------------------------------------|
| PORT                          | 8080                                  |
| DATABASE_URL                  | jdbc:postgresql://localhost:5432/whatsmax |
| DATABASE_USER                 | postgres                              |
| DATABASE_PASSWORD             | postgres                              |
| FIREBASE_SERVICE_ACCOUNT_PATH | ./firebase-service-account.json       |

---

## Технологический стек

### Backend
- **Ktor 2.3.7** — HTTP + WebSocket сервер
- **Exposed 0.45** — ORM для PostgreSQL
- **PostgreSQL 42.7** — база данных
- **HikariCP** — connection pool
- **Firebase Admin SDK 9.2** — верификация токенов
- **kotlinx.serialization** — JSON

### Android
- **Jetpack Compose BOM 2024.02** — UI
- **Hilt 2.50** — DI
- **Firebase Auth** — аутентификация
- **Retrofit 2.9 + OkHttp 4.12** — REST API
- **kotlinx.serialization** — JSON
- **Room 2.6** — локальный кэш
- **Coil 2.5** — загрузка изображений
- **Stream WebRTC** — звонки
- **Coroutines + Flow** — асинхронность
- **Navigation Compose** — навигация

---

## Структура Clean Architecture (Android)

```
┌─────────────────────────────────────────┐
│  Presentation (UI)                      │
│  ViewModel + Compose Screens            │
├─────────────────────────────────────────┤
│  Domain (бизнес-логика)                 │
│  Use Cases + Repository Interfaces      │
│  + Domain Models                        │
├─────────────────────────────────────────┤
│  Data (реализации)                      │
│  Repository Impl + API + WebSocket + DB │
└─────────────────────────────────────────┘
```

**Правила зависимостей:**
- `presentation` зависит от `domain`
- `data` зависит от `domain`
- `domain` НЕ зависит ни от чего

---

## Частые проблемы

**Q: `Connection refused` при запуске Android на эмуляторе**
A: Используйте `10.0.2.2` вместо `localhost` в `BASE_URL`

**Q: `401 Unauthorized` при всех запросах**
A: Проверьте, что `firebase-service-account.json` лежит в папке `backend/`

**Q: `Database connection failed`**
A: Убедитесь, что PostgreSQL запущен: `pg_ctl status` или проверьте службы Windows

**Q: Ошибка компиляции `kapt`**
A: Выполните `./gradlew clean` и пересоберите проект

**Q: Google Sign-In не работает**
A: Добавьте SHA-1 отпечаток вашего debug keystore в Firebase Console:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

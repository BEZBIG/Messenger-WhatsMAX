/** Тестовая инфраструктура: testModule без БД/Firebase + fake auth + моки. */
package com.whatsmax

import com.whatsmax.domain.repositories.*
import com.whatsmax.plugins.FirebasePrincipal
import com.whatsmax.plugins.configureStatusPages
import com.whatsmax.routes.*
import com.whatsmax.utils.StorageService
import com.whatsmax.websocket.WebSocketManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.time.Duration.Companion.minutes

/**
 * Фейковый формат токена: только uid (alphanumeric + underscore).
 * Bearer-парсер Ktor требует, чтобы значение было token68 — без пробелов, спецсимволов вроде ":" и "@".
 * Поэтому имя/email в принципала подставляются константами, а сам токен — это просто uid.
 */
fun fakeToken(uid: String): String = uid

fun HttpRequestBuilder.bearer(token: String) = bearerAuth(token)

/** JSON-кодек для клиента в тестах — повторяет серверную SnakeCase-стратегию. */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val testJson: Json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/**
 * Контейнер с моками для использования в каждом тесте.
 */
class TestMocks(
    val users: UserRepository       = mockk(relaxed = false),
    val chats: ChatRepository       = mockk(relaxed = false),
    val messages: MessageRepository = mockk(relaxed = false),
    val channels: ChannelRepository = mockk(relaxed = false),
    val files: FileRepository       = mockk(relaxed = true),
    val reactions: ReactionRepository = mockk(relaxed = true),
    val ws: WebSocketManager        = mockk(relaxed = true),
    val storage: StorageService     = mockk(relaxed = true)
)

/**
 * Тестовый модуль приложения: подключает только нужные плагины и регистрирует маршруты
 * с замоканными репозиториями. БД и Firebase Admin SDK не задействуются.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
fun Application.testModule(mocks: TestMocks) {

    install(DefaultHeaders) { header("X-App-Name", "WhatsMAX-Backend-Test") }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        })
    }

    configureStatusPages()

    install(Authentication) {
        bearer("firebase") {
            authenticate { credential ->
                val uid = credential.token
                if (uid.isNotBlank()) {
                    FirebasePrincipal(uid = uid, name = "TestUser", email = "test@example.com")
                } else null
            }
        }
    }

    install(RateLimit) {
        val ids = listOf("messages", "reactions", "uploads", "auth")
        ids.forEach { name ->
            register(RateLimitName(name)) {
                rateLimiter(limit = 100_000, refillPeriod = 1.minutes)
                requestKey { call ->
                    call.principal<FirebasePrincipal>()?.uid
                        ?: call.request.local.remoteHost
                }
            }
        }
    }

    coEvery { mocks.ws.sendToUsers(any(), any()) } returns Unit

    routing {
        authRoutes(mocks.users)
        userRoutes(mocks.users)
        chatRoutes(mocks.chats, mocks.users)
        messageRoutes(mocks.messages, mocks.chats, mocks.files, mocks.storage, mocks.ws, "/tmp")
    }
}

/** Хелпер: настройка клиента в testApplication для работы с JSON в SnakeCase. */
fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
    install(ClientContentNegotiation) {
        json(testJson)
    }
}

/**
 * Обёртка над testApplication, которая:
 *  - перебивает environment на пустой MapApplicationConfig (иначе Ktor сам подхватит
 *    основной module() из application.conf и попробует подключиться к PostgreSQL);
 *  - регистрирует testModule(mocks);
 *  - даёт блоку доступ к mocks для настройки coEvery {...}.
 */
fun runTestApp(block: suspend ApplicationTestBuilder.(TestMocks) -> Unit) = testApplication {
    environment { config = MapApplicationConfig() }
    val mocks = TestMocks()
    application { testModule(mocks) }
    block(mocks)
}

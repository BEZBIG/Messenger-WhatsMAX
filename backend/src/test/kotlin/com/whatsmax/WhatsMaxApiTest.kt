/** Интеграционные HTTP-тесты ключевых API мессенджера WhatsMAX. */
package com.whatsmax

import com.whatsmax.domain.models.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhatsMaxApiTest {

    // ─── Тестовые данные ──────────────────────────────────────────────────
    private val aliceUid = "uid_alice"
    private val bobUid   = "uid_bob"
    private val now      = "2026-05-29T10:00:00"

    private fun makeUser(uid: String, username: String = "user_$uid") = User(
        uid = uid, username = username, email = "$username@x.com",
        displayName = username, lastSeen = now, createdAt = now
    )

    private fun makeChat(
        id: UUID = UUID.randomUUID(), type: String = "group",
        createdBy: String = aliceUid, name: String? = "Test chat"
    ) = Chat(
        id = id.toString(), type = type, name = name, createdBy = createdBy,
        createdAt = now, updatedAt = now
    )

    private fun makeMessage(
        chatId: UUID, senderId: String, content: String = "hello",
        id: UUID = UUID.randomUUID()
    ) = Message(
        id = id.toString(), chatId = chatId.toString(), senderId = senderId,
        senderName = "User", content = content, createdAt = now
    )

    // ─── 1. registerUserSuccess ───────────────────────────────────────────
    @Test
    fun `registerUserSuccess - 201 при создании нового профиля`() = runTestApp { mocks ->
        val client = jsonClient()
        coEvery { mocks.users.getUserByUid(aliceUid) } returns null
        coEvery { mocks.users.createUser(aliceUid, any()) } returns makeUser(aliceUid, "alice")

        val response = client.post("/auth/register") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = "alice", displayName = "Alice"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val user: User = response.body()
        assertEquals("alice", user.username)
        coVerify(exactly = 1) { mocks.users.createUser(aliceUid, any()) }
    }

    // ─── 2. registerExistingUserReturns200 ────────────────────────────────
    @Test
    fun `registerExistingUserReturns200 - повторная регистрация возвращает существующего`() = runTestApp { mocks ->
        val client = jsonClient()
        val existing = makeUser(aliceUid, "alice")
        coEvery { mocks.users.getUserByUid(aliceUid) } returns existing

        val response = client.post("/auth/register") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = "alice", displayName = "Alice"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify(exactly = 0) { mocks.users.createUser(any(), any()) }
    }

    // ─── 3. registerInvalidUsernameFails ──────────────────────────────────
    @Test
    fun `registerInvalidUsernameFails - 400 для короткого username`() = runTestApp { mocks ->
        val client = jsonClient()

        val response = client.post("/auth/register") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = "ab", displayName = "Alice"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Username"))
    }

    // ─── 4. getMeReturnsUser ──────────────────────────────────────────────
    @Test
    fun `getMeReturnsUser - 200 для зарегистрированного пользователя`() = runTestApp { mocks ->
        val client = jsonClient()
        coEvery { mocks.users.getUserByUid(aliceUid) } returns makeUser(aliceUid, "alice")

        val response = client.get("/auth/me") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.OK, response.status)
        val user: User = response.body()
        assertEquals("alice", user.username)
    }

    // ─── 5. getMeForUnknownUserReturns404 ─────────────────────────────────
    @Test
    fun `getMeForUnknownUserReturns404 - 404 если профиль не создан`() = runTestApp { mocks ->
        val client = jsonClient()
        coEvery { mocks.users.getUserByUid(aliceUid) } returns null

        val response = client.get("/auth/me") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ─── 6. createDirectChatSuccess ───────────────────────────────────────
    @Test
    fun `createDirectChatSuccess - 201 и автоматическое добавление инициатора`() = runTestApp { mocks ->
        val client = jsonClient()
        val newChat = makeChat(type = "direct", name = null)
        coEvery { mocks.chats.findDirectChat(aliceUid, bobUid) } returns null
        val captured = slot<CreateChatRequest>()
        coEvery { mocks.chats.createChat(capture(captured), aliceUid) } returns newChat

        val response = client.post("/chats") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateChatRequest(type = "direct", memberUids = listOf(bobUid)))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(captured.captured.memberUids.containsAll(listOf(aliceUid, bobUid)))
    }

    // ─── 7. createDirectChatReturnsExisting ───────────────────────────────
    @Test
    fun `createDirectChatReturnsExisting - 200 если direct-чат уже есть`() = runTestApp { mocks ->
        val client = jsonClient()
        val existing = makeChat(type = "direct", name = null)
        coEvery { mocks.chats.findDirectChat(aliceUid, bobUid) } returns existing

        val response = client.post("/chats") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateChatRequest(type = "direct", memberUids = listOf(bobUid)))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify(exactly = 0) { mocks.chats.createChat(any(), any()) }
    }

    // ─── 8. createGroupChatSuccess ────────────────────────────────────────
    @Test
    fun `createGroupChatSuccess - 201 для группового чата`() = runTestApp { mocks ->
        val client = jsonClient()
        val groupChat = makeChat(type = "group", name = "Squad")
        coEvery { mocks.chats.createChat(any(), aliceUid) } returns groupChat

        val response = client.post("/chats") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(CreateChatRequest(type = "group", memberUids = listOf(bobUid), name = "Squad"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val chat: Chat = response.body()
        assertEquals("group", chat.type)
    }

    // ─── 9. getChatsReturnsList ───────────────────────────────────────────
    @Test
    fun `getChatsReturnsList - 200 со списком чатов пользователя`() = runTestApp { mocks ->
        val client = jsonClient()
        val chats = listOf(makeChat(name = "Family"), makeChat(name = "Work"))
        coEvery { mocks.chats.getUserChats(aliceUid) } returns chats

        val response = client.get("/chats") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.OK, response.status)
        val result: List<Chat> = response.body()
        assertEquals(2, result.size)
    }

    // ─── 10. getChatByIdForNonMemberFails403 ──────────────────────────────
    @Test
    fun `getChatByIdForNonMemberFails403 - 403 при доступе к чужому чату`() = runTestApp { mocks ->
        val client = jsonClient()
        val foreignChatId = UUID.randomUUID()
        coEvery { mocks.chats.isUserInChat(foreignChatId, aliceUid) } returns false

        val response = client.get("/chats/$foreignChatId") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    // ─── 11. sendMessageSuccess ───────────────────────────────────────────
    @Test
    fun `sendMessageSuccess - 201 и рассылка через WebSocket`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        val msg = makeMessage(chatId, aliceUid, "Hi Bob!")
        coEvery { mocks.chats.isUserInChat(chatId, aliceUid) } returns true
        coEvery { mocks.messages.sendMessage(chatId, aliceUid, any()) } returns msg
        coEvery { mocks.chats.getChatMembers(chatId) } returns listOf(
            ChatMember(aliceUid, "Alice", role = "admin", joinedAt = now),
            ChatMember(bobUid,   "Bob",   role = "member", joinedAt = now)
        )

        val response = client.post("/chats/$chatId/messages") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(content = "Hi Bob!"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val saved: Message = response.body()
        assertEquals("Hi Bob!", saved.content)
        coVerify(exactly = 1) { mocks.ws.sendToUsers(match { it.containsAll(listOf(aliceUid, bobUid)) }, any()) }
    }

    // ─── 12. sendMessageInForeignChatFails403 ─────────────────────────────
    @Test
    fun `sendMessageInForeignChatFails403 - 403 при отправке в чужой чат`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        coEvery { mocks.chats.isUserInChat(chatId, aliceUid) } returns false

        val response = client.post("/chats/$chatId/messages") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(content = "spam"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 0) { mocks.messages.sendMessage(any(), any(), any()) }
    }

    // ─── 13. getMessagesReturnsList ───────────────────────────────────────
    @Test
    fun `getMessagesReturnsList - 200 с историей сообщений`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        val msgs = listOf(makeMessage(chatId, aliceUid, "1"), makeMessage(chatId, bobUid, "2"))
        coEvery { mocks.chats.isUserInChat(chatId, aliceUid) } returns true
        coEvery { mocks.messages.getChatMessages(chatId, 50, null) } returns msgs

        val response = client.get("/chats/$chatId/messages") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.OK, response.status)
        val result: List<Message> = response.body()
        assertEquals(2, result.size)
    }

    // ─── 14. editOwnMessageSuccess ────────────────────────────────────────
    @Test
    fun `editOwnMessageSuccess - 200 при правке собственного сообщения`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        val msgId  = UUID.randomUUID()
        val original = makeMessage(chatId, aliceUid, "old", id = msgId)
        val updated  = original.copy(content = "new", isEdited = true)
        coEvery { mocks.messages.getMessageById(msgId) } returns original
        coEvery { mocks.messages.editMessage(msgId, "new") } returns updated
        coEvery { mocks.chats.getChatMembers(chatId) } returns emptyList()

        val response = client.put("/chats/$chatId/messages/$msgId") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(EditMessageRequest(content = "new"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val msg: Message = response.body()
        assertEquals("new", msg.content)
        assertTrue(msg.isEdited)
    }

    // ─── 15. editForeignMessageFails403 ───────────────────────────────────
    @Test
    fun `editForeignMessageFails403 - 403 при правке чужого сообщения`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        val msgId  = UUID.randomUUID()
        val foreign = makeMessage(chatId, bobUid, "bob's msg", id = msgId)
        coEvery { mocks.messages.getMessageById(msgId) } returns foreign

        val response = client.put("/chats/$chatId/messages/$msgId") {
            bearer(fakeToken(aliceUid))
            contentType(ContentType.Application.Json)
            setBody(EditMessageRequest(content = "hijacked"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 0) { mocks.messages.editMessage(any(), any()) }
    }

    // ─── 16. deleteOwnMessageSuccess ──────────────────────────────────────
    @Test
    fun `deleteOwnMessageSuccess - 204 при удалении своего сообщения`() = runTestApp { mocks ->
        val client = jsonClient()
        val chatId = UUID.randomUUID()
        val msgId  = UUID.randomUUID()
        val msg = makeMessage(chatId, aliceUid, id = msgId)
        coEvery { mocks.messages.getMessageById(msgId) } returns msg
        coEvery { mocks.messages.deleteMessage(msgId) } returns Unit
        coEvery { mocks.chats.getChatMembers(chatId) } returns emptyList()

        val response = client.delete("/chats/$chatId/messages/$msgId") {
            bearer(fakeToken(aliceUid))
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        coVerify(exactly = 1) { mocks.messages.deleteMessage(msgId) }
    }

    // ─── 17. unauthorisedAccessFails401 ───────────────────────────────────
    @Test
    fun `unauthorisedAccessFails401 - 401 без токена авторизации`() = runTestApp { _ ->
        val client = jsonClient()
        val r1 = client.get("/auth/me")
        val r2 = client.get("/chats")
        val r3 = client.post("/chats") {
            contentType(ContentType.Application.Json)
            setBody(CreateChatRequest(type = "group", memberUids = listOf(bobUid), name = "X"))
        }
        val r4 = client.post("/chats/${UUID.randomUUID()}/messages") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(content = "hi"))
        }

        assertEquals(HttpStatusCode.Unauthorized, r1.status)
        assertEquals(HttpStatusCode.Unauthorized, r2.status)
        assertEquals(HttpStatusCode.Unauthorized, r3.status)
        assertEquals(HttpStatusCode.Unauthorized, r4.status)
    }

    // ─── 18. searchUsersByQuerySuccess ────────────────────────────────────
    @Test
    fun `searchUsersByQuerySuccess - 200 с результатами поиска`() = runTestApp { mocks ->
        val client = jsonClient()
        coEvery { mocks.users.searchUsers("ali", 20) } returns listOf(
            makeUser(aliceUid, "alice"),
            makeUser("uid2", "alicia")
        )

        val response = client.get("/users/search?q=ali") { bearer(fakeToken(aliceUid)) }

        assertEquals(HttpStatusCode.OK, response.status)
        val users: List<User> = response.body()
        assertEquals(2, users.size)
        assertTrue(users.all { it.username.startsWith("ali") })
    }

    // ─── 19. searchUsersWithoutQueryFails400 ──────────────────────────────
    @Test
    fun `searchUsersWithoutQueryFails400 - 400 если не передан параметр q`() = runTestApp { _ ->
        val client = jsonClient()
        val response = client.get("/users/search") { bearer(fakeToken(aliceUid)) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ─── 20. unknownRouteReturns404 ───────────────────────────────────────
    @Test
    fun `unknownRouteReturns404 - 404 для несуществующего маршрута`() = runTestApp { _ ->
        val client = jsonClient()
        val response = client.get("/nonexistent")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

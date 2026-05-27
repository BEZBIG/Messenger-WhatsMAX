/** Базовые HTTP-тесты бэкенда. */
package com.whatsmax

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `test protected endpoint returns 401 without token`() = testApplication {
        application { module() }
        val response = client.get("/auth/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test unknown route returns 404`() = testApplication {
        application { module() }
        val response = client.get("/nonexistent")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

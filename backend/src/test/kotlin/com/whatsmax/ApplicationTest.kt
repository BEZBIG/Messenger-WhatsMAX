/** Базовые smoke-тесты бэкенда (используют testModule без БД/Firebase). */
package com.whatsmax

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun `protected endpoint returns 401 without token`() = runTestApp { _ ->
        val response = client.get("/auth/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `unknown route returns 404`() = runTestApp { _ ->
        val response = client.get("/nonexistent")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}

package com.vlatkogalev.app.api

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `health endpoint returns OK`() = testApplication {
        application {
            module()
        }

        assertEquals(HttpStatusCode.OK, client.get("/health").status)
    }
}

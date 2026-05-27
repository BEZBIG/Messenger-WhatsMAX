/** Аутентификация через Firebase ID Token с кешированием. */
package com.whatsmax.plugins

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import io.ktor.server.application.*
import io.ktor.server.auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val authLogger = LoggerFactory.getLogger("Authentication")

/** Principal с данными из Firebase ID Token. */
data class FirebasePrincipal(
    val uid: String,
    val email: String?,
    val name: String?
) : Principal

private data class CachedPrincipal(val principal: FirebasePrincipal, val expiresAtMs: Long)

private const val CACHE_TTL_MS = 5 * 60 * 1000L

private val tokenCache = ConcurrentHashMap<String, CachedPrincipal>()

fun Application.configureAuthentication() {
    install(Authentication) {
        bearer("firebase") {
            authenticate { tokenCredential ->
                val token = tokenCredential.token
                val now = System.currentTimeMillis()

                tokenCache[token]?.let { cached ->
                    if (cached.expiresAtMs > now) return@authenticate cached.principal
                    tokenCache.remove(token)
                }

                try {
                    val verified: FirebaseToken = withContext(Dispatchers.IO) {
                        FirebaseAuth.getInstance().verifyIdToken(token, true)
                    }
                    val principal = FirebasePrincipal(
                        uid   = verified.uid,
                        email = verified.email,
                        name  = verified.name
                    )
                    val expSec = (verified.claims["exp"] as? Number)?.toLong() ?: 0L
                    val ttl = minOf(expSec * 1000L - now, CACHE_TTL_MS).coerceAtLeast(0L)
                    if (ttl > 0) tokenCache[token] = CachedPrincipal(principal, now + ttl)
                    principal
                } catch (e: Exception) {
                    authLogger.warn("Firebase token verification failed: ${e.message}")
                    null
                }
            }
        }
    }
}

/** Сбрасывает кеш для конкретного токена. */
fun invalidateAuthCache(token: String) {
    tokenCache.remove(token)
}

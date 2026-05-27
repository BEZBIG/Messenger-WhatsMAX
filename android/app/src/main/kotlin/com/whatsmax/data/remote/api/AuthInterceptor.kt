/** OkHttp Interceptor: Firebase ID Token в Authorization: Bearer. */
package com.whatsmax.data.remote.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            try {
                FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
            } catch (e: Exception) { null }
        }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            val freshToken = runBlocking {
                try {
                    FirebaseAuth.getInstance().currentUser
                        ?.getIdToken(true)  // forceRefresh = true
                        ?.await()
                        ?.token
                } catch (e: Exception) { null }
            }
            if (freshToken != null) {
                return chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $freshToken")
                        .build()
                )
            }
        }

        return response
    }
}

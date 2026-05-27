/** Инициализация Firebase Admin SDK по JSON-сервисному ключу. */
package com.whatsmax.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

object FirebaseAdmin {

    private val logger = LoggerFactory.getLogger(FirebaseAdmin::class.java)

    fun initialize(serviceAccountPath: String) {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val serviceAccountFile = File(serviceAccountPath)
        if (!serviceAccountFile.exists()) {
            logger.error(
                "Firebase service account file not found at: $serviceAccountPath\n" +
                "Download it from Firebase Console → Project Settings → Service accounts"
            )
            return
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(FileInputStream(serviceAccountFile)))
            .build()

        FirebaseApp.initializeApp(options)
        logger.info("Firebase Admin SDK initialized successfully")
    }
}

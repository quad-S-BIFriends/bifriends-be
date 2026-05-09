package com.bifriends.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.FileInputStream

@Configuration
class FirebaseConfig(
    @Value("\${firebase.config.path}") private val configPath: String
) {

    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isNotEmpty()) {
            return FirebaseApp.getInstance()
        }

        val inputStream = if (configPath.startsWith("classpath:")) {
            ClassPathResource(configPath.removePrefix("classpath:")).inputStream
        } else {
            FileInputStream(configPath)
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(inputStream))
            .build()

        return FirebaseApp.initializeApp(options)
    }
}
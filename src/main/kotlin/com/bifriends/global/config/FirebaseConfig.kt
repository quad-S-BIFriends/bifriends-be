package com.bifriends.global.config

import com.bifriends.infrastructure.firebase.FallbackImageProperties
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.FileInputStream

@Configuration
@EnableConfigurationProperties(FallbackImageProperties::class)
class FirebaseConfig(
    @Value("\${firebase.config.path}") private val configPath: String,
    @Value("\${firebase.storage.bucket}") private val storageBucket: String,
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
            // StorageClient.getInstance().bucket() 기본 버킷 설정
            // (버킷명을 명시적으로 전달하는 경우에도 초기화해두는 것이 안전)
            .setStorageBucket(storageBucket)
            .build()

        return FirebaseApp.initializeApp(options)
    }
}
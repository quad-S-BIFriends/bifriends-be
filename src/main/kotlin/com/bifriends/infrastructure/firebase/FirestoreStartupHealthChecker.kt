package com.bifriends.infrastructure.firebase

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 앱 기동 직후 Firestore 연결을 1회 점검한다.
 * 실패 시 warn 로그만 남기고 기동은 계속한다.
 */
@Component
class FirestoreStartupHealthChecker(
    private val firestoreService: FirestoreService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        firestoreService.verifyConnectivity()
    }
}

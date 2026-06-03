package com.bifriends.infrastructure.firebase

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Firestore 데이터 접근 서비스
 *
 * 감정 학습 세션(mindSessions)을 Firestore에 저장·조회한다.
 *
 * Firestore 경로: users/{memberId}/mindSessions/{setId}
 */
@Service
class FirestoreService(
    @Value("\${firebase.firestore.database-id:(default)}") private val databaseId: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val db: Firestore by lazy {
        val app = FirebaseApp.getInstance()
        if (databaseId == "(default)") {
            FirestoreClient.getFirestore(app)
        } else {
            log.info("[Firestore] named database 사용 — databaseId={}", databaseId)
            FirestoreClient.getFirestore(app, databaseId)
        }
    }

    /**
     * 기동 시 Firestore 연결 가능 여부를 점검한다. 실패해도 앱 기동은 계속된다.
     */
    fun verifyConnectivity() {
        try {
            db.collection("_connectivity").document("probe").get().get()
            log.info("[Firestore] connectivity check OK — databaseId={}", databaseId)
        } catch (e: Throwable) {
            log.warn("[Firestore] connectivity check failed — Firestore 미연동 가능: {}", e.message)
        }
    }

    fun saveMindSession(
        memberId: Long,
        setId: String,
        sessionData: Map<String, Any>,
    ): DocumentReference = runFirestoreWrite("mindSession 저장") {
        val docRef = mindSessionsCollection(memberId).document(setId)
        docRef.set(sessionData).get()
        log.info("[Firestore] mindSession 저장 완료 — memberId={}, setId={}", memberId, setId)
        docRef
    }

    /**
     * 시나리오 중복 방지용. 실패 시 빈 목록(학습 생성은 계속 가능).
     */
    fun getLearnedExpressions(memberId: Long): List<String> {
        return try {
            mindSessionsCollection(memberId)
                .get()
                .get()
                .documents
                .mapNotNull { it.getString("learnedExpression") }
                .distinct()
        } catch (e: Exception) {
            log.warn("[Firestore] learnedExpressions 조회 실패 — memberId={}: {}", memberId, e.message)
            emptyList()
        }
    }

    /**
     * @return 세션 데이터. 문서가 없으면 null. 인프라 오류는 [FirestoreOperationException].
     */
    fun getMindSession(memberId: Long, setId: String): Map<String, Any>? =
        runFirestoreRead("mindSession 조회") {
            val doc = mindSessionsCollection(memberId).document(setId).get().get()
            if (doc.exists()) doc.data else null
        }

    /**
     * 완료 세션 목록(최신순). 인프라 오류는 [FirestoreOperationException].
     */
    fun getMindSessionList(memberId: Long, limit: Int = 20): List<Map<String, Any>> =
        runFirestoreRead("mindSession 목록 조회") {
            try {
                queryOrderedByCompletedAt(memberId, limit)
            } catch (e: Exception) {
                if (isIndexOrQueryError(e)) {
                    log.warn(
                        "[Firestore] completedAt index missing, using in-memory sort — memberId={}: {}",
                        memberId,
                        e.message,
                    )
                    querySortedInMemory(memberId, limit)
                } else {
                    throw e
                }
            }
        }

    private fun mindSessionsCollection(memberId: Long) =
        db.collection("users").document(memberId.toString()).collection("mindSessions")

    private fun queryOrderedByCompletedAt(memberId: Long, limit: Int): List<Map<String, Any>> =
        mindSessionsCollection(memberId)
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .get()
            .documents
            .mapNotNull { it.data }

    private fun querySortedInMemory(memberId: Long, limit: Int): List<Map<String, Any>> {
        val maxFetch = maxOf(limit * 5, 100)
        val docs = mindSessionsCollection(memberId)
            .limit(maxFetch)
            .get()
            .get()
            .documents
            .mapNotNull { it.data }

        return docs
            .sortedByDescending { it["completedAt"] as? String ?: "" }
            .take(limit)
    }

    private fun isIndexOrQueryError(e: Exception): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            if (message.contains("failed_precondition") ||
                message.contains("requires an index") ||
                message.contains("index")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun <T> runFirestoreRead(operation: String, block: () -> T): T =
        try {
            block()
        } catch (e: FirestoreOperationException) {
            throw e
        } catch (e: Exception) {
            log.error("[Firestore] {} 실패: {}", operation, e.message)
            throw FirestoreOperationException("Firestore $operation 실패", e)
        }

    private fun <T> runFirestoreWrite(operation: String, block: () -> T): T =
        runFirestoreRead(operation, block)
}

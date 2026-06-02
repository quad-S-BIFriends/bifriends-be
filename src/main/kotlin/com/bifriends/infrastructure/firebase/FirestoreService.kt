package com.bifriends.infrastructure.firebase

import com.google.cloud.firestore.DocumentReference
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Firestore 데이터 접근 서비스
 *
 * 감정 학습 세션(mindSessions)을 Firestore에 저장·조회한다.
 *
 * Firestore 경로: users/{memberId}/mindSessions/{setId}
 */
@Service
class FirestoreService {
    private val log = LoggerFactory.getLogger(javaClass)

    private val db get() = FirestoreClient.getFirestore()

    /**
     * 감정 학습 세션을 저장한다.
     *
     * @param memberId   회원 ID
     * @param setId      세트 고유 ID (AI가 생성)
     * @param sessionData Firestore에 저장할 Map (감정, 상황, 배운 표현, steps 전체, 완료 시각)
     * @return 저장된 문서 참조
     */
    fun saveMindSession(
        memberId: Long,
        setId: String,
        sessionData: Map<String, Any>,
    ): DocumentReference {
        val docRef = db.collection("users")
            .document(memberId.toString())
            .collection("mindSessions")
            .document(setId)

        docRef.set(sessionData).get()  // 동기 완료 대기

        log.info("[Firestore] mindSession 저장 완료 — memberId={}, setId={}", memberId, setId)
        return docRef
    }

    /**
     * 특정 회원의 모든 mindSessions에서 배운 표현 목록을 조회한다.
     * AI가 중복 표현을 생성하지 않도록 사용한다.
     *
     * @return 이미 학습한 표현 문자열 목록 (중복 없음)
     */
    fun getLearnedExpressions(memberId: Long): List<String> {
        return try {
            val snapshots = db.collection("users")
                .document(memberId.toString())
                .collection("mindSessions")
                .get()
                .get()

            snapshots.documents
                .mapNotNull { it.getString("learnedExpression") }
                .distinct()
        } catch (e: Exception) {
            log.warn("[Firestore] learnedExpressions 조회 실패 — memberId={}: {}", memberId, e.message)
            emptyList()
        }
    }

    /**
     * 저장된 특정 세션을 조회한다 (히스토리 재열람용).
     *
     * @return 세션 데이터 Map, 존재하지 않으면 null
     */
    fun getMindSession(memberId: Long, setId: String): Map<String, Any>? {
        return try {
            val doc = db.collection("users")
                .document(memberId.toString())
                .collection("mindSessions")
                .document(setId)
                .get()
                .get()

            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            log.warn("[Firestore] mindSession 조회 실패 — memberId={}, setId={}: {}", memberId, setId, e.message)
            null
        }
    }

    /**
     * 회원의 mindSessions 목록을 최신순으로 조회한다 (히스토리 화면).
     *
     * @param limit 최대 조회 수 (기본 20)
     */
    fun getMindSessionList(memberId: Long, limit: Int = 20): List<Map<String, Any>> {
        return try {
            db.collection("users")
                .document(memberId.toString())
                .collection("mindSessions")
                .orderBy("completedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get()
                .documents
                .mapNotNull { it.data }
        } catch (e: Exception) {
            log.warn("[Firestore] mindSession 목록 조회 실패 — memberId={}: {}", memberId, e.message)
            emptyList()
        }
    }
}

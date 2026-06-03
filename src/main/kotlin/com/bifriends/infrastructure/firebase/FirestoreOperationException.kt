package com.bifriends.infrastructure.firebase

/**
 * Firestore 인프라 오류(인증, 네트워크, 권한, 인덱스 미설정 등)를 나타낸다.
 * 문서 미존재와 구분하기 위해 별도 예외로 전파한다.
 */
class FirestoreOperationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

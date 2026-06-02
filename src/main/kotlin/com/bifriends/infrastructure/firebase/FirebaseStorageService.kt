package com.bifriends.infrastructure.firebase

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.firebase.cloud.StorageClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

/**
 * Firebase Storage 업로드 서비스
 *
 * AI가 반환한 step3 만화 컷 이미지(Base64)를 Firebase Storage에 업로드하고
 * Firebase Storage 다운로드 URL을 반환한다.
 *
 * ── Firebase Storage 다운로드 토큰 방식 ──────────────────────────────────
 * Firebase Storage URL 형식:
 *   https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{encoded_path}?alt=media&token={uuid}
 *
 * `token`은 OAuth 토큰이 아닌 파일에 영구 부여되는 UUID다.
 * 이 URL을 가진 누구나(FE 포함) 별도 인증 헤더 없이 파일에 접근할 수 있다.
 *
 * Firebase Admin SDK로 업로드할 때는 `firebaseStorageDownloadTokens` 메타데이터에
 * UUID를 직접 설정해야 이 URL 패턴을 사용할 수 있다.
 * (Firebase 콘솔/클라이언트 SDK 업로드 시에는 자동으로 생성됨)
 *
 * ── 사용 예 ──────────────────────────────────────────────────────────────
 * val url = firebaseStorageService.uploadBase64Image(
 *     base64 = "iVBORw0KGgo...",
 *     contentType = "image/png",
 *     folder = "mindSessions/setId/comic"
 * )
 * // url → "https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app
 * //         /o/mindSessions%2FsetId%2Fcomic%2Fuuid.png?alt=media&token=some-uuid"
 * ────────────────────────────────────────────────────────────────────────
 */
@Service
class FirebaseStorageService(
    @Value("\${firebase.storage.bucket}") private val bucketName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Base64 인코딩된 이미지를 Firebase Storage에 업로드하고 다운로드 URL을 반환한다.
     *
     * 반환된 URL은 `?alt=media&token={uuid}` 형식으로,
     * FE에서 인증 없이 이미지를 표시할 수 있다.
     *
     * @param base64      Base64 인코딩 이미지 데이터 (data URL prefix 자동 제거)
     * @param contentType MIME 타입 (예: "image/png", "image/jpeg")
     * @param folder      저장 경로 접두사 (예: "mindSessions/setId/comic")
     * @return Firebase Storage 다운로드 URL (token 포함)
     */
    fun uploadBase64Image(
        base64: String,
        contentType: String = "image/png",
        folder: String = "emotion/comic",
    ): String {
        // data URL prefix 제거 (예: "data:image/png;base64,")
        val cleanBase64 = base64.substringAfter(",", base64)
        val bytes = Base64.getDecoder().decode(cleanBase64)

        val extension = contentType.substringAfter("/", "png")
        val fileName = "$folder/${UUID.randomUUID()}.$extension"

        // Firebase Storage 다운로드 토큰 (영구 UUID)
        val downloadToken = UUID.randomUUID().toString()

        // BlobInfo에 firebaseStorageDownloadTokens 메타데이터 설정
        // → Firebase Storage URL의 ?token= 파라미터로 사용됨
        val blobId = BlobId.of(bucketName, fileName)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .setMetadata(mapOf("firebaseStorageDownloadTokens" to downloadToken))
            .build()

        // Firebase Admin SDK의 StorageClient → 내부 GCS Storage 클라이언트로 업로드
        val storage = StorageClient.getInstance().bucket(bucketName).storage
        storage.create(blobInfo, bytes)

        // 파일 경로의 슬래시를 %2F로 인코딩 (Firebase Storage URL 규칙)
        val encodedPath = fileName.replace("/", "%2F")
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedPath?alt=media&token=$downloadToken"

        log.info("[FirebaseStorage] 업로드 완료 — path={}, token={}", fileName, downloadToken)
        return url
    }
}

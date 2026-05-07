package com.bifriends.domain.home.controller

import com.bifriends.domain.home.dto.HomeResponse
import com.bifriends.domain.home.service.HomeService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/home")
class HomeController(
    private val homeService: HomeService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * 홈 화면 단일 조회 API
     *
     * 한 번의 요청으로 홈 화면 렌더링에 필요한 모든 데이터를 반환한다.
     * - 출석 체크 (streak 갱신 + 보상 지급) 포함 — 멱등 처리됨
     * - 인사 메시지 (first_login / comeback / streak 분기)
     * - 레벨 · 풀 · streak 통계
     * - 오늘의 할 일 목록
     *
     * 
 ① 앱 오픈 / 홈 탭 진입
   → GET /api/v1/home
     응답: 인사 메시지 + 레벨/풀/streak + 할 일 목록
     (이게 상단 영역 + 인사 영역 + 상태 영역 + 프로그레스 바 + 할 일 리스트 한 번에 커버)

② 할 일 클릭 (CHAT / LEARNING / EMOTION 타입)
   → 해당 탭으로 이동 (백엔드 API 없음, 클라이언트 라우팅)
   → 탭에서 완료 후 홈으로 복귀 시 다시 ① 실행

③ 할 일 직접 완료 처리 (완료 버튼 탭 등)
   → PATCH /api/v1/todos/{id}/complete
     응답: 완료된 할 일 + 획득 풀 + 전체 완료 보너스 여부 + 레벨업 여부
   → 클라이언트에서 응답 값으로 로컬 상태 갱신 (프로그레스 바, 풀 표시 등)
     * 
     * Authorization: Bearer {accessToken}
     */
    @GetMapping
    fun getHome(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<HomeResponse> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        return ResponseEntity.ok(homeService.getHome(memberId))
    }
}

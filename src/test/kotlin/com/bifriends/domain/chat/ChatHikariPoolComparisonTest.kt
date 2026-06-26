package com.bifriends.domain.chat

import com.bifriends.domain.chat.dto.ChatMessageRequest
import com.bifriends.domain.chat.service.ChatService
import com.bifriends.domain.chat.support.LegacyChatTransactionSimulator
import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.model.Interest
import com.bifriends.infrastructure.ai.AiChatClient
import com.bifriends.infrastructure.ai.dto.AiChatRequest
import com.bifriends.infrastructure.ai.dto.AiChatResponse
import com.bifriends.infrastructure.firebase.FirestoreService
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.math.ceil

/**
 * STAR(상황·과제·행동·결과)용 — 채팅 TX 분리 전/후 Hikari 커넥션 대기 비교.
 *
 * 실행:
 *   ./gradlew test --tests "com.bifriends.domain.chat.ChatHikariPoolComparisonTest" -i
 *
 * stdout에 [STAR] 블록이 출력된다. 이 수치를 문서/발표에 그대로 사용하면 된다.
 */
@SpringBootTest
@ActiveProfiles("chat-pool-test")
@Import(LegacyChatTransactionSimulator::class)
@DisplayName("채팅 동시 부하 — Hikari 커넥션 대기 비교 (STAR)")
class ChatHikariPoolComparisonTest {

    companion object {
        private const val POOL_SIZE = 5
        private const val CONCURRENCY = 12
        private const val AI_DELAY_MS = 2_000L
    }

    @Autowired lateinit var chatService: ChatService
    @Autowired lateinit var legacySimulator: LegacyChatTransactionSimulator
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var dataSource: DataSource
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @MockBean lateinit var aiChatClient: AiChatClient
    @MockBean lateinit var firestoreService: FirestoreService

    private lateinit var member: Member

    @BeforeEach
    fun setUp() {
        doNothing().whenever(firestoreService).verifyConnectivity()
        whenever(aiChatClient.sendChat(any())).thenAnswer {
            Thread.sleep(AI_DELAY_MS)
            AiChatResponse(reply = "테스트 응답")
        }

        member = memberRepository.save(
            Member(
                email = "chat-pool-test@bifriends.test",
                providerId = "chat-pool-test-${UUID.randomUUID()}",
                onboardingCompleted = true,
                nickname = "테스트",
                grade = 4,
            ),
        )
    }

    @Test
    fun compareLegacyVsFixed_hikariAwaitingAndDbProbeLatency() {
        val legacy = runScenario("LEGACY (TX 안에서 AI 대기)") { index ->
            val sessionKey = "legacy-$index-${UUID.randomUUID()}"
            legacySimulator.sendMessageHoldingConnection(
                memberId = member.id,
                sessionKey = sessionKey,
                message = "안녕 $index",
                aiDelayMs = AI_DELAY_MS,
                aiReply = "응답 $index",
            )
        }

        val fixed = runScenario("FIXED (TX 분리 + NOT_SUPPORTED)") { index ->
            chatService.sendMessage(
                member.id,
                ChatMessageRequest(
                    sessionId = "fixed-$index-${UUID.randomUUID()}",
                    message = "안녕 $index",
                    nickname = member.nickname!!,
                    grade = member.grade!!,
                    interests = listOf(Interest.SCIENCE),
                ),
            )
        }

        printStarReport(legacy, fixed)

        assertThat(fixed.maxThreadsAwaiting)
            .`as`("수정 후: 커넥션 대기 스레드가 레거시보다 적어야 함")
            .isLessThan(legacy.maxThreadsAwaiting)

        assertThat(fixed.probeMaxMs)
            .`as`("수정 후: 부하 중 DB 프로브 최대 지연이 더 짧아야 함")
            .isLessThan(legacy.probeMaxMs)
    }

    private fun runScenario(label: String, action: (Int) -> Unit): ScenarioResult {
        val pool = hikariPool()
        val maxAwaiting = AtomicInteger(0)
        val sampler = Executors.newSingleThreadExecutor()
        val samplerFuture = sampler.submit {
            while (!Thread.currentThread().isInterrupted) {
                maxAwaiting.updateAndGet { current -> maxOf(current, pool.threadsAwaitingConnection) }
                Thread.sleep(20)
            }
        }

        val probeLatenciesMs = mutableListOf<Long>()
        val probeExecutor = Executors.newSingleThreadExecutor()
        val probeFuture = probeExecutor.submit {
            while (!Thread.currentThread().isInterrupted) {
                probeLatenciesMs += probeDbOnceMs()
                Thread.sleep(50)
            }
        }

        val chatExecutor = Executors.newFixedThreadPool(CONCURRENCY)
        val startedAt = System.currentTimeMillis()
        val futures = (0 until CONCURRENCY).map { index ->
            chatExecutor.submit(Callable {
                action(index)
            })
        }

        var success = 0
        var failures = 0
        futures.forEach { future ->
            try {
                future.get(30, TimeUnit.SECONDS)
                success++
            } catch (_: Exception) {
                failures++
            }
        }
        val elapsedMs = System.currentTimeMillis() - startedAt

        samplerFuture.cancel(true)
        probeFuture.cancel(true)
        sampler.shutdownNow()
        probeExecutor.shutdownNow()
        chatExecutor.shutdownNow()

        val sortedProbes = probeLatenciesMs.sorted()
        val p95 = percentile(sortedProbes, 95.0)

        return ScenarioResult(
            label = label,
            concurrency = CONCURRENCY,
            poolSize = POOL_SIZE,
            aiDelayMs = AI_DELAY_MS,
            success = success,
            failures = failures,
            elapsedMs = elapsedMs,
            maxActiveConnections = pool.activeConnections.coerceAtLeast(0),
            maxThreadsAwaiting = maxAwaiting.get(),
            probeSampleCount = sortedProbes.size,
            probeP95Ms = p95,
            probeMaxMs = sortedProbes.maxOrNull() ?: 0,
        )
    }

    private fun hikariPool(): HikariPoolMXBean {
        val hikari = dataSource as HikariDataSource
        return hikari.hikariPoolMXBean
            ?: error("HikariPoolMXBean unavailable — hikari.register-mbeans=true 필요")
    }

    private fun probeDbOnceMs(): Long {
        val start = System.nanoTime()
        jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
        return (System.nanoTime() - start) / 1_000_000
    }

    private fun percentile(sorted: List<Long>, p: Double): Long {
        if (sorted.isEmpty()) return 0
        val index = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size) - 1
        return sorted[index]
    }

    private fun printStarReport(legacy: ScenarioResult, fixed: ScenarioResult) {
        val awaitingReduction = legacy.maxThreadsAwaiting - fixed.maxThreadsAwaiting
        val probeImprovementPct = if (legacy.probeMaxMs > 0) {
            ((legacy.probeMaxMs - fixed.probeMaxMs) * 100.0 / legacy.probeMaxMs)
        } else {
            0.0
        }

        println(
            """
            |
            |========== [STAR] 채팅 TX 분리 — Hikari 부하 비교 ==========
            |Situation : 동시 채팅 ${CONCURRENCY}건, AI 응답 ${AI_DELAY_MS}ms, Hikari pool=${POOL_SIZE}
            |Task      : AI HTTP 대기 중 DB 커넥션 점유 → 풀 고갈·다른 API 대기 감소 증명
            |Action    : sendMessage를 ① TX 분리(NOT_SUPPORTED) vs ② 레거시(단일 TX+sleep) 재현
            |
            |--- LEGACY (수정 전 패턴) ---
            |  성공/실패        : ${legacy.success}/${legacy.failures}
            |  총 소요(ms)       : ${legacy.elapsedMs}
            |  max threadsAwaiting : ${legacy.maxThreadsAwaiting}
            |  DB 프로브 P95(ms) : ${legacy.probeP95Ms} (max ${legacy.probeMaxMs}, n=${legacy.probeSampleCount})
            |
            |--- FIXED (현재 코드) ---
            |  성공/실패        : ${fixed.success}/${fixed.failures}
            |  총 소요(ms)       : ${fixed.elapsedMs}
            |  max threadsAwaiting : ${fixed.maxThreadsAwaiting}
            |  DB 프로브 P95(ms) : ${fixed.probeP95Ms} (max ${fixed.probeMaxMs}, n=${fixed.probeSampleCount})
            |
            |Result:
            |  threadsAwaiting 감소 : ${legacy.maxThreadsAwaiting} → ${fixed.maxThreadsAwaiting} (Δ ${awaitingReduction})
            |  DB 프로브 P95 개선   : ${"%.1f".format(probeImprovementPct)}% (max ${legacy.probeMaxMs}ms → ${fixed.probeMaxMs}ms)
            |============================================================
            |
            """.trimMargin(),
        )
    }

    private data class ScenarioResult(
        val label: String,
        val concurrency: Int,
        val poolSize: Int,
        val aiDelayMs: Long,
        val success: Int,
        val failures: Int,
        val elapsedMs: Long,
        val maxActiveConnections: Int,
        val maxThreadsAwaiting: Int,
        val probeSampleCount: Int,
        val probeP95Ms: Long,
        val probeMaxMs: Long,
    )
}

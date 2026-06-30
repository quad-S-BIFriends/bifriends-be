package com.bifriends.domain.home.service

import com.bifriends.domain.member.event.MemberRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDate
import java.time.ZoneId

@Component
class MemberRegistrationEventListener(
    private val todoService: TodoService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMemberRegistered(event: MemberRegisteredEvent) {
        try {
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            todoService.generateDailyTodos(event.member, today)
        } catch (e: Exception) {
            log.error("[MemberRegistration] 할 일 생성 실패 — memberId={}", event.member.id, e)
        }
    }
}

package com.bifriends.domain.home.service

import com.bifriends.domain.member.event.MemberRegisteredEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDate
import java.time.ZoneId

@Component
class MemberRegistrationEventListener(
    private val todoService: TodoService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMemberRegistered(event: MemberRegisteredEvent) {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        todoService.generateDailyTodos(event.member, today)
    }
}

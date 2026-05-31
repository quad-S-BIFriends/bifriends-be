package com.bifriends.domain.chat.repository

import com.bifriends.domain.chat.model.ChatSession
import org.springframework.data.jpa.repository.JpaRepository

interface ChatSessionRepository : JpaRepository<ChatSession, String>

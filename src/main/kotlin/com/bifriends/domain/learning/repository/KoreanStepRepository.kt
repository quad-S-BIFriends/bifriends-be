package com.bifriends.domain.learning.repository

import com.bifriends.domain.learning.model.KoreanStep
import org.springframework.data.jpa.repository.JpaRepository

interface KoreanStepRepository : JpaRepository<KoreanStep, Long> {

    fun findByGradeOrderByStepNumber(grade: Int): List<KoreanStep>

    fun findByGradeAndStepNumber(grade: Int, stepNumber: Int): KoreanStep?
}

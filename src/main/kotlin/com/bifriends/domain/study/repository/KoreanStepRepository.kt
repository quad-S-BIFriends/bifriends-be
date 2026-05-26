package com.bifriends.domain.study.repository

import com.bifriends.domain.study.model.KoreanStep
import org.springframework.data.jpa.repository.JpaRepository

interface KoreanStepRepository : JpaRepository<KoreanStep, Long> {

    fun findByGradeOrderByStepNumber(grade: Int): List<KoreanStep>

    fun findByGradeAndStepNumber(grade: Int, stepNumber: Int): KoreanStep?
}

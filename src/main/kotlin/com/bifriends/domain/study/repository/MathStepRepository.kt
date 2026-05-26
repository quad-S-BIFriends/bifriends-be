package com.bifriends.domain.study.repository

import com.bifriends.domain.study.model.MathStep
import org.springframework.data.jpa.repository.JpaRepository

interface MathStepRepository : JpaRepository<MathStep, Long> {

    fun findByGradeOrderByStepNumber(grade: Int): List<MathStep>

    fun findByGradeAndStepNumber(grade: Int, stepNumber: Int): MathStep?

    fun existsByGradeAndStepNumber(grade: Int, stepNumber: Int): Boolean
}

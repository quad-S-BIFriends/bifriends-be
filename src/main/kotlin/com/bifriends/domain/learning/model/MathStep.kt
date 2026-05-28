package com.bifriends.domain.learning.model

import com.bifriends.infrastructure.converter.JsonNodeConverter
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*

@Entity
@Table(
    name = "math_step",
    uniqueConstraints = [UniqueConstraint(name = "uq_math_step_grade_number", columnNames = ["grade", "step_number"])]
)
class MathStep(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val grade: Int,

    @Column(name = "step_number", nullable = false)
    val stepNumber: Int,

    @Column(name = "step_title", nullable = false)
    val stepTitle: String,

    @Column(nullable = false)
    val concept: String,

    @Convert(converter = JsonNodeConverter::class)
    @Column(name = "content_json", columnDefinition = "text", nullable = false)
    val contentJson: JsonNode,
)

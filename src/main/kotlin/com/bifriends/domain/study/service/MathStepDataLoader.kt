package com.bifriends.domain.study.service

import com.bifriends.domain.study.model.MathStep
import com.bifriends.domain.study.repository.MathStepRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MathStepDataLoader(
    private val mathStepRepository: MathStepRepository,
    private val objectMapper: ObjectMapper,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class SeedFile(val grade: Int, val stepNumber: Int, val path: String)

    private val seedFiles = listOf(
        SeedFile(3, 1, "db/math/grade3_step1.json"),
        SeedFile(3, 2, "db/math/grade3_step2.json"),
        SeedFile(3, 3, "db/math/grade3_step3.json"),
        SeedFile(4, 1, "db/math/grade4_step1.json"),
        SeedFile(4, 2, "db/math/grade4_step2.json"),
        SeedFile(4, 3, "db/math/grade4_step3.json"),
        SeedFile(5, 1, "db/math/grade5_step1.json"),
        SeedFile(5, 2, "db/math/grade5_step2.json"),
        SeedFile(5, 3, "db/math/grade5_step3.json"),
        SeedFile(6, 1, "db/math/grade6_step1.json"),
        SeedFile(6, 2, "db/math/grade6_step2.json"),
        SeedFile(6, 3, "db/math/grade6_step3.json"),
    )

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (mathStepRepository.count() > 0) {
            return
        }

        seedFiles.forEach { seed ->
            val content = loadJson(seed.path)
            mathStepRepository.save(
                MathStep(
                    grade = seed.grade,
                    stepNumber = seed.stepNumber,
                    stepTitle = content.get("step_title").asText(),
                    concept = content.get("concept").asText(),
                    contentJson = content,
                )
            )
        }

        log.info("Math step seed data loaded: {} steps", seedFiles.size)
    }

    private fun loadJson(path: String): JsonNode {
        val resource = ClassPathResource(path)
        require(resource.exists()) { "Math seed file not found: $path" }
        resource.inputStream.use { input ->
            return objectMapper.readTree(input)
        }
    }
}

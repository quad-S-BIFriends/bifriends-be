package com.bifriends.infrastructure.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class JsonNodeConverter : AttributeConverter<JsonNode, String> {

    private val mapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: JsonNode?): String =
        attribute?.let { mapper.writeValueAsString(it) } ?: "{}"

    override fun convertToEntityAttribute(dbData: String?): JsonNode =
        dbData?.let { mapper.readTree(it) } ?: mapper.createObjectNode()
}

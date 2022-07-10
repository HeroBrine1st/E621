package ru.herobrine1st.e621.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

val objectMapper: ObjectMapper by lazy {
    jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
    }.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
}
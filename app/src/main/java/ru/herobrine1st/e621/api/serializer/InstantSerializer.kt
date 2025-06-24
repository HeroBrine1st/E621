package ru.herobrine1st.e621.api.serializer

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.serializers.FormattedInstantSerializer
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object ISO8601Serializer :
    FormattedInstantSerializer("ISO8601Serializer", DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
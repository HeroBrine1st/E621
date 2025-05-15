package ru.herobrine1st.e621.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

abstract class NullAsEmptyObjectSerializer<T>(val serializer: KSerializer<T>) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: T?) {
        if (value == null) encoder.beginStructure(descriptor).endStructure(descriptor)
        else encoder.encodeSerializableValue(serializer, value)
    }

    override fun deserialize(decoder: Decoder): T? {
        decoder as JsonDecoder
        val jsonObject = decoder.decodeJsonElement().jsonObject
        return if (jsonObject.isEmpty()) null else decoder.json.decodeFromJsonElement(serializer, jsonObject)
    }
}
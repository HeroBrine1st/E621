package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import ru.herobrine1st.e621.api.FileType
import ru.herobrine1st.e621.api.Rating
import java.time.Instant
import java.time.OffsetDateTime

@JsonIgnoreProperties("preview", "flags")
data class Post(
    val id: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?,
    val file: File,
    // preview is not applicable
    val sample: Sample,
    val score: Score,
    val tags: Tags,
    val lockedTags: List<String> = emptyList(),
    @JsonProperty("change_seq")
    val changeSequence: Int,
    // val flags: Flags, // No class
    val rating: Rating,
    @JsonProperty("fav_count")
    val favoriteCount: Int,
    val sources: List<String>,
    val pools: List<Int>,
    val relationships: Relationships,
    val approverId: Int,
    val uploaderId: Int,
    val description: String,
    val commentCount: Int,
    @JsonProperty(required = false)
    val isFavorited: Boolean = false,
    @JsonProperty(required = false)
    val hasNotes: Boolean = false,
    val duration: Float = 0f
) {
    val files: List<NormalizedFile> = listOf(
        NormalizedFile(file),
        NormalizedFile(sample),
        *sample.alternates.map { NormalizedFile(it.key, it.value) }.toTypedArray()
    ).sortedBy { it.width }
}

data class PostReduced(
    val status: String,
    val flags: String,
    @JsonProperty("file_ext")
    val type: FileType,
    val id: Int,
    val createdAt: Instant,
    val rating: Rating,
    val previewWidth: Int,
    val previewHeight: Int,
    val width: Int,
    val height: Int,
    val tags: List<String>,
    val score: Int,
    val uploaderId: Int,
    val uploader: String,
    val md5: String, // o_O
    val previewUrl: String,
    val croppedUrl: String
)
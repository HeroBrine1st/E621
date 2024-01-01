@file:Suppress("SpellCheckingInspection")

package ru.herobrine1st.e621

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post

@Suppress("SpellCheckingInspection")
val samplePost = """{
      "id": 100,
      "created_at": "2022-02-05T00:00:00.000Z",
      "updated_at": "2022-02-07T00:00:00.000Z",
      "file": {
        "width": 1920,
        "height": 1080,
        "ext": "png",
        "size": 577161,
        "md5": "redacted",
        "url": "redacted"
      },
      "preview": {
        "width": 100,
        "height": 100,
        "url": "redacted"
      },
      "sample": {
        "has": false,
        "height": 1000,
        "width": 1000,
        "url": "redacted",
        "alternates": {}
      },
      "score": {
        "up": 11,
        "down": 0,
        "total": 11
      },
      "tags": {
        "general": [
          "redacted"
        ],
        "species": [
          "redacted"
        ],
        "character": [
          "redacted"
        ],
        "copyright": [
          "redacted"
        ],
        "artist": [
          "conditional_dnp",
          "tom_fischbach"
        ],
        "invalid": [],
        "lore": [],
        "meta": [
          "2022",
          "blue_and_white",
          "monochrome",
          "sketch"
        ]
      },
      "locked_tags": [
        "conditional_dnp"
      ],
      "change_seq": 37318403,
      "flags": {
        "pending": true,
        "flagged": false,
        "note_locked": false,
        "status_locked": false,
        "rating_locked": false,
        "deleted": false
      },
      "rating": "s",
      "fav_count": 0,
      "sources": [],
      "pools": [],
      "relationships": {
        "parent_id": null,
        "has_children": false,
        "has_active_children": false,
        "children": []
      },
      "approver_id": null,
      "uploader_id": 0,
      "description": "redacted",
      "comment_count": 0,
      "is_favorited": false,
      "has_notes": false,
      "duration": null
    }""".trimIndent()

class TagProcessorTest {
    private lateinit var post: Post

    private fun test(expect: Boolean, query: String) {
        assertEquals(expect, createTagProcessor(query).test(post))
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        coerceInputValues = true
    }

    @Before
    fun prepareSamplePost() {
        post = json.decodeFromString(samplePost)
    }

    @Test
    fun tagProcessor_positive_simple() {
        test(true, "rating:s")
        test(true, "rating:safe")
        test(true, "tom_fischbach")
        test(true, "conditional_dnp")
        test(true, "-my_little_pony")
    }

    @Test
    fun tagProcessor_positive_combined() {
        test(true, "tom_fischbach conditional_dnp")
        test(true, "tom_fischbach conditional_dnp -my_little_pony")
        test(true, "tom_fischbach conditional_dnp -my_little_pony rating:s")
    }

    @Test
    fun tagProcessor_negative_simple() {
        test(false, "my_little_pony")
        test(false, "rating:q")
        test(false, "rating:qwe")
    }

    @Test
    fun tagProcessor_negative_combined() {
        test(false, "tom_fischbach conditional_dnp -my_little_pony rating:q")
        test(false, "tom_fischbach conditional_dnp -my_little_pony rating:qwe")
        test(false, "conditional_dnp -tom_fischbach")
    }

    @Test
    fun tagProcessor_positive_integer() {
        test(true, "score:>10")
        test(true, "score:<12")
        test(true, "score:11")
        test(true, "score:10..12")
        test(true, "score:11..12")
        test(true, "score:10..11")
        test(true, "id:100")
        test(true, "id:<101")
        test(true, "id:>99")
    }

    @Test
    fun tagProcessor_negative_integer() {
        test(false, "score:<10")
        test(false, "score:>12")
        test(false, "score:10")
        test(false, "score:15..20")
        test(false, "score:5..10")
        test(false, "id:101")
        test(false, "id:>100")
        test(false, "id:<100")
    }

    @Test
    fun tagProcessor_positive_or() {
        test(true, "~tom_fischbach ~test")
        test(true, "~rating:q ~rating:s")
    }
}
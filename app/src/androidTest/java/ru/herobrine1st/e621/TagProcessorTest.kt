package ru.herobrine1st.e621

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post

@Suppress("SpellCheckingInspection")
val samplePost = """{
      "id": 3159688,
      "created_at": "2022-02-05T07:30:34.513Z",
      "updated_at": "2022-02-07T16:49:28.289Z",
      "file": {
        "width": 825,
        "height": 1075,
        "ext": "png",
        "size": 577161,
        "md5": "cc9467233d680292d4b5a211dadbb83f",
        "url": "https://static1.e621.net/data/cc/94/cc9467233d680292d4b5a211dadbb83f.png"
      },
      "preview": {
        "width": 115,
        "height": 150,
        "url": "https://static1.e621.net/data/preview/cc/94/cc9467233d680292d4b5a211dadbb83f.jpg"
      },
      "sample": {
        "has": false,
        "height": 1075,
        "width": 825,
        "url": "https://static1.e621.net/data/cc/94/cc9467233d680292d4b5a211dadbb83f.png",
        "alternates": {}
      },
      "score": {
        "up": 12,
        "down": -1,
        "total": 11
      },
      "tags": {
        "general": [
          "ambiguous_gender",
          "angry",
          "anthro",
          "attack",
          "attacked",
          "bisection",
          "broken",
          "close-up",
          "clothed",
          "clothing",
          "death",
          "fight",
          "open_mouth",
          "scared",
          "shocked",
          "simple_background",
          "stone_golem",
          "teeth",
          "webcomic",
          "white_background",
          "wide_eyed"
        ],
        "species": [
          "human",
          "mammal",
          "mythological_golem",
          "reptile",
          "scalie"
        ],
        "character": [
          "guardian_(twokinds)",
          "trace_legacy"
        ],
        "copyright": [
          "jewish_mythology",
          "mythology",
          "twokinds"
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
      "fav_count": 10,
      "sources": [
        "https://twokindscomic.com/images/20220204_sketch.png",
        "https://twokinds.keenspot.com/comic/1166/",
        "https://twitter.com/TwoKinds/status/1489811019723595776"
      ],
      "pools": [
        7516
      ],
      "relationships": {
        "parent_id": null,
        "has_children": false,
        "has_active_children": false,
        "children": []
      },
      "approver_id": null,
      "uploader_id": 104363,
      "description": "\"Breakable\"\r\n\r\nSketch of the 1166th page of TwoKinds.\r\n\r\n[section=Transcript]\r\nTrace: Can we break the crystal?!\r\nRoselyn: Yes, but hurry!\r\nTrace: Stoney!\r\nTrace: We need her to get out of there! Get it open!\r\nStoney:[b] Grah! [/b]\r\n????: [b]NO![/b]\r\n\r\nPage transcript provided by \"BananFisk\":/user/show/104363\r\n[/section]",
      "comment_count": 11,
      "is_favorited": false,
      "has_notes": false,
      "duration": null
    }""".trimIndent()


@RunWith(AndroidJUnit4::class)
class TagProcessorTest {
    private lateinit var post: Post

    private fun test(expect: Boolean, query: String) {
        assertEquals(expect, createTagProcessor(query).test(post))
    }

    @Before
    fun prepareSamplePost() {
        val objectMapper = jacksonObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        post = objectMapper.readValue(samplePost)
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
        test(true, "id:3159688")
        test(true, "id:<3159689")
        test(true, "id:>3159687")
    }

    @Test
    fun tagProcessor_negative_integer() {
        test(false, "score:<10")
        test(false, "score:>12")
        test(false, "score:10")
        test(false, "score:15..20")
        test(false, "score:5..10")
        test(false, "id:3159689")
        test(false, "id:>3159689")
        test(false, "id:<3159687")
    }

    @Test
    fun tagProcessor_positive_or() {
        test(true, "~tom_fischbach ~test")
        test(true, "~rating:q ~rating:s")
    }
}
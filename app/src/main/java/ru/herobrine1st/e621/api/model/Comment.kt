package ru.herobrine1st.e621.api.model

import androidx.compose.runtime.Stable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.Jsoup
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class Comment(
    val id: Int, // data-comment-id
    val score: Int, // data-score
    val authorId: Int, // data-creator-id
    val dataIsSticky: Boolean, // data-is-sticky wtf is that
    val isDeleted: Boolean, // data-is-deleted
    val authorName: String, // author-info -> name-rank -> author-name -> a content
    val levelString: String, // author-info -> name-rank -> text; idk what could it be; Name from source code
    val avatarPostId: Int, // avatar -> data-id
    val avatarPost: PostReduced?, // from the same response
    val postedAt: Instant, // post-time -> datetime attribute
    val content: String, // content -> body -> styled-dtext content
)

@JsonIgnoreProperties("warning_type", "warning_user_id")
data class CommentBB(
    val id: Int,
    val createdAt: OffsetDateTime,
    @JsonProperty("post_id")
    val parentPostId: Int,
    val creatorId: Int,
    val updaterId: Int,
    val body: String,
    val score: Int,
    val updatedAt: OffsetDateTime,
    val doNotBumpPost: Boolean, // wtf
    val isHidden: Boolean, // okay maybe I understand it
    val isSticky: Boolean, // wtf
    // val warningType: Unknown?,
    // val warningUserId: Unknown?,
    val creatorName: String,
    val updaterName: String
) {
    companion object {
        val sample
            @Stable get() = CommentBB(
                id = 0,
                createdAt = OffsetDateTime.MIN,
                parentPostId = 0,
                creatorId = 0,
                updaterId = 0,
                body = "Some text",
                score = 0,
                updatedAt = OffsetDateTime.MIN,
                doNotBumpPost = false,
                isHidden = false,
                isSticky = false,
                creatorName = "Creator name",
                updaterName = "Updater name"
            )
    }
}

@Suppress("SpellCheckingInspection")
val dateTimeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]XXX") }

fun parseComments(response: PostCommentsEndpoint): List<Comment> {
    val document = Jsoup.parse(response.html)
    return document.body().children().map { article ->
        val id = article.attr("data-comment-id").toInt()
        val score = article.attr("data-score").toInt()
        val authorId = article.attr("data-creator-id").toInt()
        val dataIsSticky = article.attr("data-is-sticky").toBooleanStrict()
        val isDeleted = article.attr("data-is-deleted").toBooleanStrict()
        val authorName: String
        val levelString: String
        val avatarPostId: Int
        val postedAt: Instant
        article.getElementsByClass("author-info").first()!!.let { authorInfo ->
            authorInfo.getElementsByClass("name-rank").first()!!.let { nameRank ->
                authorName =
                    nameRank.getElementsByClass("author-name").first()!!.getElementsByTag("a")
                        .first()!!
                        .wholeText()
                levelString = nameRank.ownText()
            }
            avatarPostId = authorInfo.getElementsByClass("avatar").first()
                ?.getElementsByClass("post-thumb")?.first()?.attr("data-id")
                ?.toInt() ?: 0
            postedAt = Instant.from(
                dateTimeFormatter.parse(
                    authorInfo.getElementsByTag("time").first()!!.attr("datetime")
                )
            )
        }
        val content = article.getElementsByClass("content").first()!!
            .getElementsByClass("body").first()!!
            .getElementsByClass("styled-dtext").first()!!.html()
        Comment(
            id,
            score,
            authorId,
            isDeleted,
            dataIsSticky,
            authorName,
            levelString,
            avatarPostId,
            response.posts[avatarPostId],
            postedAt,
            content
        )
    }
}

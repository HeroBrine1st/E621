package ru.herobrine1st.e621.api.model

import org.jsoup.Jsoup
import java.time.Instant
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
    val post: PostReduced?, // from the same response
    val postedAt: Instant, // post-time -> datetime attribute
    val content: String, // content -> body -> styled-dtext content
)

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
        val postsMap = response.posts.associateBy { it.id }
        Comment(
            id,
            score,
            authorId,
            isDeleted,
            dataIsSticky,
            authorName,
            levelString,
            avatarPostId,
            postsMap[avatarPostId],
            postedAt,
            content
        )
    }
}

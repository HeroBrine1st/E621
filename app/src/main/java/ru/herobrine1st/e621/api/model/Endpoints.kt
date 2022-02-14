package ru.herobrine1st.e621.api.model

data class PostEndpoint(val posts: List<Post>)

data class PostCommentsEndpoint(val html: String, val posts: List<PostReduced>)
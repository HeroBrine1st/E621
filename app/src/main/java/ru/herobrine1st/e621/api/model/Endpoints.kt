package ru.herobrine1st.e621.api.model

data class PostEndpoint(val posts: List<Post>)

data class PostCommentsEndpoint(val html: String, val posts: List<PostReduced>)

data class PostVoteEndpoint(val up: Int, val down: Int, val total: Int, val ourScore: Int)
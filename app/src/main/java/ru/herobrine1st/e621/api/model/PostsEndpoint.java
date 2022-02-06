package ru.herobrine1st.e621.api.model;

import java.util.List;

public class PostsEndpoint {
    public List<Post> getPosts() {
        return posts;
    }

    List<Post> posts;
}

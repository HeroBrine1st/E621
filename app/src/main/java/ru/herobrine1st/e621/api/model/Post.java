package ru.herobrine1st.e621.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {
    public int getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public Score getScore() {
        return score;
    }

    public Tags getTags() {
        return tags;
    }

    public String getRating() {
        return rating;
    }

    public String getDescription() {
        return description;
    }

    public Relationships getRelationships() {
        return relationships;
    }

    int id;
    File file;
    Score score;
    Tags tags;
    String rating;
    String description;
    Relationships relationships;
}

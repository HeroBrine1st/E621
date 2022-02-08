package ru.herobrine1st.e621.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties({"invalid"})
public class Tags {
    List<String> general;
    List<String> species;
    List<String> character;
    List<String> copyright;
    List<String> artist;
    List<String> lore;
    List<String> meta;

    public List<String> getGeneral() {
        return general;
    }

    public List<String> getSpecies() {
        return species;
    }

    public List<String> getCharacter() {
        return character;
    }

    public List<String> getCopyright() {
        return copyright;
    }

    public List<String> getArtist() {
        return artist;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getMeta() {
        return meta;
    }
}

package ru.kluchikhin.vkapi;

import java.util.Objects;

public class VKTrack {
    private final String id;
    private final String artist;
    private final String title;
    private final String duration;
    private final String owner_id;
    private String url;

    public VKTrack(String id, String artist, String title, String duration, String url, String owner_id) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.duration = duration;
        this.url = url;
        this.owner_id = owner_id;
    }

    public String getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getUrl() {
        return url;
    }
    
    public void setURL(String newURL){
        url = newURL;
    }

    public String getOwner_id() {
        return owner_id;
    }

    @Override
    public String toString() {
        return artist + " - " + title;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VKTrack other = (VKTrack) obj;
        return Objects.equals(this.id, other.id);
    }
    
    
}
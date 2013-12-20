package ru.kluchikhin.musiclibrary;

public interface PlaylistChangeListener {
    public void addPlaylistToLibraryDone(PlaylistChangeEvent event);
    public void deletePlaylistFromLibraryDone(PlaylistChangeEvent event);
}

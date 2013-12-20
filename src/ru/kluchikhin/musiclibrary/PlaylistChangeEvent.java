package ru.kluchikhin.musiclibrary;

import java.util.EventObject;

public class PlaylistChangeEvent extends EventObject {
    
    public PlaylistChangeEvent(Object source) {
        super(source);
    }
}

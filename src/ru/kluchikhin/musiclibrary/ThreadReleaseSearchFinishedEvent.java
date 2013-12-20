package ru.kluchikhin.musiclibrary;

import java.util.EventObject;
import java.util.List;
import ru.kluchikhin.vkapi.VKTrack;

public class ThreadReleaseSearchFinishedEvent extends EventObject {
    private final List<VKTrack> releaseTracks;
    private int releaseTrackCount = 0;
    private int exceptionCode = 0;

    public ThreadReleaseSearchFinishedEvent(Object source, List<VKTrack> tracks, int count, int exceptionCode) {
        super(source);
        this.releaseTracks = tracks;
        this.releaseTrackCount = count;
        this.exceptionCode = exceptionCode;
    }
    
    public List<VKTrack> getReleaseTracks(){
        return releaseTracks;
    }
    
    public int getReleaseTrackCount(){
        return releaseTrackCount;
    }

    public int getExceptionCode() {
        return exceptionCode;
    }
}

package ru.kluchikhin.musiclibrary;

import java.util.EventObject;

public class ThreadDownloadFileFinishedEvent extends EventObject {
    private String filePath = null;

    public ThreadDownloadFileFinishedEvent(Object source, String downloadedFilePath) {
        super(source);
        this.filePath = downloadedFilePath;
    }
    
    public String getDownloadedFilePath(){
        return filePath;
    }
}

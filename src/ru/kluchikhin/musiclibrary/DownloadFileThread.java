package ru.kluchikhin.musiclibrary;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;
import ru.kluchikhin.vkapi.Utils;
import ru.kluchikhin.vkapi.VKApi;
import ru.kluchikhin.vkapi.VKTrack;

public class DownloadFileThread extends Thread {
    private final ArrayList<ThreadDownloadFileFinishedEventListener> listeners = new ArrayList<>();
    private final VKTrack track;
    
    public void addThreadFileDownloadFinishedEventListener(ThreadDownloadFileFinishedEventListener listener){
        listeners.add(listener);
    }
    
    public ThreadDownloadFileFinishedEventListener[] getThreadFileDownloadFinishedEventListeners(){
        return listeners.toArray(new ThreadDownloadFileFinishedEventListener[listeners.size()]);
    }
  
    public void removeThreadDownloadFileFinishedEventListener(ThreadDownloadFileFinishedEventListener listener){
        listeners.remove(listener);
    }
    
    public DownloadFileThread(VKTrack track){
        this.track = track;
    }
    
    @Override
    public void run() {
        String filePath = "";
        try {
            if(new File(Utils.getDownloadableName(new URL(track.getUrl()))).exists()) filePath = Utils.getDownloadableName(new URL(track.getUrl()));
            else{
            VKApi.getInstance().updateURLForTrack(track);
            filePath = Utils.downloadFile(new URL(track.getUrl()));
        }
        
        } catch (MalformedURLException ex) {
            Logger.getLogger(DownloadFileThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException | ParseException | IOException ex) {
            Logger.getLogger(DownloadFileThread.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            ThreadDownloadFileFinishedEvent ev = new ThreadDownloadFileFinishedEvent(this, filePath);
            for(ThreadDownloadFileFinishedEventListener l: listeners)
                l.threadDownloadFileFinished(ev);
        }

    }
}

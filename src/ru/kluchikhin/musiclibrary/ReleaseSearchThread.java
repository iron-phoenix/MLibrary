package ru.kluchikhin.musiclibrary;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.parser.ParseException;
import org.musicbrainz.model.entity.ReleaseWs2;
import org.musicbrainz.model.searchresult.ReleaseResultWs2;
import ru.kluchikhin.vkapi.Utils;
import ru.kluchikhin.vkapi.VKTrack;

public class ReleaseSearchThread extends Thread {
    private final ArrayList<ThreadReleaseSearchFinishedEventListener> listeners = new ArrayList<>();
    private final String title;
    private final String artist;
    
    
    public void addThreadReleaseSearchFinishedEventListener(ThreadReleaseSearchFinishedEventListener listener){
        listeners.add(listener);
    }
    
    public ThreadReleaseSearchFinishedEventListener[] getThreadFileDownloadFinishedEventListeners(){
        return listeners.toArray(new ThreadReleaseSearchFinishedEventListener[listeners.size()]);
    }
  
    public void removeThreadReleaseSearchFinishedEventListener(ThreadReleaseSearchFinishedEventListener listener){
        listeners.remove(listener);
    }
    
    public ReleaseSearchThread(String title, String artist){
        this.title = title;
        this.artist = artist;
    }
    
    @Override
    public void run() {
        ReleaseWs2 result = null;
        List<VKTrack> searchResults = null;
        int exceptionCode = 0;
        try {
            String albumArtist = artist.replaceAll("/", "\\\\/");
            List<ReleaseResultWs2> resultReleases = Utils.getReleases(title, albumArtist);
            
            result = resultReleases.get(0).getRelease();
            for(ReleaseResultWs2 res: resultReleases)
                if(res.getRelease().getTitle().equals(title)){
                    result = res.getRelease();
                    break;
                }
            
            if(artist.equals(""))
                albumArtist = result.getArtistCredit().getNameCredits().get(0).getArtistName();
            
            searchResults = Utils.getTracksOfRelease(result.getId(), albumArtist);
        } catch (ParseException ex) {
            exceptionCode = 1;
        } catch (URISyntaxException ex) {
            exceptionCode = 2;
        } catch (IOException ex) {
            exceptionCode = 3;
        } catch (InterruptedException ex) {
            exceptionCode = 4;
        } catch (java.text.ParseException ex) {
            exceptionCode = 5;
        } finally {
            int trackCounts = (result == null ? 0 : result.getTracksCount());
            ThreadReleaseSearchFinishedEvent ev = new ThreadReleaseSearchFinishedEvent(this, searchResults, trackCounts, exceptionCode);
            for(ThreadReleaseSearchFinishedEventListener l: listeners)
                l.threadSearchFileFinished(ev);
        }

    }
}

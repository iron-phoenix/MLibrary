package ru.kluchikhin.musiclibrary;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.ParseException;
import ru.kluchikhin.vkapi.VKApi;
import ru.kluchikhin.vkapi.VKTrack;

public class MediaLibrary {
    
    private final ArrayList<PlaylistChangeListener> listeners = new ArrayList<>();
    
    public void addAddPlaylistToLibraryListener(PlaylistChangeListener listener){
        listeners.add(listener);
    }
    
    public PlaylistChangeListener[] getAddPlaylistToLibraryListeners(){
        return listeners.toArray(new PlaylistChangeListener[listeners.size()]);
    }
  
    public void removeAddPlaylistToLibraryListener(PlaylistChangeListener listener){
        listeners.remove(listener);
    }
    
    public static MediaLibrary getInstance(){
        return SingletonHolder.instance;
    }
    
    public void init(String libraryPath) throws IOException, URISyntaxException, ParseException{
        this.libraryPath = libraryPath;
        loadLibrary();
    }
    
    public List<VKTrack> getTracksOfPlaylist(String libraryName, String playlistName){
        if(!sources.containsKey(libraryName)) return null;
        return sources.get(libraryName).get(playlistName);
    }
    
    public Set<String> getplaylistNames(String libraryName){
        if(!sources.containsKey(libraryName)) return null;
        return sources.get(libraryName).keySet();
    }
    
    public boolean addTrackToPlaylist(String libraryName, String playlistName, VKTrack track) throws IOException{
        if(!sources.containsKey(libraryName)) return false;
        if(!sources.get(libraryName).containsKey(playlistName)) addPlaylist(libraryName, playlistName);
        if(sources.get(libraryName).get(playlistName).contains(track)) return false;
        sources.get(libraryName).get(playlistName).add(track);
        dumpPlaylist(libraryName, playlistName);
        return true;
    }
    
    public boolean addTrackListToPlaylist(String libraryName, String playlistName, List<VKTrack> trackList) throws IOException{
        if(!sources.containsKey(libraryName)) return false;
        if(!addPlaylist(libraryName, playlistName)) return false;
        for(VKTrack track: trackList){
            if(sources.get(libraryName).get(playlistName).contains(track)) return false;
            sources.get(libraryName).get(playlistName).add(track);
        }
        if(libraryName.equals("Library")) dumpPlaylist(libraryName, playlistName);
        return true;
    }
    
    public void deleteTrackFromList(String libraryName, String playlistName, VKTrack track){
        if(!sources.containsKey(libraryName)) return;
        if(sources.get(libraryName).get(playlistName).contains(track)){
            sources.get(libraryName).get(playlistName).remove(track);
        }
    }
    
    public boolean addPlaylist(String libraryName, String playlistName) throws IOException{
        if(!sources.containsKey(libraryName)) return false;
        if(playlistName == null) return false;
        if(sources.get(libraryName).containsKey(playlistName) || playlistName.equals("")) return false;
        sources.get(libraryName).put(playlistName, new ArrayList<VKTrack>());
        PlaylistChangeEvent ev = new PlaylistChangeEvent(this);
        for(PlaylistChangeListener listener : listeners)
            listener.addPlaylistToLibraryDone(ev);
        if(libraryName.equals("Library")) dumpLibrary(libraryName);
        return true;
    }
    
    public void deletePlaylist(String libraryName, String playlistName) throws IOException{
        if(!sources.containsKey(libraryName)) return;
        if(sources.get(libraryName).containsKey(playlistName)){
            sources.get(libraryName).remove(playlistName);
            sources.get(libraryName).keySet().remove(playlistName);
            PlaylistChangeEvent ev = new PlaylistChangeEvent(this);
            for(PlaylistChangeListener listener : listeners)
                listener.deletePlaylistFromLibraryDone(ev);
            File playlistFile = new File(libraryPath + "\\" + playlistName);
            playlistFile.delete();
            dumpLibrary(libraryName);
        }
    }
    
    public void dumpPlaylist(String libraryName, String playlistName) throws IOException {
        if(!sources.containsKey(libraryName)) return;
        Gson gson = new Gson();
        String playlistNameFileName = playlistName.replaceAll("/", "___");
        String json = gson.toJson(sources.get(libraryName).get(playlistName));
        try (FileWriter fileWriter = new FileWriter(libraryPath + "\\" + playlistNameFileName, false)) {
            fileWriter.write(json);
        }
    }
    
    public void dumpLibrary(String libraryName) throws IOException {
        if(!sources.containsKey(libraryName)) return;
        try (FileWriter libraryWriter = new FileWriter(libraryPath + "\\library.lib", false)) {
            for (String playlistName : sources.get(libraryName).keySet()) {
                libraryWriter.write(playlistName + System.getProperty("line.separator"));
            }
        }
    }
    
    public void dumpAll(String libraryName) throws IOException{
        if(!sources.containsKey(libraryName)) return;
        dumpLibrary(libraryName);
        for(String playlistName: sources.get(libraryName).keySet()){
            dumpPlaylist(libraryName, playlistName);
        }
    }
    
    public VKTrack nextTrack(String libraryName, String playlistName, VKTrack track){
        if(!sources.containsKey(libraryName)) return null;
        if(sources.get(libraryName).get(playlistName).indexOf(track) == sources.get(libraryName).get(playlistName).size() - 1){
            return sources.get(libraryName).get(playlistName).get(0);
        }
        return sources.get(libraryName).get(playlistName).get(sources.get(libraryName).get(playlistName).indexOf(track) + 1);
    }
    
    public void cleanTrackDirectory() throws IOException{
        FileUtils.cleanDirectory(new File(libraryPath + "\\mp3"));
    }
    
    public Set<String> getLibrariesNames(){
        return sources.keySet();
    }
    
    //----------------------------------------------------------------------
    
    private void loadLibrary() throws FileNotFoundException, IOException, ParseException, URISyntaxException {
        File libFile = new File(libraryPath + "\\library.lib");
        Map<String, List<VKTrack> > library = new HashMap<>();
        if (libFile.exists()) {
            try (BufferedReader brlibrary = new BufferedReader(new FileReader(libFile))) {
                String playlistName = "";
                while ((playlistName = brlibrary.readLine()) != null) {
                    String playlistNameFileName = playlistName.replaceAll("/", "___");
                    try (BufferedReader br = new BufferedReader(new FileReader(libraryPath + "\\" + playlistNameFileName))) {
                        String json = br.readLine();
                        Gson gson = new Gson();
                        List<VKTrack> playlist = gson.fromJson(json, new TypeToken< List<VKTrack>>() {}.getType());
                        library.put(playlistName, playlist);
                    }
                }
            }
        }
        Map<String, List<VKTrack> > VKLibrary = VKApi.getInstance().getTracksFromVKAlbums();
        sources.put("Library", library);
        sources.put("VK Library", VKLibrary);
    }
    
    private static class SingletonHolder{
        private final static MediaLibrary instance = new MediaLibrary();
    }
    
    //private Map<String, List<VKTrack> > library;
    private Map<String, Map<String, List<VKTrack> > > sources = new HashMap<>();
    private String libraryPath;
}

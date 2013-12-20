package ru.kluchikhin.vkapi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;
import org.json.simple.parser.ParseException;

public class VKApi {

    public static VKApi getInstance() {
        return SingletonHolder.instance;
    }
    
    public boolean isAccessTokenValid(String accessToken, long expires, String userId) throws ParseException, URISyntaxException, IOException{
        access_token = accessToken;
        if(getTrackList("Yello Submarine") == null) return false;
        user_id = userId;
        return true;
    }

    public void setAccessToken(String access_token, String user_id) {
        this.access_token = access_token;
        this.user_id = user_id;
    }
    
    public boolean isAccessTokenExists(){
        return !access_token.isEmpty();
    }
    
    public String getAuthURL(){
        return authURL +
                "?client_id=" + clientId +
                "&scope=" + scope +
                "&redirect_uri=" + redirectURI +
                "&display=" + display +
                "&v=" + apiVersion +
                "&response_type=" + responseType;
    }

    public List<VKTrack> getTrackList(String trackName)
            throws ParseException, URISyntaxException, IOException {
        return getTrackList(trackName, 10, 0);
    }

    public List<VKTrack> getTrackList(String trackName, int count)
            throws ParseException, URISyntaxException, IOException {
        return getTrackList(trackName, count, 0);
    }

    public List<VKTrack> getTrackList(String trackName, int count, int offset)
            throws ParseException, URISyntaxException, IOException {
        return Utils.getVKTrackListFromJson(audioSearch(trackName, count, offset));
    }
    
    private boolean compareTime(String duration1, String duration2) throws java.text.ParseException{
        int d1 = Integer.valueOf(duration1);
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        Date d2Date = formatter.parse(duration2);
        int d2 = d2Date.getMinutes() * 60 + d2Date.getSeconds();
        return (d1 == d2 || d1 == d2 - 1 || d1 == d2 + 1 || d1 == d2 - 2 || d1 == d2 + 2);
    }
    
    public VKTrack getOneTrack(String trackName, String duration) throws ParseException, URISyntaxException, IOException, java.text.ParseException{
        List<VKTrack> result = getTrackList(trackName, 10, 0);
        if(result == null) return null;
        for(VKTrack track: result){
            if(compareTime(track.getDuration(), duration)) return track;
        }
        if(!result.isEmpty()){
            return result.get(0);
        }
        else{
            return null;
        }
    }

    public String downloadTrack(VKTrack track) throws IOException {
        return Utils.downloadFile(new URL(track.getUrl()));
    }
    
    public void updateURLForTrack(VKTrack track) throws URISyntaxException, IOException, ParseException{
        String audioId = track.getId();
        String ownerId = track.getOwner_id();
        VKTrack newTrack = Utils.getVKTrackFromJsonString(getAudioById(ownerId, audioId));
        track.setURL(newTrack.getUrl());
    }
    
    private String getAudioById(String ownerId, String audioId) throws URISyntaxException, IOException{
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioGetByIdMethod)
                .setParameter("audios", ownerId + "_" + audioId)
                .setParameter("v", apiVersion)
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }

    private String audioSearch(String title, int count, int offset)
            throws URISyntaxException, IOException {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioSearchMethod)
                .setParameter("q", title)
                .setParameter("v", apiVersion)
                .setParameter("count", String.valueOf(count))
                .setParameter("offset", String.valueOf(offset))
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }
    
    public Map<String, List<VKTrack> > getTracksFromVKAlbums() throws ParseException, URISyntaxException, IOException{
        Map<String, List<VKTrack> > albumTracks = new HashMap<>();
        for(String[] album: getAlbumsList()){
            albumTracks.put(album[1], Utils.getVKTrackListFromJson(getAudio(album[0], 0, 0)));
        }
        return albumTracks;
    }
    
    public boolean addNewAlbumToVKPlaylists(String title, List<VKTrack> tracks) throws URISyntaxException, IOException, ParseException, InterruptedException{
        String jsonAddResponse = addAlbum(title);
        String newAlbumId = Utils.getAlbumId(jsonAddResponse);
        return Utils.getMoveToAlbumResponse(moveToAlbum(newAlbumId, tracks));
    }
    
    private List<String[]> getAlbumsList() throws ParseException, URISyntaxException, IOException{
        String response = getAlbums(0, 0);
        return Utils.getVKAlbumstFromJson(response);
    }
    
    private String getAlbums(int count, int offset)
            throws URISyntaxException, IOException {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioGetAlbumsMethod)
                .setParameter("owner_id", user_id)
                .setParameter("v", apiVersion)
                .setParameter("count", String.valueOf(count))
                .setParameter("offset", String.valueOf(offset))
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }
    
    private String getAudio(String albumId, int count, int offset)
            throws URISyntaxException, IOException {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioGetMethod)
                .setParameter("owner_id", user_id)
                .setParameter("album_id", albumId)
                .setParameter("v", apiVersion)
                .setParameter("count", String.valueOf(count))
                .setParameter("offset", String.valueOf(offset))
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }
    
    private String addAlbum(String title)
            throws URISyntaxException, IOException {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioAddAlbumMethod)
                .setParameter("title", title)
                .setParameter("v", apiVersion)
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }
    
    private String moveToAlbum(String albumId, List<VKTrack> tracks)
            throws URISyntaxException, IOException, ParseException, InterruptedException {
        StringBuilder tracksIds = new StringBuilder();
        String prefix = "";
        for(VKTrack track: tracks){
            String trackId = Utils.getTrackId(addAudio(track));
            if(trackId.equals("")) return "";
            tracksIds.append(prefix).append(trackId);
            prefix = ",";
            Thread.sleep(500);
        }
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioMoveToAlbumMethod)
                .setParameter("album_id", albumId)
                .setParameter("audio_ids", tracksIds.toString())
                .setParameter("v", apiVersion)
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }
    
    private String addAudio(VKTrack track)
            throws URISyntaxException, IOException {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(apiHost).setPath(apiAudioAddMethod)
                .setParameter("owner_id", track.getOwner_id())
                .setParameter("audio_id", track.getId())
                .setParameter("v", apiVersion)
                .setParameter("access_token", access_token);
        URI uri = uribuilder.build();
        return NetworkManager.getInstance().getResponse(uri);
    }

    private static class SingletonHolder {

        private static final VKApi instance = new VKApi();
    }

    private String access_token = "";
    private String user_id = "";
    private final String authURL = "https://oauth.vk.com/authorize";
    private final String clientId = "4003323";
    private final String scope = "audio";
    private final String redirectURI = "http://oauth.vk.com/blank.html";
    private final String display = "popup";
    private final String responseType = "token";
    private final String apiHost = "api.vk.com";
    private final String apiAudioSearchMethod = "/method/audio.search";
    private final String apiAudioGetByIdMethod = "/method/audio.getById";
    private final String apiAudioGetAlbumsMethod = "/method/audio.getAlbums";
    private final String apiAudioGetMethod = "/method/audio.get";
    private final String apiAudioAddAlbumMethod = "/method/audio.addAlbum";
    private final String apiAudioMoveToAlbumMethod = "/method/audio.moveToAlbum";
    private final String apiAudioAddMethod = "/method/audio.add";
    private final String apiVersion = "5.4";
}

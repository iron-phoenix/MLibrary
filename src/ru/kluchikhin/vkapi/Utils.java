package ru.kluchikhin.vkapi;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.musicbrainz.controller.Recording;
import org.musicbrainz.controller.Release;
import org.musicbrainz.model.searchresult.RecordingResultWs2;
import org.musicbrainz.model.searchresult.ReleaseResultWs2;

public final class Utils {
    public static String downloadFile(URL url) throws IOException{
        String path = getDownloadableName(url);
        File destination = new File(path);
        FileUtils.copyURLToFile(url, destination);
        return path;
    }
    
    public static String getDownloadableName(URL url){
        String[] tokens = url.getPath().split("/");
        return System.getProperty("user.dir") + "\\library\\mp3\\" + tokens[tokens.length - 1];
    }

    private static VKTrack getVKTrackFromJSON(JSONObject jsonTrackObject) {
        VKTrack track = new VKTrack(jsonTrackObject.get("id").toString(),
                                    jsonTrackObject.get("artist").toString(),
                                    jsonTrackObject.get("title").toString(),
                                    jsonTrackObject.get("duration").toString(),
                                    jsonTrackObject.get("url").toString(),
                                    jsonTrackObject.get("owner_id").toString());
        return track;
    }
    
    public static VKTrack getVKTrackFromJsonString(String jsonString) throws ParseException{
        List<VKTrack> trackList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(jsonString);
        JSONArray response = (JSONArray) jsonResponse.get("response");
        for (Object trackObject : response) {
            JSONObject jsonTrackObject = (JSONObject) trackObject;
            return getVKTrackFromJSON(jsonTrackObject);
        }
        return null;
    }
    
    public static List<VKTrack> getVKTrackListFromJson(String json) throws ParseException{
        List<VKTrack> trackList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(json);
        JSONObject response = (JSONObject) jsonResponse.get("response");
        if(response == null) return null;
        JSONArray mp3list = (JSONArray) response.get("items");
        for (Object trackObject : mp3list) {
            JSONObject jsonTrackObject = (JSONObject) trackObject;
            trackList.add(getVKTrackFromJSON(jsonTrackObject));
        }
        return trackList;
    }
    
    private static String[] getVKAlbumFromJSON(JSONObject jsonObject){
        String album_id = jsonObject.get("album_id").toString();
        String title = jsonObject.get("title").toString();
        return new String[] {album_id, title};
    }
    
    public static List<String[]> getVKAlbumstFromJson(String json) throws ParseException{
        List<String[]> albumList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(json);
        JSONObject response = (JSONObject) jsonResponse.get("response");
        if(response == null) return null;
        JSONArray albumsList = (JSONArray) response.get("items");
        for (Object trackObject : albumsList) {
            JSONObject jsonTrackObject = (JSONObject) trackObject;
            albumList.add(getVKAlbumFromJSON(jsonTrackObject));
        }
        return albumList;
    }
    
    public static String getAlbumId(String json) throws ParseException{
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(json);
        JSONObject response = (JSONObject) jsonResponse.get("response");
        if(response == null) return null;
        return response.get("album_id").toString();
    }
    
    public static String getTrackId(String json) throws ParseException{
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(json);
        if(!jsonResponse.containsKey("response")) return "";
        return jsonResponse.get("response").toString();
    }
    
    public static boolean getMoveToAlbumResponse(String json){
        return json.equals("response: 1");
    }
    
    public static boolean checkAccessToken(){
        JSONParser parser = new JSONParser();
        try{
            JSONObject jsonObj;
            try (FileReader accessTokenReader = new FileReader(access_file)) {
                jsonObj = (JSONObject) parser.parse(accessTokenReader);
            }
            String token = (String) jsonObj.get("access_token");
            long expires = (Long) jsonObj.get("expires");
            String userId = (String) jsonObj.get("user_id");
            long currentMilliseconds = System.currentTimeMillis();
            if(expires > currentMilliseconds){
                return VKApi.getInstance().isAccessTokenValid(token, expires, userId);
            }
            else return false;
        }
        catch(Exception e){
            return false;
        }
    }
    
    public static void saveAccessToken(String token, String time, String id){
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("access_token", token);
        jsonObj.put("expires", System.currentTimeMillis() + Long.parseLong(time) * 1000);
        jsonObj.put("user_id", id);
        
        try(FileWriter fileWriter = new FileWriter(access_file)){
            fileWriter.write(jsonObj.toJSONString());
            fileWriter.flush();
            fileWriter.close();
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
    }
    
    public static List<VKTrack> getTracksOfRelease(String releaseId, String currentArtist) throws ParseException, URISyntaxException, IOException, InterruptedException, java.text.ParseException{
        Recording recording = new Recording();
        recording.getSearchFilter().setLimit((long)50);
        recording.search("reid:" + releaseId);
        
        List<RecordingResultWs2> recordingResults  =  recording.getFirstSearchResultPage();
        
        List<VKTrack> searchResults = new ArrayList<>();
        for(RecordingResultWs2 result: recordingResults){
            VKTrack newTrack = VKApi.getInstance().getOneTrack(currentArtist + " " + result.getRecording().getTitle(), result.getRecording().getDuration());
            if(newTrack != null){
                searchResults.add(newTrack);
                Thread.sleep(500);
            }
            else{
                newTrack = VKApi.getInstance().getOneTrack(result.getRecording().getTitle(), result.getRecording().getDuration());
                if(newTrack != null){
                    searchResults.add(newTrack);
                    Thread.sleep(500);
            }
            }
        }
        return searchResults;
    }
    
    public static List<ReleaseResultWs2> getReleases(String title, String artist){
        Release release = new Release();
        release.getSearchFilter().setLimit((long)10);
        release.getSearchFilter().setMinScore((long)100);
        String request = "release:" + title;
        if(!artist.equals("")){
            request += " AND " + "artist:" + artist;
        }
        release.search(request);
        
        return release.getFirstSearchResultPage();
    }
    
    private static final String access_file = "access_token.json";
}
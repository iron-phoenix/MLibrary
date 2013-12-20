package ru.kluchikhin.vkapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class NetworkManager {
    public String getResponse(URI uri) throws IOException{
        String response = "";
        HttpGet httpGet = new HttpGet(uri);
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        if (null != httpEntity) {
            InputStream is = null;
            try {
                is = httpEntity.getContent();
                response = IOUtils.toString(is);
            } finally {
                if (null != is) {
                    is.close();
                }
            }
        }
        return response;
    }
    
    public static NetworkManager getInstance(){
        return SingletonHolder.instance;
    }
    
    private static class SingletonHolder{
        private static final NetworkManager instance = new NetworkManager();
    }
    
    private NetworkManager(){
        httpClient = new DefaultHttpClient();
    }
    
    private final HttpClient httpClient;
}

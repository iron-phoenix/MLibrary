package ru.kluchikhin.musiclibrary;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.simple.parser.ParseException;
import ru.kluchikhin.vkapi.VKApi;
import ru.kluchikhin.vkapi.Utils;

public class MusicLibrary extends Application {

    @Override
    public void start(final Stage stage) throws Exception {
        initWebViewAuth(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    private void loadMainForm(Stage stage) throws IOException, URISyntaxException, ParseException {

        MediaLibrary.getInstance().init(System.getProperty("user.dir") + "\\library");

        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Music Library");
        stage.show();
    }
    
    private void initWebViewAuth(final Stage stage){
        if(Utils.checkAccessToken()){
            try {
                loadMainForm(stage);
                return;
            } catch (IOException | URISyntaxException | ParseException ex) {
                Logger.getLogger(MusicLibrary.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        final Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        final StackPane dialogPane = new StackPane();

        WebView x = new WebView();
        final WebEngine ex = x.getEngine();
        ex.load(VKApi.getInstance().getAuthURL());
        ex.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldLocation, String newLocation) {
                if (ex.locationProperty().getValue().contains("access_token")) {
                    try {
                        String token = newLocation.split("#")[1].split("&")[0].split("=")[1];
                        String time = newLocation.split("#")[1].split("&")[1].split("=")[1];
                        String id = newLocation.split("#")[1].split("&")[2].split("=")[1];
                        Utils.saveAccessToken(token, time, id);
                        VKApi.getInstance().setAccessToken(token, id);
                        
                        loadMainForm(stage);
                        dialog.close();
                    } catch (IOException | URISyntaxException | ParseException ex1) {
                        Logger.getLogger(MusicLibrary.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
        });

//        dialog.setOnHiding(new EventHandler<WindowEvent>() {
//
//            @Override
//            public void handle(WindowEvent t) {
//                if (!VKApi.getInstance().isValidAccessToken()) {
//                    Platform.runLater(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            stage.close();
//                        }
//                    });
//                }
//            }
//        });

        dialogPane.getChildren().add(x);
        Scene dialogScene = new Scene(dialogPane, 750, 500);
        dialog.setTitle("Web View");
        dialog.setScene(dialogScene);
        dialog.show();
    }
}

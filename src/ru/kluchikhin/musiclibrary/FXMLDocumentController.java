package ru.kluchikhin.musiclibrary;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialogs;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.simple.parser.ParseException;
import ru.kluchikhin.vkapi.Utils;
import ru.kluchikhin.vkapi.VKApi;
import ru.kluchikhin.vkapi.VKTrack;

public class FXMLDocumentController implements Initializable {
    private Media media = null;
    private MediaPlayer player = null;
    private String currentLibraryName;
    private String currentPlaylist;
    private VKTrack currentTrack;
    private int currentTrackIndex = 0;
    private DownloadFileThread downloadThread;

    @FXML
    private Button playButton;
    @FXML
    private Button stopButton;
    @FXML
    private ListView playlist;
    @FXML
    private TreeView<String> libraryTree;
    @FXML
    private Slider progressSlider;
    @FXML
    private Label timeLabel;
    @FXML
    private Button searchButton;
    @FXML
    private TextField titleSearchField;
    @FXML
    private TextField artistSearchField;
    @FXML
    private ListView searchPlaylist;
    @FXML
    private Label resultLabel;
    @FXML
    private Button addToLibraryButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label progressLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label playerStatusLabel;
    @FXML
    private Button logoutButton;
    
    private MediaPlayer createMediaPlayer(Media media) {
        player = new MediaPlayer(media);

        player.setOnReady(new Runnable() {

            @Override
            public void run() {
                progressSlider.setMin(0.0);
                progressSlider.setValue(0.0);
                progressSlider.setMax(player.getTotalDuration().toSeconds());
            }
        });

        player.setOnPlaying(new Runnable() {

            @Override
            public void run() {
                ImageView pauseImageView = new ImageView("file:///" + System.getProperty("user.dir") + "\\icons\\Pause.png");
                pauseImageView.setFitHeight(20.0);
                pauseImageView.setFitWidth(20.0);
                playButton.setGraphic(pauseImageView);

                stopButton.setDisable(false);
                
                playerStatusLabel.setText("Сейчас играет: " + currentTrack.getArtist() + " - " + currentTrack.getTitle());
            }
        });

        player.setOnPaused(new Runnable() {

            @Override
            public void run() {
                ImageView pauseImageView = new ImageView("file:///" + System.getProperty("user.dir") + "\\icons\\Play.png");
                pauseImageView.setFitHeight(20.0);
                pauseImageView.setFitWidth(20.0);
                playButton.setGraphic(pauseImageView);
                playerStatusLabel.setText("Пауза: " + currentTrack.getArtist() + " - " + currentTrack.getTitle());
            }
        });

        player.setOnStopped(new Runnable() {

            @Override
            public void run() {
                stopButton.setDisable(true);
                ImageView pauseImageView = new ImageView("file:///" + System.getProperty("user.dir") + "\\icons\\Play.png");
                pauseImageView.setFitHeight(20.0);
                pauseImageView.setFitWidth(20.0);
                playButton.setGraphic(pauseImageView);
                
            }
        });

        player.setOnEndOfMedia(new Runnable() {

            @Override
            public void run() {
                if(currentTrackIndex == -1) return;
                if (1 == MediaLibrary.getInstance().getTracksOfPlaylist(currentLibraryName, currentPlaylist).size()) {
                    player.seek(player.getStartTime());
                } else {
                    try {
                        VKTrack track = MediaLibrary.getInstance().nextTrack(currentLibraryName, currentPlaylist, currentTrack);
                        currentTrack = track;
                        ++currentTrackIndex;
                        if (currentTrackIndex == MediaLibrary.getInstance().getTracksOfPlaylist(currentLibraryName, currentPlaylist).size()) {
                            currentTrackIndex = 0;
                        }
                        if (libraryTree.getSelectionModel().getSelectedItem().getValue().equals(currentPlaylist)) {
                            playlist.getSelectionModel().select(currentTrackIndex);
                        }
                        playTrack(track);
                    } catch (IOException | URISyntaxException | ParseException ex) {
                        Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        player.currentTimeProperty().addListener(new ChangeListener<Duration>() {

            @Override
            public void changed(ObservableValue<? extends Duration> ov, Duration t, Duration t1) {
                progressSlider.setValue(t1.toSeconds());
                timeLabel.setText(timeFormat(t1, player.getTotalDuration()));
            }
        });
        return player;
    }
    
    private static String convertToTime(int minutes, int seconds){
        String minutesString = "";
        if(minutes < 10) minutesString = "0";
        minutesString += String.valueOf(minutes);
        String secondsString = "";
        if(seconds < 10) secondsString = "0";
        secondsString += String.valueOf(seconds);
        return minutesString + ":" + secondsString;
    }
    
    private static String timeFormat(Duration current, Duration total){
        int seconds = (int) current.toSeconds() - 60 * (int) current.toMinutes();
        int minutes = (int) current.toMinutes();
        String currentDuration = convertToTime(minutes, seconds);
        seconds = (int) total.toSeconds() - 60 * (int) total.toMinutes();
        minutes = (int) total.toMinutes();
        String totalDuration = convertToTime(minutes, seconds);
        return currentDuration + " / " + totalDuration;
    }
    
    private void startPlayingTrack(VKTrack track) throws IOException, MalformedURLException, URISyntaxException, ParseException {
        TreeItem<String> currentPlaylistItem = libraryTree.getSelectionModel().getSelectedItem();
        currentTrack = track;
        currentLibraryName = currentPlaylistItem.getParent().getValue();
        currentPlaylist = currentPlaylistItem.getValue();
        currentTrackIndex = playlist.getSelectionModel().getSelectedIndex();
        playTrack(track);
    }
    
    private void play(final VKTrack track) throws IOException, URISyntaxException, ParseException{
        if(downloadThread != null){
            downloadThread.interrupt();
        }
        downloadThread = new DownloadFileThread(track);
        downloadThread.addThreadFileDownloadFinishedEventListener(new ThreadDownloadFileFinishedEventListener() {

            @Override
            public void threadDownloadFileFinished(final ThreadDownloadFileFinishedEvent ev) {
                Platform.runLater(new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            if(player != null) player.dispose();
                            media = new Media(new File(ev.getDownloadedFilePath()).toURI().toURL().toExternalForm());
                            player = createMediaPlayer(media);
                            progressLabel.setVisible(false);
                            playerStatusLabel.setText("Сейчас играет: " + track.getArtist() + " - " + track.getTitle());
                            statusLabel.setText("");
                            player.play();
                        } catch (MalformedURLException ex) {
                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        });
        statusLabel.setText("Загрузка");
        progressLabel.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("progress.gif"))));
        progressLabel.setVisible(true);
        downloadThread.start();
    }
    
    private void playTrack(VKTrack track) throws MalformedURLException, IOException, URISyntaxException, ParseException {
        String playFileURI = new File(Utils.getDownloadableName(new URL(track.getUrl()))).toURI().toURL().toExternalForm();
        if (player != null) {
            if(!media.getSource().equals(playFileURI)){
                if (player.getStatus() == Status.PLAYING) player.stop();
                play(track);
            }
            else{
                if (player.getStatus() == Status.PAUSED || player.getStatus() == Status.STOPPED) player.play();
            }
        }
        else{
            play(track);
        }
    }
    
    @FXML
    private void stopButtonAction(ActionEvent event){
        player.stop();
        playerStatusLabel.setText("Остановлено: " + currentTrack.getArtist() + " - " + currentTrack.getTitle());
    }

    @FXML
    private void playButtonAction(ActionEvent event) {
        try {
            if (null != player) {
                if (Status.PLAYING == player.getStatus()) {
                    player.pause();
                } else {
                    if (null != currentTrack) {
                        playTrack(currentTrack);
                    }
                    else {
                        VKTrack track = (VKTrack) playlist.getSelectionModel().getSelectedItem();
                        if (null != track) {
                            startPlayingTrack(track);
                        }
                    }
                }
            } else {
                VKTrack track = (VKTrack) playlist.getSelectionModel().getSelectedItem();
                if (null != track) {
                    startPlayingTrack(track);
                }
            }
        } catch (Exception e) {
            Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Произошла ошибка воспроизведения. Проверьте соединение с Интернетом", "Ошибка воспроизведения", "Ошибка");
        }

    }
    
    @FXML
    private void searchButtonAction(ActionEvent event) {

        String title = titleSearchField.getText();
        if (title.equals("")) {
            Dialogs.showInformationDialog((Stage) searchButton.getScene().getWindow(), "Для выполнения поиска необходимо указать название альбома", "Информация", "Название альбома");
            return;
        }
        String artist = artistSearchField.getText();

        ReleaseSearchThread searchThread = new ReleaseSearchThread(title, artist);
        searchThread.addThreadReleaseSearchFinishedEventListener(new ThreadReleaseSearchFinishedEventListener() {

            @Override
            public void threadSearchFileFinished(final ThreadReleaseSearchFinishedEvent ev) {
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {
                        switch (ev.getExceptionCode()) {
                            case 1: Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Произошла ошибка парсинга XML. Попробуйте изменить запрос.", "Ошибка парсинга", "Ошибка");
                                break;
                            case 2: Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Невалидный URL запроса. Попробуйте изменить запрос.", "Ошибка URL", "Ошибка");
                                break;
                            case 3: Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Ошибка ввода-вывода. Попробуйте изменить запрос.", "Ошибка ввода-вывода", "Ошибка");
                                break;
                            case 4: Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Вызвано прерывание потока. Попробуйте перезапусть приложение.", "Ошибка потока", "Ошибка");
                                break;
                            case 5: Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Неверный формат полученный данных. Попробуйте изменить запрос.", "Ошибка получения данных", "Ошибка");
                                break;
                        }
                        progressLabel.setVisible(false);
                        statusLabel.setText("Поиск завершён");
                        if(ev.getReleaseTrackCount() == 0 || ev.getReleaseTracks() == null) {
                            resultLabel.setText("Ничего не найдено");
                            return;
                        }
                        resultLabel.setText("Композиций: " + String.valueOf(ev.getReleaseTrackCount())
                                + "   Загружено: " + String.valueOf(ev.getReleaseTracks().size()));
                        searchPlaylist.getItems().clear();
                        searchPlaylist.getItems().addAll(ev.getReleaseTracks());
                    }
                });
            }
        });
        statusLabel.setText("Поиск");
        progressLabel.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("progress.gif"))));
        progressLabel.setVisible(true);
        searchThread.start();

    }
    
    @FXML
    private void addToLibraryButtonAction(ActionEvent event) throws ParseException, URISyntaxException, IOException, InterruptedException{
        VKApi.getInstance().getTracksFromVKAlbums();
        if(!searchPlaylist.getItems().isEmpty()){
            String playlistName = Dialogs.showInputDialog((Stage)addToLibraryButton.getScene().getWindow(), "Введите имя нового альбома", "Добавление нового альбома", "Новый альбом");
            if(playlistName != null){
                MediaLibrary.getInstance().addTrackListToPlaylist("Library", playlistName, searchPlaylist.getItems());
                searchPlaylist.getItems().clear();
                titleSearchField.setText("");
                artistSearchField.setText("");
                resultLabel.setText("");
                tabPane.getSelectionModel().select(0);
            }
        }
        else{
            Dialogs.showInformationDialog((Stage) addToLibraryButton.getScene().getWindow(), "Вы пытаетесь добавить пустой альбом", "Попытка добавления пустого альбома", "Новый альбом");
        }
    }
    
    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        File access_token = new File("access_token.json");
        access_token.delete();
        if(player != null) player.dispose();
        MediaLibrary.getInstance().cleanTrackDirectory();
        logoutButton.getScene().getWindow().hide();
    }
    
    private void repaintTree(){
        TreeItem<String> rootItem = new TreeItem<>("My libraries");
        rootItem.setExpanded(true);
        for(String libraryName: MediaLibrary.getInstance().getLibrariesNames()){
            TreeItem<String> rootLibraryItem = new TreeItem<>(libraryName);
            rootLibraryItem.setExpanded(true);
            for (String playlistName : MediaLibrary.getInstance().getplaylistNames(libraryName)) {
                TreeItem<String> playlistItem = new TreeItem<>(playlistName);
                rootLibraryItem.getChildren().add(playlistItem);
            }
            rootItem.getChildren().add(rootLibraryItem);
        }
        libraryTree.setRoot(rootItem);
    }
    
    private void paintPlaylist() {
        if (0 == libraryTree.getSelectionModel().getSelectedIndex()) {
            return;
        }
        playlist.getItems().clear();
        TreeItem<String> selectedItem = libraryTree.getSelectionModel().getSelectedItem();
        if (null != selectedItem && !selectedItem.getValue().equals("Library") && !selectedItem.getValue().equals("VK Library")) {
            String playlistName = (String) selectedItem.getValue();
            playlist.getItems().addAll(MediaLibrary.getInstance().getTracksOfPlaylist(selectedItem.getParent().getValue(), playlistName));
            if (playlistName.equals(currentPlaylist)) {
                playlist.getSelectionModel().select(currentTrackIndex);
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusLabel.setText("Вас приветствует MusicLibrary");
        playerStatusLabel.setText("");
        repaintTree();

        MediaLibrary.getInstance().addAddPlaylistToLibraryListener(new PlaylistChangeListener() {

            @Override
            public void addPlaylistToLibraryDone(PlaylistChangeEvent event) {
                repaintTree();
            }

            @Override
            public void deletePlaylistFromLibraryDone(PlaylistChangeEvent event) {
                repaintTree();
            }
        });

        final ContextMenu cm = new ContextMenu();
        MenuItem cmItem1 = new MenuItem("Удалить");
        MenuItem cmItem2 = new MenuItem("Загрузить В Контакте");
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (libraryTree.getSelectionModel().getSelectedItem().getParent().getValue().equals("Library")) {
                    try {
                        String playlistName = libraryTree.getSelectionModel().getSelectedItem().getValue();
                        MediaLibrary.getInstance().deletePlaylist("Library", playlistName);
                    } catch (IOException ex) {
                        Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        });
        
        cmItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (libraryTree.getSelectionModel().getSelectedItem().getParent().getValue().equals("Library")) {
                    final String playlistName = libraryTree.getSelectionModel().getSelectedItem().getValue();
                    final List<VKTrack> tracks = MediaLibrary.getInstance().getTracksOfPlaylist("Library", playlistName);

                    Thread addThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                VKApi.getInstance().addNewAlbumToVKPlaylists(playlistName, tracks);
                                Platform.runLater(new Runnable() {
                                    
                                    @Override
                                    public void run() {
                                        try {
                                            MediaLibrary.getInstance().addTrackListToPlaylist("VK Library", playlistName, tracks);
                                            progressLabel.setVisible(false);
                                            statusLabel.setText("Альбом создан");
                                        } catch (IOException ex) {
                                            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                });
                            } catch (URISyntaxException | IOException | ParseException | InterruptedException ex) {
                                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    statusLabel.setText("Создание альбома");
                    progressLabel.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("progress.gif"))));
                    progressLabel.setVisible(true);
                    addThread.start();
                }
            }
        });

        cm.getItems().add(cmItem1);
        cm.getItems().add(cmItem2);

        playlist.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if (t.getButton().equals(MouseButton.PRIMARY)) {
                    if (2 == t.getClickCount()) {

                        VKTrack track = (VKTrack) playlist.getSelectionModel().getSelectedItem();
                        if (null != track) {
                            try {
                                startPlayingTrack(track);
                            } catch (IOException | URISyntaxException | ParseException ex) {
                                Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Произошла ошибка воспроизведения. Проверьте соединение с Интернетом", "Ошибка воспроизведения", "Ошибка");
                            }
                        }
                    }
                }
            }
        });

        searchPlaylist.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if (t.getButton().equals(MouseButton.PRIMARY)) {
                    if (2 == t.getClickCount()) {

                        VKTrack track = (VKTrack) searchPlaylist.getSelectionModel().getSelectedItem();
                        if (null != track) {
                            try {
                                currentTrackIndex = -1;
                                currentTrack = track;
                                playTrack(track);
                            } catch (IOException | URISyntaxException | ParseException ex) {
                                Dialogs.showErrorDialog((Stage) searchButton.getScene().getWindow(), "Произошла ошибка воспроизведения. Проверьте соединение с Интернетом", "Ошибка воспроизведения", "Ошибка");
                            }
                        }
                    }
                }
            }
        });

        libraryTree.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if (t.getButton().equals(MouseButton.PRIMARY)) {
                    if (1 == t.getClickCount()) {
                        cm.hide();
                        paintPlaylist();
                    }
                }

                if (t.getButton().equals(MouseButton.SECONDARY)) {
                    cm.show(libraryTree, t.getScreenX(), t.getScreenY());
                    paintPlaylist();
                }
            }
        });

        progressSlider.valueProperty().addListener(new InvalidationListener() {

            @Override
            public void invalidated(Observable o) {
                if (progressSlider.isValueChanging()) {
                    if (null != media && null != player) {
                        player.seek(Duration.seconds(progressSlider.getValue()));
                    }
                }
            }
        });
    }

}

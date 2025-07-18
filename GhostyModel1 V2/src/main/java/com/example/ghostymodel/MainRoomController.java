package com.example.ghostymodel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MainRoomController {

    @FXML
    private ImageView avatarImage;
    @FXML
    private ListView<Room> userCreatedRoomList;
    @FXML
    private Label firstText, fourthText, secondText, thirdText;


    private final ObservableList<Room> dynamicRooms = FXCollections.observableArrayList();

    @FXML
    private javafx.scene.control.Label welcomeLabel;



    public void initialize() {
        // Yeni bir thread başlatalım, çünkü jsoup ağ bağlantısı ana thread'i bloklamasın
        new Thread(() -> {
            try {
                String[] trends = getTopTrends();

                // JavaFX UI güncellemeleri Platform.runLater ile yapılır
                Platform.runLater(() -> {
                    firstText.setText(trends[0]);
                    secondText.setText(trends[1]);
                    thirdText.setText(trends[2]);
                    fourthText.setText(trends[3]);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        userCreatedRoomList.setItems(dynamicRooms);
        avatarImage.imageProperty().bind(UserData.getInstance().avatarImageProperty());
        AvatarUtil.setupAvatarClickHandler(avatarImage);

    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Avatar Seç");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Resimler", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(((ImageView) event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            Image newAvatar = new Image(selectedFile.toURI().toString());
            UserData.getInstance().setAvatarImage(newAvatar);
        }
    }


    private String[] getTopTrends() throws IOException {
        Document doc = Jsoup.connect("https://trends24.in/turkey/").get();
        Elements trendElements = doc.select(".trend-card__list li a");

        String[] trends = new String[5];
        for (int i = 0; i < 5; i++) {
            trends[i] = trendElements.get(i).text();
        }
        return trends;
    }

    public void setUsername(String username) {
        welcomeLabel.setText( username );
    }


    @FXML
    private void createNewRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Yeni Oda");
        dialog.setHeaderText(null);
        dialog.setContentText("Oda ismi gir:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(roomName -> {
            if (!roomName.trim().isEmpty()) {
                dynamicRooms.add(new Room(roomName, false));
            }
        });
    }

    @FXML
    private void deleteSelectedRoom() {
        Room selected = userCreatedRoomList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Oda Sil");
            alert.setHeaderText(null);
            alert.setContentText("'" + selected.getName() + "' odasını silmek istiyor musun?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dynamicRooms.remove(selected);
            }
        }
    }


    @FXML
    void lobbyClicked(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chatRoom.fxml"));
            Parent root = loader.load();

            // Controller'a erişiyo
            ChatRoomController chatRoomController = loader.getController();

            // Trend verilerini gönder chatRoomController için
            String[] trends = {
                    firstText.getText(),
                    secondText.getText(),
                    thirdText.getText(),
                    fourthText.getText()
            };
            chatRoomController.setTrends(trends);



            Label clickedLabel = (Label) event.getSource();
            chatRoomController.setLobbyText(clickedLabel.getText());

            // Kullanıcı ismini de gönderiyoruz
            chatRoomController.setUsername(welcomeLabel.getText());

            // Yeni sahneyi açıp
            Stage stage = new Stage();
            stage.setTitle("Ghosty");
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

            // Mevcut sahneyi kapatıyoz
            ((Stage)(((javafx.scene.Node) event.getSource()).getScene().getWindow())).close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}

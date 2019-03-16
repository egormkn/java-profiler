package io.github.egormkn.profiler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

public class Main extends Application {

    public static final String NAME = "Profiler";
    public static final String VERSION = "0.1.0";

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Locale.setDefault(Locale.US);
        String title = String.format("%s v%s", NAME, VERSION);
        URL layout = Main.class.getResource("/fxml/layout.fxml");
        Image icon = new Image(Main.class.getResourceAsStream("/icon.png"));
        Parent root = FXMLLoader.load(layout);
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.getIcons().add(icon);
        stage.setIconified(false);
        stage.show();
    }
}

package fsa.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FsaEditorApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 640, 480);

        stage.setTitle("Edytor grafów automatów skończonych");
        stage.setScene(scene);
        stage.show();
    }
}

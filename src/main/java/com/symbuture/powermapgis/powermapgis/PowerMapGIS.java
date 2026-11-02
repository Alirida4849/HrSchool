package com.symbuture.powermapgis.powermapgis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PowerMapGIS extends Application {

 
  @Override
public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML.fxml"));
    stage.getIcons().add(
    new Image(getClass().getResourceAsStream("/images/logo.png"))
);
    Scene scene = new Scene(loader.load());
    
    stage.setTitle("HrSchool");
    stage.setScene(scene);
    stage.show();
}

    public static void main(String[] args) {
        launch(args);
    }
}
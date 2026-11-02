package com.uacam.minidbeaver.mini_dbeaver;

import com.uacam.minidbeaver.mini_dbeaver.model.RemotoDbServicio;
import com.uacam.minidbeaver.mini_dbeaver.model.SSHTunnelServicio;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Aplicacion extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Aplicacion.class.getResource("/com/uacam/minidbeaver/mini_dbeaver/LoginVista.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Mini_DBeaver - Conexión");
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(560);
        stage.show();
    }
    @Override
    public void stop() {
        RemotoDbServicio.getInstance().disconnect();
        SSHTunnelServicio.getInstance().disconnect();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
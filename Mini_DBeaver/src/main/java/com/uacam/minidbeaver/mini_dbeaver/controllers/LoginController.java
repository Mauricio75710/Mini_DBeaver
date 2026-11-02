package com.uacam.minidbeaver.mini_dbeaver.controllers;

import com.uacam.minidbeaver.mini_dbeaver.model.RemotoDbServicio;
import com.uacam.minidbeaver.mini_dbeaver.model.SSHTunnelServicio;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {
    @FXML private TextField hostField;
    @FXML private TextField userField;
    @FXML private PasswordField passwordField;
    @FXML private TextField dbUserField;
    @FXML private PasswordField dbPasswordField;
    @FXML private TextField databaseField;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @FXML
    private void initialize() {
        databaseField.setText("its5a");
        dbUserField.setText("becario");
    }
    @FXML
    private void conectar() {
        String host = hostField.getText().trim();
        String sshUser = userField.getText().trim();
        String sshPassword = passwordField.getText();
        String dbUser = dbUserField.getText().trim();
        String dbPassword = dbPasswordField.getText();
        String database = databaseField.getText().trim();

        if (host.isEmpty()
                || sshUser.isEmpty()
                || sshPassword.isEmpty()
                || dbUser.isEmpty()
                || dbPassword.isEmpty()
                || database.isEmpty()) {
            statusLabel.setText("Completa todos los campos.");
            return;
        }
        connectButton.setDisable(true);
        statusLabel.setText("Conectando...");
        executor.submit(() -> {
            try {
                int port = SSHTunnelServicio.getInstance().connect(host, sshUser, sshPassword);
                RemotoDbServicio.getInstance().connect(port, dbUser, dbPassword, database);
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                        "/com/uacam/minidbeaver/mini_dbeaver/TablasVista.fxml"));
                        Scene scene = new Scene(loader.load());
                        TablasController controller =
                                loader.getController();
                        controller.setDatabaseName(database);
                        Stage stage = (Stage) connectButton
                                .getScene().getWindow();
                        stage.setTitle("Mini_DBeaver - Tablas");
                        stage.setScene(scene);
                        stage.setMinWidth(750);
                        stage.setMinHeight(500);
                    } catch (Exception e) {
                        statusLabel.setText(
                                "No se pudo abrir la ventana: "
                                        + e.getMessage()
                        );
                        connectButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                SSHTunnelServicio.getInstance().disconnect();
                RemotoDbServicio.getInstance().disconnect();
                Platform.runLater(() -> {
                    statusLabel.setText(
                            "Error de conexión: " + e.getMessage()
                    );
                    connectButton.setDisable(false);
                });
            }
        });
    }
}
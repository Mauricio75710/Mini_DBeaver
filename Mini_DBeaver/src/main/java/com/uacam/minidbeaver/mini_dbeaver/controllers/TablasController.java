package com.uacam.minidbeaver.mini_dbeaver.controllers;

import com.uacam.minidbeaver.mini_dbeaver.model.LocalDbServicio;
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

public class TablasController {
    @FXML private ListView<String> tablesList;
    @FXML private Label statusLabel;
    @FXML private Button downloadButton;

    private String databaseName;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        cargarTablas();
    }
    @FXML
    private void initialize() {
        tablesList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) ->
                        downloadButton.setDisable(newValue == null)
                );
        downloadButton.setDisable(true);
    }
    @FXML
    private void cargarTablas() {
        statusLabel.setText("Consultando tablas");
        executor.submit(() -> {

            try {
                var tables = RemotoDbServicio.getInstance().getTables();
                Platform.runLater(() -> {
                    tablesList.getItems().setAll(tables);
                    statusLabel.setText("Base de datos: " + databaseName + " | Tablas: " + tables.size());
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
            }
        });
    }
    @FXML
    private void descargarTabla() {
        String selected = tablesList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }
        downloadButton.setDisable(true);
        statusLabel.setText("Comprobando tabla...");
        executor.submit(() -> {

            try {
                LocalDbServicio localDb = LocalDbServicio.getInstance();

                if (localDb.tableExists(selected)) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Abriendo copia local de " + selected);
                        downloadButton.setDisable(false);
                        abrirCrud(selected);
                    });
                } else {
                    localDb.replicateTable(RemotoDbServicio.getInstance().getConnection(), selected);
                    Platform.runLater(() -> {
                        statusLabel.setText("Tabla descargada correctamente.");
                        downloadButton.setDisable(false);
                        abrirCrud(selected);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    downloadButton.setDisable(false);
                });
            }
        });
    }
    private void abrirCrud(String tableName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uacam/minidbeaver/mini_dbeaver/CrudVista.fxml"));
            Scene scene = new Scene(loader.load());
            CrudController controller =
                    loader.getController();
            controller.setTableName(tableName);

            Stage stage = (Stage) tablesList.getScene().getWindow();
            stage.setTitle("Mini_DBeaver - SQLite - " + tableName
            );
            stage.setScene(scene);
        } catch (Exception e) {statusLabel.setText("No se pudo abrir CRUD: " + e.getMessage());
        }
    }
    @FXML
    private void desconectar() {
        executor.submit(() -> {
            RemotoDbServicio.getInstance().disconnect();
            SSHTunnelServicio.getInstance().disconnect();
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uacam/minidbeaver/mini_dbeaver/LoginVista.fxml"));
                    Scene scene = new Scene(loader.load());
                    Stage stage = (Stage) tablesList.getScene().getWindow();
                    stage.setTitle("Mini_DBeaver - Conexión");
                    stage.setScene(scene);
                } catch (Exception e) {
                    statusLabel.setText("Error al volver: " + e.getMessage());
                }
            });
        });
    }
}
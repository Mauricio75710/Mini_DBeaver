package com.uacam.minidbeaver.mini_dbeaver.controllers;

import com.uacam.minidbeaver.mini_dbeaver.model.LocalDbServicio;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Map;

public class CrudController {
    @FXML private Label tableLabel;
    @FXML private TableView<Map<String, Object>> dataTable;
    @FXML private TextArea sqlArea;
    @FXML private Label statusLabel;

    private String tableName;

    public void setTableName(String tableName) {
        this.tableName = tableName;
        tableLabel.setText("Tabla local: " + tableName + " (SQLite)"
        );
        sqlArea.setText("SELECT * FROM \"" + tableName.replace("\"", "\"\"") + "\";"
        );
        refreshTable();
    }

    @FXML
    private void initialize() {
        dataTable.setEditable(false);
    }

    @FXML
    private void refreshTable() {
        if (tableName == null) {
            return;
        }
        try {
            var result = LocalDbServicio.getInstance().executeQuery("SELECT * FROM " + LocalDbServicio.quote(tableName));
            mostrarResultados(result);
            statusLabel.setText("Registros mostrados: " + result.rows().size());
        } catch (Exception e) {
            statusLabel.setText("Error al consultar: " + e.getMessage());
        }
    }
    private void mostrarResultados(
            LocalDbServicio.QueryResult result) {
        dataTable.getColumns().clear();
        for (String columnName : result.columns()) {
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cell ->
                    new ReadOnlyObjectWrapper<>(cell.getValue().get(columnName)));
            column.setPrefWidth(150);
            dataTable.getColumns().add(column);
        }
        dataTable.getItems().setAll(result.rows());
    }
    @FXML
    private void ejecutarSql() {
        String sql = sqlArea.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("Escribe una instrucción SQL.");
            return;
        }
        try {
            String tipo = sql.stripLeading().toUpperCase();
            if (tipo.startsWith("SELECT")
                    || tipo.startsWith("PRAGMA")
                    || tipo.startsWith("WITH")) {
                var result = LocalDbServicio.getInstance().executeQuery(sql);
                mostrarResultados(result);
                statusLabel.setText("Consulta realizada. Filas: " + result.rows().size());
            } else {
                int affected = LocalDbServicio.getInstance().executeUpdate(sql);
                statusLabel.setText("Operación realizada. Filas afectadas: " + affected);
                refreshTable();
            }
        } catch (Exception e) {
            statusLabel.setText("Error SQL: " + e.getMessage());
        }
    }
    @FXML
    private void volverTablas() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uacam/minidbeaver/mini_dbeaver/TablasVista.fxml"));
            Scene scene = new Scene(loader.load());
            TablasController controller = loader.getController();
            controller.setDatabaseName("its5a");
            Stage stage = (Stage) dataTable.getScene().getWindow();
            stage.setTitle("Mini_DBeaver - Tablas");
            stage.setScene(scene);

        } catch (Exception e) {
            statusLabel.setText("No se pudo volver: " + e.getMessage());
        }
    }
}
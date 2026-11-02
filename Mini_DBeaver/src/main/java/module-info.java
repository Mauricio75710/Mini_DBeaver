module com.uacam.minidbeaver.mini_dbeaver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.jcraft.jsch;
    requires org.mariadb.jdbc;
    requires org.xerial.sqlitejdbc;

    opens com.uacam.minidbeaver.mini_dbeaver to javafx.fxml;
    opens com.uacam.minidbeaver.mini_dbeaver.controllers to javafx.fxml;

    exports com.uacam.minidbeaver.mini_dbeaver;
    exports com.uacam.minidbeaver.mini_dbeaver.controllers;
    exports com.uacam.minidbeaver.mini_dbeaver.model;
}
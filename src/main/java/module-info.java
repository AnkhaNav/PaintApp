module com.example.paintapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;

    opens com.example.paintapp to javafx.fxml;
    exports com.example.paintapp;
}
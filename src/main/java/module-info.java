module fsa {
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    exports fsa;
    exports fsa.gui;
    opens fsa to javafx.fxml;
    opens fsa.gui to javafx.fxml;
}
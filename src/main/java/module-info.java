module com.example.chess_project_p2p_hybrid {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chess_project_p2p_hybrid to javafx.fxml;
    exports com.example.chess_project_p2p_hybrid;
}
module com.example.chess_project_p2p_hybrid {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.example.chess_project_p2p_hybrid.client.controller to javafx.fxml;
    opens com.example.chess_project_p2p_hybrid.client.connection to com.google.gson;
    opens com.example.chess_project_p2p_hybrid.client.model.game to com.google.gson;
    opens com.example.chess_project_p2p_hybrid.client.model.board to com.google.gson;
    opens com.example.chess_project_p2p_hybrid.client.model.piece to com.google.gson;
    opens com.example.chess_project_p2p_hybrid.server to com.google.gson;

    exports com.example.chess_project_p2p_hybrid;
}
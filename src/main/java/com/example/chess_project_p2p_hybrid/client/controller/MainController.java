package com.example.chess_project_p2p_hybrid.client.controller;


import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.ResourceBundle;


public class MainController implements Initializable {

    @FXML
    private GridPane chessBoard;

    private static final String LIGHT_COLOR = "#f0d9b5";
    private static final String DARK_COLOR = "#b58863";

    // White
    private static final String WK = "\u2654";
    private static final String WQ = "\u2655";
    private static final String WR = "\u2656";
    private static final String WB = "\u2657";
    private static final String WN = "\u2658";
    private static final String WP = "\u2659";

    // Black
    private static final String BK = "\u265A";
    private static final String BQ = "\u265B";
    private static final String BR = "\u265C";
    private static final String BB = "\u265D";
    private static final String BN = "\u265E";
    private static final String BP = "\u265F";

    // --- Biến cho chức năng Kéo thả (Drag and Drop) ---
    private double dragDeltaX;
    private double dragDeltaY;
    private Label draggedPiece;


    private Integer startCol;
    private Integer startRow;


    /**
     * Phương thức được gọi sau khi tất cả các phần tử FXML đã được tải và tiêm.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        createAndColorChessBoard();

        setupInitialPieces();
    }

    /**
     * Tạo 64 ô cờ (Pane) và thêm chúng vào GridPane với màu sắc xen kẽ.
     */
    private void createAndColorChessBoard() {

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                boolean isLightSquare = (row + col) % 2 == 0;
                String color = isLightSquare ? LIGHT_COLOR : DARK_COLOR;

                Pane square = new Pane();
                square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                square.setStyle("-fx-background-color: " + color + ";");


                chessBoard.add(square, col, row);
            }
        }
    }

    /**
     * Thiết lập vị trí ban đầu của 32 quân cờ theo quy tắc cờ vua.
     */
    private void setupInitialPieces() {

        String[] backRankBlack = {BR, BN, BB, BQ, BK, BB, BN, BR};
        for (int col = 0; col < 8; col++) {
            placePiece(backRankBlack[col], 0, col);
            placePiece(BP, 1, col);
        }


        String[] backRankWhite = {WR, WN, WB, WQ, WK, WB, WN, WR};
        for (int col = 0; col < 8; col++) {
            placePiece(WP, 6, col);
            placePiece(backRankWhite[col], 7, col);
        }
    }

    /**
     * Tạo một Label cho quân cờ và đặt nó vào ô (row, col) trên bàn cờ,
     * đồng thời gắn các sự kiện kéo thả.
     *
     * @param pieceChar Ký tự Unicode của quân cờ.
     * @param row       Hàng (0-7, 0 là trên cùng/hàng 8).
     * @param col       Cột (0-7, 0 là cột A).
     */
    private void placePiece(String pieceChar, int row, int col) {
        Label pieceLabel = new Label(pieceChar);

        pieceLabel.setFont(new Font(50));

        pieceLabel.setAlignment(Pos.CENTER);

        pieceLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        pieceLabel.setOnMousePressed(this::handleMousePressed);
        pieceLabel.setOnMouseDragged(this::handleMouseDragged);
        pieceLabel.setOnMouseReleased(this::handleMouseReleased);

        pieceLabel.setStyle("-fx-opacity: 1.0;");

        GridPane.setConstraints(pieceLabel, col, row);
        chessBoard.getChildren().add(pieceLabel);
    }

    // --- Các phương thức xử lý sự kiện Kéo thả (Drag and Drop) ---

    /**
     * Xử lý sự kiện nhấn chuột để bắt đầu kéo.
     * 1. Lưu quân cờ đang được kéo.
     * 2. Lưu vị trí ban đầu (offset) của chuột so với góc trên bên trái của quân cờ.
     */
    private void handleMousePressed(MouseEvent event) {
        draggedPiece = (Label) event.getSource();

        // Lưu tọa độ ô cờ ban đầu
        startCol = GridPane.getColumnIndex(draggedPiece);
        startRow = GridPane.getRowIndex(draggedPiece);

        // Tính offset: Khoảng cách từ góc trên trái của quân cờ đến con trỏ chuột.
        // Điều này giúp tránh quân cờ nhảy khi bắt đầu kéo.
        dragDeltaX = draggedPiece.getLayoutX() - event.getSceneX();
        dragDeltaY = draggedPiece.getLayoutY() - event.getSceneY();

        // Đặt quân cờ đang kéo lên trên cùng (Layering: Z-index)
        draggedPiece.toFront();

        // Làm mờ nhẹ và phóng to khi bắt đầu kéo (tùy chọn)
        draggedPiece.setStyle("-fx-opacity: 0.7; -fx-scale-x: 1.1; -fx-scale-y: 1.1;");
    }

    /**
     * Xử lý sự kiện kéo chuột (di chuyển quân cờ theo chuột).
     * 1. Cập nhật vị trí Layout X, Y của quân cờ.
     */
    private void handleMouseDragged(MouseEvent event) {
        if (draggedPiece == null) return;


        draggedPiece.setLayoutX(event.getSceneX() + dragDeltaX);
        draggedPiece.setLayoutY(event.getSceneY() + dragDeltaY);
    }

    /**
     * Xử lý sự kiện nhả chuột (kết thúc kéo).
     * 1. Xác định ô cờ mới (target cell).
     * 2. (TODO: Logic kiểm tra nước đi hợp lệ sẽ nằm ở đây).
     * 3. Đặt quân cờ vào ô mới.
     * 4. Đặt lại trạng thái hiển thị của quân cờ.
     */
    private void handleMouseReleased(MouseEvent event) {
        if (draggedPiece == null) return;

        // 1. Xác định vị trí thả (trong hệ tọa độ của GridPane)
        double sceneX = event.getSceneX();
        double sceneY = event.getSceneY();

        // Chuyển đổi tọa độ màn hình sang tọa độ cục bộ của GridPane
        javafx.geometry.Point2D localPoint = chessBoard.sceneToLocal(sceneX, sceneY);

        // Tính toán cột và hàng mới
        double cellWidth = chessBoard.getWidth() / 8.0;
        double cellHeight = chessBoard.getHeight() / 8.0;

        int targetCol = (int) Math.floor(localPoint.getX() / cellWidth);
        int targetRow = (int) Math.floor(localPoint.getY() / cellHeight);

        // Đảm bảo targetCol/Row nằm trong giới hạn 0-7
        targetCol = Math.max(0, Math.min(7, targetCol));
        targetRow = Math.max(0, Math.min(7, targetRow));

        // 2. TODO: LOGIC KIỂM TRA NƯỚC ĐI CỜ VUA (Hiện tại chỉ di chuyển bất chấp)
        boolean isMoveValid = true; // Giả định nước đi luôn hợp lệ để thấy hiệu ứng kéo thả

        if (isMoveValid) {
            // Xóa quân cờ khỏi vị trí cũ
            chessBoard.getChildren().remove(draggedPiece);

            // Cập nhật ràng buộc (constraints) và thêm lại vào vị trí mới
            GridPane.setConstraints(draggedPiece, targetCol, targetRow);
            chessBoard.getChildren().add(draggedPiece);
        }

        // 3. Đặt lại trạng thái hiển thị và vị trí (layout)
        draggedPiece.setStyle("-fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        draggedPiece.setLayoutX(0);
        draggedPiece.setLayoutY(0);

        draggedPiece = null;
        startCol = null;
        startRow = null;
    }

}

package com.example.chess_project_p2p_hybrid.client.controller;


import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.example.chess_project_p2p_hybrid.client.handler.GameMessageHandler;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Game;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;
import com.example.chess_project_p2p_hybrid.client.model.game.GameResult;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.model.piece.Piece;
import com.example.chess_project_p2p_hybrid.client.model.piece.PieceType;
import com.example.chess_project_p2p_hybrid.client.sync.GameSyncManager;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import com.example.chess_project_p2p_hybrid.client.util.SceneNavigator;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.List;
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

    // --- Model game và lịch sử ---
    private Game game;
    private GameSyncManager syncManager;
    private GameMessageHandler gameMessageHandler;

    // Trạng thái chọn và gợi ý nước đi
    private Position selectedFrom;
    private List<Move> currentLegalMoves = java.util.Collections.emptyList();

    @FXML
    private ListView<String> moveListView;
    private final ObservableList<String> moveItems = FXCollections.observableArrayList();

    // Sidebar buttons & right panels
    @FXML private Button homeButton;
    @FXML private Button pvpButton;
    @FXML private Button pvcButton;
    @FXML private Button chatButton;
    @FXML private Button howToPlayButton;
    @FXML private Button exitButton;
    @FXML private Button resignButton;
    @FXML private Button newGameButton;
    @FXML private Button quickMatchButton;
    @FXML private Button joinRoomButton;
    @FXML private Button createRoomButton;
    @FXML private Button undoButton;
    @FXML private Label roomIdLabel;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private Label lNameWhite;
    @FXML private Label lNameBlack;
    @FXML private Label tipLabel;
    @FXML private TextField joinRoomField;
    @FXML private VBox homePanelView;
    @FXML private VBox gamePanelView;

    private final ClientSession session = ClientSession.getInstance();


    /**
     * Phương thức được gọi sau khi tất cả các phần tử FXML đã được tải và tiêm.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        session.setMainController(this);
        game = new Game();
        syncManager = new GameSyncManager(game, session);
        syncManager.setStatusCallback(this::setTip);
        
        // Setup game message handler
        gameMessageHandler = new GameMessageHandler(syncManager, session);
        gameMessageHandler.setStatusCallback(this::setTip);
        gameMessageHandler.setOnGameStart(() -> {
            renderBoardFromModel();
            updateStatusLabels();
            clearHighlights();
        });
        gameMessageHandler.setOnMoveReceived(() -> {
            renderBoardFromModel();
            updateStatusLabels();
            rebuildHistoryFromGame();
            clearHighlights();
        });
        gameMessageHandler.setOnGameViewSwitch(() -> {
            if (session.getRoomId() != null) {
                startGameView(session.getRoomId());
            }
        });
        
        createAndColorChessBoard();
        if (moveListView != null) {
            moveListView.setItems(moveItems);
        }
        renderBoardFromModel();
        if (tipLabel != null) {
            tipLabel.setText("Tip: Nhấp vào quân để xem nước đi hợp lệ. Kéo hoặc nhấp vào ô để di chuyển.");
        }

        // wire sidebar actions
        if (homeButton != null) homeButton.setOnAction(e -> showHome());
        if (pvpButton != null) pvpButton.setOnAction(e -> startGameView("[PvP Local]") );
        if (pvcButton != null) pvcButton.setOnAction(e -> startGameView("[PvC]") );
        if (chatButton != null) chatButton.setOnAction(e -> SceneNavigator.showChatWindow());
        if (howToPlayButton != null) howToPlayButton.setOnAction(e -> setTip("Luật cờ vua: nhập thành, en passant, phong cấp..."));
        if (exitButton != null) exitButton.setOnAction(e -> javafx.application.Platform.exit());
        if (resignButton != null) resignButton.setOnAction(e -> handleResign());
        if (newGameButton != null) newGameButton.setOnAction(e -> resetGame(true));
        if (undoButton != null) undoButton.setOnAction(e -> handleUndo());
        if (quickMatchButton != null) quickMatchButton.setOnAction(e -> handleQuickMatch());
        if (joinRoomButton != null) joinRoomButton.setOnAction(e -> handleJoinRoom());
        if (createRoomButton != null) createRoomButton.setOnAction(e -> handleCreateRoom());

        // Luôn setup handler nếu đã connected (ngay cả khi chưa có room)
        if (session.isConnected() && session.getPeer() != null) {
            // Set handler cho peer để nhận messages
            session.getPeer().setHandler(gameMessageHandler);
            session.setMessageHandler(gameMessageHandler);
            
            // Update UI nếu đã có room
            if (session.getRoomId() != null && !session.getRoomId().isBlank()) {
                if (roomIdLabel != null) roomIdLabel.setText(session.getRoomId());
                applyPlayerNames();
            }
        }
        if (roomIdLabel != null && (session.getRoomId() == null || session.getRoomId().isBlank())) {
            roomIdLabel.setText("[No Room]");
        }

        updateStatusLabels();
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

                final int r = row;
                final int c = col;
                square.setOnMouseClicked(e -> handleSquareClicked(r, c));


                chessBoard.add(square, col, row);
            }
        }
    }

    private void renderBoardFromModel() {
        // Xóa toàn bộ quân (Label) hiện tại, giữ lại các Pane ô nền
        chessBoard.getChildren().removeIf(node -> node instanceof Label);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = game.getBoard().getPiece(Position.of(r, c));
                if (piece != null) {
                    String glyph = mapPieceToUnicode(piece.getColor(), piece.getType());
                    placePiece(glyph, r, c);
                }
            }
        }
        if (selectedFrom == null) {
            highlightCheckIndicator();
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
        final int r = row;
        final int c = col;
        pieceLabel.setOnMouseClicked(e -> handlePieceClicked(r, c));

        pieceLabel.setStyle("-fx-opacity: 1.0;");

        GridPane.setConstraints(pieceLabel, col, row);
        chessBoard.getChildren().add(pieceLabel);
    }

    private String mapPieceToUnicode(Color color, PieceType type) {
        return switch (type) {
            case KING -> color == Color.WHITE ? WK : BK;
            case QUEEN -> color == Color.WHITE ? WQ : BQ;
            case ROOK -> color == Color.WHITE ? WR : BR;
            case BISHOP -> color == Color.WHITE ? WB : BB;
            case KNIGHT -> color == Color.WHITE ? WN : BN;
            case PAWN -> color == Color.WHITE ? WP : BP;
        };
    }

    // --- Các phương thức xử lý sự kiện Kéo thả (Drag and Drop) ---

    /**
     * Xử lý sự kiện nhấn chuột để bắt đầu kéo.
     * 1. Lưu quân cờ đang được kéo.
     * 2. Lưu vị trí ban đầu (offset) của chuột so với góc trên bên trái của quân cờ.
     */
    private void handleMousePressed(MouseEvent event) {
        // Kiểm tra game đã kết thúc chưa
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Ván cờ đã kết thúc. Không thể di chuyển quân cờ.");
            return;
        }
        draggedPiece = (Label) event.getSource();

        // Lưu tọa độ ô cờ ban đầu
        startCol = GridPane.getColumnIndex(draggedPiece);
        startRow = GridPane.getRowIndex(draggedPiece);

        // Chặn kéo nếu không phải lượt quân này
        if (startCol != null && startRow != null) {
            Piece piece = game.getBoard().getPiece(Position.of(startRow, startCol));
            if (piece == null || piece.getColor() != game.getTurn()) {
                draggedPiece = null;
                return;
            }

            // Tính và highlight các nước đi hợp lệ cho quân được chọn
            selectedFrom = Position.of(startRow, startCol);
            currentLegalMoves = game.legalMovesFor(selectedFrom);
            highlightMoves(selectedFrom, currentLegalMoves);
        }

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
        // Kiểm tra game đã kết thúc chưa
        if (game.getResult() != GameResult.ONGOING) {
            return;
        }


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
        
        // Kiểm tra game đã kết thúc chưa
        if (game.getResult() != GameResult.ONGOING) {
            draggedPiece.setStyle("-fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            draggedPiece.setLayoutX(0);
            draggedPiece.setLayoutY(0);
            draggedPiece = null;
            startCol = null;
            startRow = null;
            setTip("Ván cờ đã kết thúc. Không thể di chuyển quân cờ.");
            return;
        }
        
        // Kiểm tra nếu không phải lượt của mình (trong P2P mode)
        if (session.isConnected() && session.getPlayerColor() != game.getTurn()) {
            draggedPiece.setStyle("-fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            draggedPiece.setLayoutX(0);
            draggedPiece.setLayoutY(0);
            draggedPiece = null;
            startCol = null;
            startRow = null;
            Color currentTurn = game.getTurn();
            String message = "Chưa đến lượt của bạn! Lượt hiện tại: " + 
                (currentTurn == Color.WHITE ? "Trắng" : "Đen");
            setTip(message);
            return;
        }

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

        // 2. Kiểm tra hợp lệ theo model Game và áp dụng nếu hợp lệ
        if (startCol != null && startRow != null) {
            Position from = Position.of(startRow, startCol);
            Position to = Position.of(targetRow, targetCol);

            List<Move> legalMoves = (selectedFrom != null && selectedFrom.equals(from)) ? currentLegalMoves : game.legalMovesFor(from);
            Move chosen = pickMoveTo(legalMoves, to);
            if (chosen != null) {
                System.out.println("[MainController] ===== Applying local move =====");
                System.out.println("[MainController] Move: " + chosen);
                System.out.println("[MainController] Turn BEFORE: " + game.getTurn());
                System.out.println("[MainController] My color: " + session.getPlayerColor());
                
                if (syncManager.applyLocalMove(chosen)) {
                    System.out.println("[MainController] ✓ Local move applied successfully");
                    System.out.println("[MainController] Turn AFTER: " + game.getTurn());
                    appendHistory(chosen);
                    renderBoardFromModel();
                    updateStatusLabels();
                    // Gửi move sau khi đã apply thành công
                    sendMoveToPeer(chosen);
                    selectedFrom = null;
                    currentLegalMoves = java.util.Collections.emptyList();
                    clearHighlights();
                    System.out.println("[MainController] ===== Local move completed =====");
                } else {
                    System.out.println("[MainController] ✗ Failed to apply local move: " + chosen);
                    setTip("Nước đi không hợp lệ hoặc chưa đến lượt của bạn!");
                }
            }
        }

        // 3. Đặt lại trạng thái hiển thị và vị trí (layout)
        draggedPiece.setStyle("-fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        draggedPiece.setLayoutX(0);
        draggedPiece.setLayoutY(0);

        draggedPiece = null;
        startCol = null;
        startRow = null;
    }

    private Move pickMoveTo(List<Move> legalMoves, Position to) {
        // Lọc tất cả move đến ô to
        List<Move> candidates = new java.util.ArrayList<>();
        for (Move m : legalMoves) if (m.getTo().equals(to)) candidates.add(m);
        if (candidates.isEmpty()) return null;

        // Nếu không có promotion hoặc chỉ có 1 lựa chọn, trả luôn
        boolean anyPromotion = candidates.stream().anyMatch(m -> m.getPromotionTo() != null);
        if (!anyPromotion || candidates.size() == 1) return candidates.get(0);

        // Có nhiều lựa chọn phong cấp: mở dialog cho user chọn
        List<PieceType> options = java.util.List.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT);
        ChoiceDialog<PieceType> dialog = new ChoiceDialog<>(PieceType.QUEEN, options);
        dialog.setTitle("Promotion");
        dialog.setHeaderText("Chọn quân để phong cấp");
        dialog.setContentText("Phong thành:");
        var result = dialog.showAndWait();
        if (result.isEmpty()) return null;
        PieceType chosenType = result.get();
        for (Move m : candidates) {
            if (chosenType.equals(m.getPromotionTo())) return m;
        }
        // fallback: nếu không tìm thấy đúng type, chọn hậu
        for (Move m : candidates) if (m.getPromotionTo() == PieceType.QUEEN) return m;
        return candidates.get(0);
    }

    private void appendHistory(Move m) {
        if (moveListView == null) return;
        moveItems.add(moveItems.size() + 1 + ". " + m);
        moveListView.scrollTo(moveItems.size() - 1);
    }

    private void showHome() {
        if (homePanelView != null && gamePanelView != null) {
            homePanelView.setVisible(true);
            homePanelView.setManaged(true);
            gamePanelView.setVisible(false);
            gamePanelView.setManaged(false);
        }
    }

    private void startGameView(String roomId) {
        if (roomIdLabel != null) roomIdLabel.setText(roomId);
        if (homePanelView != null && gamePanelView != null) {
            homePanelView.setVisible(false);
            homePanelView.setManaged(false);
            gamePanelView.setVisible(true);
            gamePanelView.setManaged(true);
        }
        resetGame(false);
    }

    private void resetGame(boolean broadcast) {
        syncManager.syncGameState();
        moveItems.clear();
        renderBoardFromModel();
        updateStatusLabels();
        clearHighlights();
        if (broadcast && session.isConnected()) {
            // Gửi yêu cầu ván mới cho đối thủ
            requestNewGame();
        }
    }
    
    private void requestNewGame() {
        if (!session.isConnected()) {
            // Local game - reset ngay
            syncManager.syncGameState();
            moveItems.clear();
            renderBoardFromModel();
            updateStatusLabels();
            clearHighlights();
            return;
        }
        
        // P2P game - gửi request
        session.setNewGameRequestPending(true);
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "new_game_request");
        payload.addProperty("from", session.getPlayerName());
        sendSystemCommand(payload);
        setTip("Đã gửi yêu cầu ván mới. Chờ đối thủ đồng ý...");
    }
    

    private void handleResign() {
        // Kiểm tra điều kiện
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Ván cờ đã kết thúc, không thể đầu hàng.");
            return;
        }
        
        if (!session.isConnected()) {
            // Local game - chỉ cần set result
            game.setResult(session.getPlayerColor() == Color.WHITE ? 
                GameResult.CHECKMATE_BLACK : GameResult.CHECKMATE_WHITE);
            moveItems.add("[Resign - " + session.getPlayerName() + "]");
            setTip("Bạn đã đầu hàng.");
            updateStatusLabels();
            renderBoardFromModel();
            return;
        }
        
        // P2P game - gửi resign signal
        // Set result cho bên thua (chính mình)
        Color winner = session.getPlayerColor().opposite();
        game.setResult(winner == Color.WHITE ? 
            GameResult.CHECKMATE_WHITE : GameResult.CHECKMATE_BLACK);
        
        moveItems.add("[Resign - " + session.getPlayerName() + "]");
        setTip("Bạn đã đầu hàng. " + session.getOpponentName() + " thắng!");
        updateStatusLabels();
        renderBoardFromModel();
        
        // Gửi resign signal cho đối thủ
        sendSystemEvent("resign");
    }

    private void handleUndo() {
        // Kiểm tra điều kiện
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Không thể undo khi ván cờ đã kết thúc.");
            return;
        }
        
        if (game.getHistory().isEmpty()) {
            setTip("Không còn nước để undo.");
            return;
        }
        
        // Kiểm tra trong P2P mode: chỉ undo được khi có ít nhất 2 moves (1 move của mình + 1 move của đối thủ)
        // Hoặc cho phép undo ngay cả khi chỉ có 1 move (nếu là move của mình)
        if (session.isConnected() && game.getHistory().size() < 1) {
            setTip("Chưa có nước đi nào để undo.");
            return;
        }
        
        // Undo local
        if (game.undoLastMove()) {
            System.out.println("[MainController] Undo successful - History size: " + game.getHistory().size());
            rebuildHistoryFromGame();
            renderBoardFromModel();
            updateStatusLabels();
            clearHighlights();
            
            // Trong P2P mode, gửi signal cho đối thủ để đồng bộ
            if (session.isConnected()) {
                sendSystemEvent("undo");
                setTip("Đã undo nước đi. Đang chờ đối thủ đồng bộ...");
            } else {
                setTip("Đã undo nước đi.");
            }
        } else {
            setTip("Không thể undo nước đi này.");
        }
    }

    private void handleCreateRoom() {
        if (!ensureConnected()) return;
        setTip("Đang tạo phòng mới...");
        sendSystemEvent("create_room");
    }

    private void handleJoinRoom() {
        if (!ensureConnected()) return;
        if (joinRoomField == null) return;
        String roomId = joinRoomField.getText();
        if (roomId == null || roomId.isBlank()) {
            setTip("Nhập Room ID trước khi Join.");
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "join_room");
        payload.addProperty("roomId", roomId.trim());
        sendSystemCommand(payload);
        setTip("Đang vào phòng " + roomId + "...");
    }

    private void handleQuickMatch() {
        if (!ensureConnected()) return;
        setTip("Đang tìm đối thủ...");
        sendSystemEvent("quick_match");
    }

    private void updateStatusLabels() {
        if (turnLabel != null) {
            Color currentTurn = game.getTurn();
            String text = currentTurn == Color.WHITE ? "Trắng" : "Đen";
            
            if (session.isConnected()) {
                if (session.getPlayerColor() == currentTurn) {
                    text += " (Lượt của BẠN)";
                } else {
                    text += " (Lượt đối thủ)";
                }
                
                // Thêm thông tin về màu của bạn
                String myColorText = session.getPlayerColor() == Color.WHITE ? " (Bạn: Trắng)" : " (Bạn: Đen)";
                text += myColorText;
            }
            
            turnLabel.setText(text);
        }
        if (statusLabel != null) {
            String base = game.getResult().name();
            if (game.getResult() == GameResult.ONGOING) {
                boolean inCheck = game.getBoard().isInCheck(game.getTurn());
                statusLabel.setText(inCheck ? "CHECK" : base);
            } else {
                statusLabel.setText(base);
            }
        }
        if (selectedFrom == null) {
            highlightCheckIndicator();
        }
    }

    private void highlightMoves(Position from, List<Move> moves) {
        clearHighlights();
        // highlight ô nguồn
        highlightSquare(from, "#3498db");
        // highlight các ô đích
        for (Move m : moves) highlightSquare(m.getTo(), "#27ae60");
    }

    private void clearHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Pane sq = getSquarePane(row, col);
                if (sq == null) continue;
                boolean isLight = (row + col) % 2 == 0;
                sq.setStyle("-fx-background-color: " + (isLight ? LIGHT_COLOR : DARK_COLOR) + ";");
            }
        }
        if (selectedFrom == null) {
            highlightCheckIndicator();
        }
    }

    private void highlightSquare(Position p, String borderColor) {
        Pane sq = getSquarePane(p.row(), p.col());
        if (sq == null) return;
        boolean isLight = (p.row() + p.col()) % 2 == 0;
        String bg = isLight ? LIGHT_COLOR : DARK_COLOR;
        sq.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 3;");
    }

    private Pane getSquarePane(int row, int col) {
        for (javafx.scene.Node n : chessBoard.getChildren()) {
            if (n instanceof Pane) {
                Integer c = GridPane.getColumnIndex(n);
                Integer r = GridPane.getRowIndex(n);
                int cc = c == null ? 0 : c;
                int rr = r == null ? 0 : r;
                if (cc == col && rr == row) return (Pane) n;
            }
        }
        return null;
    }

    private void handlePieceClicked(int row, int col) {
        // Kiểm tra game đã kết thúc chưa
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Ván cờ đã kết thúc. Không thể di chuyển quân cờ.");
            return;
        }
        
        // Kiểm tra nếu không phải lượt của mình (trong P2P mode)
        if (session.isConnected() && session.getPlayerColor() != game.getTurn()) {
            Color currentTurn = game.getTurn();
            String message = "Chưa đến lượt của bạn! Lượt hiện tại: " + 
                (currentTurn == Color.WHITE ? "Trắng" : "Đen");
            setTip(message);
            return;
        }
        Position pos = Position.of(row, col);
        Piece piece = game.getBoard().getPiece(pos);
        if (piece == null) return;
        // Nếu đang chọn quân và click vào quân đối thủ -> thử thực hiện nước ăn
        if (selectedFrom != null && piece.getColor() != game.getTurn()) {
            Move chosen = pickMoveTo(currentLegalMoves, pos);
            if (chosen != null) {
                System.out.println("[MainController] ===== Applying local move (capture) =====");
                System.out.println("[MainController] Move: " + chosen);
                System.out.println("[MainController] Turn BEFORE: " + game.getTurn());
                
                if (syncManager.applyLocalMove(chosen)) {
                    System.out.println("[MainController] ✓ Local move applied successfully");
                    System.out.println("[MainController] Turn AFTER: " + game.getTurn());
                    appendHistory(chosen);
                    renderBoardFromModel();
                    updateStatusLabels();
                    sendMoveToPeer(chosen);
                    selectedFrom = null;
                    currentLegalMoves = java.util.Collections.emptyList();
                    clearHighlights();
                    System.out.println("[MainController] ===== Local move completed =====");
                    return;
                } else {
                    System.out.println("[MainController] ✗ Failed to apply local move");
                }
            }
        }
        // Nếu click vào quân đúng lượt -> chọn quân đó và highlight
        if (piece.getColor() == game.getTurn()) {
            selectedFrom = pos;
            currentLegalMoves = game.legalMovesFor(pos);
            highlightMoves(pos, currentLegalMoves);
        }
    }

    private void sendMoveToPeer(Move move) {
        if (!session.isConnected() || session.getPeer() == null) {
            setTip("Chưa kết nối server. Không thể gửi nước đi.");
            System.err.println("[MainController] Cannot send move: not connected");
            return;
        }
        if (session.getRoomId() == null || session.getRoomId().isBlank()) {
            setTip("Chưa vào phòng. Không thể gửi nước đi.");
            System.err.println("[MainController] Cannot send move: no room ID");
            return;
        }
        try {
            Message msg = new Message(session.getPlayerName(), session.getRoomId(), MessageType.MOVE, move.toJson());
            System.out.println("[MainController] Sending MOVE: " + move + " from " + session.getPlayerName() + " to room " + session.getRoomId());
            session.getPeer().send(msg);
            System.out.println("[MainController] MOVE sent successfully");
        } catch (Exception e) {
            System.err.println("[MainController] Error sending move: " + e.getMessage());
            e.printStackTrace();
            setTip("Lỗi gửi nước đi: " + e.getMessage());
        }
    }

    private void sendSystemEvent(String event) {
        if (!session.isConnected() || session.getPeer() == null) {
            setTip("Chưa kết nối server. Vui lòng đăng nhập trước.");
            return;
        }
        try {
            JsonObject json = new JsonObject();
            json.addProperty("event", event);
            Message msg = new Message(session.getPlayerName(), "server", MessageType.SYSTEM, json.toString());
            session.getPeer().send(msg);
        } catch (Exception e) {
            setTip("Lỗi gửi message: " + e.getMessage());
        }
    }

    private void sendSystemCommand(JsonObject payload) {
        if (!session.isConnected() || session.getPeer() == null) {
            setTip("Chưa kết nối server. Vui lòng đăng nhập trước.");
            return;
        }
        try {
            Message msg = new Message(session.getPlayerName(), "server", MessageType.SYSTEM, payload.toString());
            session.getPeer().send(msg);
        } catch (Exception e) {
            setTip("Lỗi gửi message: " + e.getMessage());
        }
    }

    private boolean ensureConnected() {
        if (!session.isConnected()) {
            setTip("Chưa kết nối server. Vui lòng đăng nhập trước.");
            return false;
        }
        return true;
    }

    private void rebuildHistoryFromGame() {
        moveItems.clear();
        int index = 1;
        for (Move mv : game.getHistory()) {
            moveItems.add(index++ + ". " + mv);
        }
    }
    
    // Public method để GameMessageHandler có thể access moveItems
    public ObservableList<String> getMoveItems() {
        return moveItems;
    }

    public void applyPlayerNames() {
        if (lNameWhite == null || lNameBlack == null) return;
        if (session.getPlayerColor() == Color.WHITE) {
            lNameWhite.setText(session.getPlayerName());
            lNameBlack.setText(session.getOpponentName());
        } else {
            lNameWhite.setText(session.getOpponentName());
            lNameBlack.setText(session.getPlayerName());
        }
    }
    
    public void updateRoomId(String roomId) {
        if (roomIdLabel != null) {
            roomIdLabel.setText(roomId);
        }
        // Luôn update joinRoomField với room ID mới (để dễ share)
        if (joinRoomField != null) {
            joinRoomField.setText(roomId);
        }
    }

    private void highlightCheckIndicator() {
        if (game.getResult() != GameResult.ONGOING) return;
        Color checked = game.getTurn();
        if (!game.getBoard().isInCheck(checked)) return;
        Position kingPos = findKingPosition(checked);
        if (kingPos != null) {
            highlightSquare(kingPos, "#e74c3c");
        }
    }

    private Position findKingPosition(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = game.getBoard().getPiece(Position.of(r, c));
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return Position.of(r, c);
                }
            }
        }
        return null;
    }

    private void setTip(String message) {
        if (tipLabel != null && message != null) {
            tipLabel.setText(message);
        }
    }

    // Message handling đã được chuyển sang GameMessageHandler

    private void handleSquareClicked(int row, int col) {
        // Kiểm tra game đã kết thúc chưa
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Ván cờ đã kết thúc. Không thể di chuyển quân cờ.");
            return;
        }
        
        // Kiểm tra nếu không phải lượt của mình (trong P2P mode)
        if (session.isConnected() && session.getPlayerColor() != game.getTurn()) {
            Color currentTurn = game.getTurn();
            String message = "Chưa đến lượt của bạn! Lượt hiện tại: " + 
                (currentTurn == Color.WHITE ? "Trắng" : "Đen");
            setTip(message);
            return;
        }
        Position target = Position.of(row, col);
        // Nếu đã chọn nguồn và target là nước hợp lệ → đi
        if (selectedFrom != null && !currentLegalMoves.isEmpty()) {
            Move chosen = pickMoveTo(currentLegalMoves, target);
            if (chosen != null) {
                System.out.println("[MainController] ===== Applying local move (click) =====");
                System.out.println("[MainController] Move: " + chosen);
                System.out.println("[MainController] Turn BEFORE: " + game.getTurn());
                
                if (syncManager.applyLocalMove(chosen)) {
                    System.out.println("[MainController] ✓ Local move applied successfully");
                    System.out.println("[MainController] Turn AFTER: " + game.getTurn());
                    appendHistory(chosen);
                    renderBoardFromModel();
                    updateStatusLabels();
                    sendMoveToPeer(chosen);
                    selectedFrom = null;
                    currentLegalMoves = java.util.Collections.emptyList();
                    clearHighlights();
                    System.out.println("[MainController] ===== Local move completed =====");
                    return;
                } else {
                    System.out.println("[MainController] ✗ Failed to apply local move");
                }
            }
        }
        // Nếu chưa chọn hoặc click vào ô có quân đúng lượt → chọn và highlight
        Piece onSquare = game.getBoard().getPiece(target);
        if (onSquare != null && onSquare.getColor() == game.getTurn()) {
            selectedFrom = target;
            currentLegalMoves = game.legalMovesFor(target);
            highlightMoves(target, currentLegalMoves);
        } else {
            // Click vào ô trống khi chưa chọn hợp lệ → bỏ chọn
            selectedFrom = null;
            currentLegalMoves = java.util.Collections.emptyList();
            clearHighlights();
        }
    }

}

package com.example.chess_project_p2p_hybrid.client.controller;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageType;
import com.example.chess_project_p2p_hybrid.client.handler.GameMessageHandler;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Game;
import com.example.chess_project_p2p_hybrid.client.model.game.GameResult;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;
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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

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

    // --- Promotion UI ---
    private HBox promotionOverlay;
    private Position pendingPromotionFrom;
    private Position pendingPromotionTo;

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
    @FXML
    private Button homeButton;
    @FXML
    private Button pvpButton;
    @FXML
    private Button pvcButton;
    @FXML
    private Button chatButton;
    @FXML
    private Button howToPlayButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button resignButton;
    @FXML
    private Button newGameButton;
    @FXML
    private Button quickMatchButton;
    @FXML
    private Button joinRoomButton;
    @FXML
    private Button createRoomButton;
    @FXML
    private Button undoButton;
    @FXML
    private Button drawButton;
    @FXML
    private Label roomIdLabel;
    @FXML
    private Label turnLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label lNameWhite;
    @FXML
    private Label lNameBlack;
    @FXML
    private Label whitePlayerName;
    @FXML
    private Label blackPlayerName;
    @FXML
    private Label playerInfoLabel;
    @FXML
    private Label opponentInfoLabel;
    @FXML
    private Label tipLabel;
    @FXML
    private TextField joinRoomField;
    @FXML
    private VBox homePanelView;
    @FXML
    private VBox gamePanelView;
    @FXML
    private VBox matchInfoCard;
    @FXML
    private StackPane waitingOverlay;
    @FXML
    private StackPane countdownOverlay;
    @FXML
    private Label countdownLabel;

    private final ClientSession session = ClientSession.getInstance();
    private boolean boardInteractive = false;

    /**
     * Phương thức được gọi sau khi tất cả các phần tử FXML đã được tải và tiêm.
     */
    @FXML
    private Label whiteTimerLabel;
    @FXML
    private Label blackTimerLabel;
    @FXML
    private Button pauseButton;
    @FXML
    private Button exitRoomButton;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userAvatarLabel;

    public void updateUserProfile() {
        if (userNameLabel != null && userAvatarLabel != null) {
            String name = session.getPlayerName();
            if (name == null || name.isEmpty()) {
                name = "Khách";
            }
            userNameLabel.setText(name);
            
            // Lấy chữ cái đầu làm avatar
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            userAvatarLabel.setText(initial);
        }
    }

    // Timer variables
    private static final int GAME_TIME_SECONDS = 600; // 10 minutes
    private int whiteTimeSeconds = GAME_TIME_SECONDS;
    private int blackTimeSeconds = GAME_TIME_SECONDS;
    private javafx.animation.Timeline gameTimer;
    private boolean isPaused = false;

    // ... existing code ...

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
            javafx.application.Platform.runLater(() -> {
                renderBoardFromModel();
                updateStatusLabels();
                clearHighlights();
                rebuildHistoryFromGame(); // Sync UI history with model (which is empty on new game)
                startTimer(); // Start timer when game starts
            });
        });
        gameMessageHandler.setOnMoveReceived(() -> {
            renderBoardFromModel();
            updateStatusLabels();
            rebuildHistoryFromGame();
            clearHighlights();
            switchTimer(); // Switch timer when move received
        });
        gameMessageHandler.setOnGameViewSwitch(() -> {
            if (session.getRoomId() != null) {
                startGameView(session.getRoomId());
            }
        });

        createAndColorChessBoard();
        if (moveListView != null) {
            moveListView.setItems(moveItems);
            moveListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px; -fx-padding: 5;");
                    }
                }
            });
        }
        renderBoardFromModel();
        setBoardInteractivity(false);
        if (tipLabel != null) {
            tipLabel.setText("Mẹo: Nhấp vào quân để xem nước đi hợp lệ. Kéo hoặc nhấp vào ô để di chuyển.");
        }

        if (lNameWhite != null) {
            lNameWhite.setText("Chưa sẵn sàng");
        }
        if (lNameBlack != null) {
            lNameBlack.setText("Chưa sẵn sàng");
        }

        // wire sidebar actions
        if (homeButton != null)
            homeButton.setOnAction(e -> showHome());
        if (pvpButton != null)
            pvpButton.setOnAction(e -> startGameView("[PvP nội bộ]"));
        if (pvcButton != null)
            pvcButton.setOnAction(e -> startGameView("[PvC với máy]"));
        if (chatButton != null)
            chatButton.setOnAction(e -> SceneNavigator.showChatWindow());
        if (howToPlayButton != null)
            howToPlayButton
                    .setOnAction(e -> setTip("Luật cờ vua: nhập thành, bắt tốt qua đường (en passant), phong cấp..."));
        if (exitButton != null)
            exitButton.setOnAction(e -> javafx.application.Platform.exit());
        if (resignButton != null)
            resignButton.setOnAction(e -> handleResign());
        if (newGameButton != null)
            newGameButton.setOnAction(e -> resetGame(true, false));
        if (undoButton != null)
            undoButton.setOnAction(e -> handleUndo());
        if (quickMatchButton != null)
            quickMatchButton.setOnAction(e -> handleQuickMatch());
        if (joinRoomButton != null)
            joinRoomButton.setOnAction(e -> handleJoinRoom());
        if (createRoomButton != null)
            createRoomButton.setOnAction(e -> handleCreateRoom());
        if (pauseButton != null)
            pauseButton.setOnAction(e -> handlePause());
        if (exitRoomButton != null)
            exitRoomButton.setOnAction(e -> handleExitRoom());
        if (drawButton != null)
            drawButton.setOnAction(e -> handleDrawOffer());

        // Luôn setup handler nếu đã connected (ngay cả khi chưa có room)
        if (session.isConnected()) {
            session.setMessageHandler(gameMessageHandler);
            if (session.getRoomId() != null && !session.getRoomId().isBlank()) {
                if (roomIdLabel != null)
                    roomIdLabel.setText(session.getRoomId());
                applyPlayerNames();
            }
        }
        if (roomIdLabel != null && (session.getRoomId() == null || session.getRoomId().isBlank())) {
            roomIdLabel.setText("[Chưa có phòng]");
        }

        updateStatusLabels();
        setupTimer();
        updateUserProfile();
    }

    // --- Timer Logic ---
    private void setupTimer() {
        gameTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (game.getResult() != GameResult.ONGOING || isPaused) return;

            if (game.getTurn() == Color.WHITE) {
                whiteTimeSeconds--;
                if (whiteTimeSeconds <= 0) handleTimeout(Color.WHITE);
            } else {
                blackTimeSeconds--;
                if (blackTimeSeconds <= 0) handleTimeout(Color.BLACK);
            }
            updateTimerLabels();
        }));
        gameTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
    }

    public void startTimer() {
        whiteTimeSeconds = GAME_TIME_SECONDS;
        blackTimeSeconds = GAME_TIME_SECONDS;
        updateTimerLabels();
        gameTimer.play();
    }

    private void switchTimer() {
        // Timer runs continuously, logic inside checks whose turn it is
    }

    private void stopTimer() {
        if (gameTimer != null) gameTimer.stop();
    }

    private void updateTimerLabels() {
        if (whiteTimerLabel != null) whiteTimerLabel.setText(formatTime(whiteTimeSeconds));
        if (blackTimerLabel != null) blackTimerLabel.setText(formatTime(blackTimeSeconds));
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void handleTimeout(Color loserColor) {
        stopTimer();
        GameResult result = (loserColor == Color.WHITE) ? GameResult.CHECKMATE_BLACK : GameResult.CHECKMATE_WHITE;
        game.setResult(result);

        String msg = (loserColor == Color.WHITE ? "Trắng" : "Đen") + " hết giờ! " +
                (loserColor == Color.WHITE ? "Đen" : "Trắng") + " thắng.";
        setTip(msg);
        moveItems.add("[Hết giờ - " + msg + "]");
        updateStatusLabels();

        // Gửi thông báo timeout cho đối thủ (nếu là local timeout của mình)
        if (session.isConnected() && session.getPlayerColor() == loserColor) {
            sendSystemEvent("timeout");
        }
    }

    private void handlePause() {
        if (game.getResult() != GameResult.ONGOING) return;

        if (!session.isConnected()) {
            setGamePaused(!isPaused);
            return;
        }

        // P2P: Gửi yêu cầu pause hoặc resume
        JsonObject payload = new JsonObject();
        if (isPaused) {
            payload.addProperty("event", "resume_request");
            setTip("Đã gửi yêu cầu tiếp tục...");
        } else {
            payload.addProperty("event", "pause_request");
            setTip("Đã gửi yêu cầu tạm dừng...");
        }
        payload.addProperty("from", session.getPlayerName());
        sendSystemCommand(payload);
    }

    public void onPauseRequest(String from) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Yêu cầu tạm dừng");
            alert.setHeaderText(from + " muốn tạm dừng trận đấu");
            alert.setContentText("Bạn có đồng ý không?");

            alert.showAndWait().ifPresent(type -> {
                if (type == javafx.scene.control.ButtonType.OK) {
                    sendSystemEvent("pause_accept");
                    setGamePaused(true);
                } else {
                    sendSystemEvent("pause_reject");
                }
            });
        });
    }

    public void onResumeRequest(String from) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Yêu cầu tiếp tục");
            alert.setHeaderText(from + " muốn tiếp tục trận đấu");
            alert.setContentText("Bạn có đồng ý không?");

            alert.showAndWait().ifPresent(type -> {
                if (type == javafx.scene.control.ButtonType.OK) {
                    sendSystemEvent("resume_accept");
                    setGamePaused(false);
                } else {
                    sendSystemEvent("resume_reject");
                }
            });
        });
    }

    public void setGamePaused(boolean paused) {
        this.isPaused = paused;
        if (pauseButton != null) pauseButton.setText(paused ? "Tiếp tục" : "Tạm dừng");
        
        // Chỉ update tip nếu nó không quá quan trọng (ví dụ không overwrite thông báo kết quả)
        if (game.getResult() == GameResult.ONGOING) {
            setTip(paused ? "Trận đấu đang tạm dừng." : "Trận đấu tiếp tục!");
        }

        if (paused) {
            if (gameTimer != null) gameTimer.pause();
        } else {
            if (gameTimer != null) gameTimer.play();
        }
    }

    private void handleExitRoom() {
        if (!session.isConnected()) {
            showHome();
            return;
        }

        boolean hasOpponent = session.getOpponentName() != null;
        boolean isGameActive = game.getResult() == GameResult.ONGOING;

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Thoát phòng");
        alert.setHeaderText("Bạn có chắc muốn thoát phòng?");

        if (hasOpponent && isGameActive) {
            alert.setContentText("Ván đấu đang diễn ra. Bạn sẽ bị xử THUA nếu thoát ngay bây giờ.");
        } else {
            alert.setContentText("Bạn sẽ rời khỏi phòng hiện tại.");
        }

        alert.showAndWait().ifPresent(type -> {
            if (type == javafx.scene.control.ButtonType.OK) {
                if (hasOpponent && isGameActive) {
                    handleResign(); // Tự động resign nếu đang chơi
                }
                sendSystemEvent("leave_room");
                
                // Reset P2P connection to allow new connections later
                if (session.getChessClient() != null) {
                    session.getChessClient().resetP2P();
                }
                
                session.setRoomId(null);
                session.setOpponentName(null);
                session.setPlayerColor(null);
                showHome();
                setTip("Đã thoát phòng.");
            }
        });
    }

    // ... existing code ...


    // ... existing code ...

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
     * 2. Lưu vị trí ban đầu (offset) của chuột so với góc trên bên trái của quân
     * cờ.
     */
    private void handleMousePressed(MouseEvent event) {
        if (!boardInteractive) {
            setTip("Bắt đầu một ván mới hoặc tham gia phòng để chơi.");
            return;
        }
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
        if (draggedPiece == null)
            return;
        if (!boardInteractive) {
            return;
        }
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
        if (draggedPiece == null)
            return;
        if (!boardInteractive) {
            draggedPiece.setStyle("-fx-opacity: 1.0; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            draggedPiece.setLayoutX(0);
            draggedPiece.setLayoutY(0);
            draggedPiece = null;
            startCol = null;
            startRow = null;
            setTip("Bắt đầu một ván mới hoặc tham gia phòng để chơi.");
            return;
        }

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

            List<Move> legalMoves = (selectedFrom != null && selectedFrom.equals(from)) ? currentLegalMoves
                    : game.legalMovesFor(from);
            Move chosen = pickMoveTo(legalMoves, to);
            if (chosen != null) {
                applyMove(chosen);
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
        for (Move m : legalMoves)
            if (m.getTo().equals(to))
                candidates.add(m);
        if (candidates.isEmpty())
            return null;

        // Nếu không có promotion hoặc chỉ có 1 lựa chọn, trả luôn
        boolean anyPromotion = candidates.stream().anyMatch(m -> m.getPromotionTo() != null);
        if (!anyPromotion || candidates.size() == 1)
            return candidates.get(0);

        // Có nhiều lựa chọn phong cấp: hiển thị Custom UI
        pendingPromotionFrom = candidates.get(0).getFrom();
        pendingPromotionTo = to;
        showPromotionUI();
        
        return null; // Trả về null để đợi người dùng chọn
    }

    private void applyMove(Move chosen) {
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

    private void createPromotionOverlay() {
        promotionOverlay = new HBox(15);
        promotionOverlay.setAlignment(Pos.CENTER);
        promotionOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-padding: 20; -fx-background-radius: 15; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 15;");
        promotionOverlay.setMaxSize(450, 150);

        PieceType[] types = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
        
        for (int i = 0; i < types.length; i++) {
            final PieceType type = types[i];
            Button btn = new Button();
            // Padding 0 is critical to prevent "..." truncation
            btn.setStyle("-fx-font-size: 45px; -fx-min-width: 80px; -fx-min-height: 80px; -fx-max-width: 80px; -fx-max-height: 80px; -fx-padding: 0; -fx-background-color: #f0d9b5; -fx-cursor: hand; -fx-alignment: center; -fx-text-fill: black;");
            btn.setOnAction(e -> onPromotionSelected(type));
            promotionOverlay.getChildren().add(btn);
        }
        
        if (waitingOverlay != null) {
             waitingOverlay.getChildren().add(promotionOverlay);
             promotionOverlay.setVisible(false);
        }
    }

    private void showPromotionUI() {
        if (promotionOverlay == null) {
            createPromotionOverlay();
        }
        // Update symbols based on turn color
        Color turn = game.getTurn();
        String[] symbols = (turn == Color.WHITE) ? new String[]{WQ, WR, WB, WN} : new String[]{BQ, BR, BB, BN};
        for(int i=0; i<promotionOverlay.getChildren().size(); i++) {
            if(promotionOverlay.getChildren().get(i) instanceof Button) {
                Button btn = (Button) promotionOverlay.getChildren().get(i);
                btn.setText(symbols[i]);
            }
        }
        
        if (waitingOverlay != null) {
            waitingOverlay.setVisible(true);
            waitingOverlay.setMouseTransparent(false);
        }
        promotionOverlay.setVisible(true);
        promotionOverlay.toFront();
    }

    private void onPromotionSelected(PieceType type) {
        if (pendingPromotionFrom == null || pendingPromotionTo == null) return;
        
        List<Move> legalMoves = game.legalMovesFor(pendingPromotionFrom);
        Move chosen = null;
        for (Move m : legalMoves) {
            if (m.getTo().equals(pendingPromotionTo) && m.getPromotionTo() == type) {
                chosen = m;
                break;
            }
        }
        
        // Fallback
        if (chosen == null) {
             for (Move m : legalMoves) {
                if (m.getTo().equals(pendingPromotionTo) && m.getPromotionTo() == PieceType.QUEEN) {
                    chosen = m;
                    break;
                }
            }
        }

        if (chosen != null) {
            applyMove(chosen);
        }

        // Hide UI
        promotionOverlay.setVisible(false);
        if (waitingOverlay != null) {
            waitingOverlay.setVisible(false);
            waitingOverlay.setMouseTransparent(true);
        }
        
        pendingPromotionFrom = null;
        pendingPromotionTo = null;
    }

    private void appendHistory(Move m) {
        if (moveListView == null)
            return;
        moveItems.add((moveItems.size() + 1) + ". " + m.toAlgebraicNotation());
        moveListView.scrollTo(moveItems.size() - 1);
    }

    private void showHome() {
        setBoardInteractivity(false);
        setPlayerInfoVisible(false);
        if (homePanelView != null && gamePanelView != null) {
            homePanelView.setVisible(true);
            homePanelView.setManaged(true);
            gamePanelView.setVisible(false);
            gamePanelView.setManaged(false);
        }
    }

    private void startGameView(String roomId) {
        if (roomIdLabel != null)
            roomIdLabel.setText(roomId);
        if (homePanelView != null && gamePanelView != null) {
            homePanelView.setVisible(false);
            homePanelView.setManaged(false);
            gamePanelView.setVisible(true);
            gamePanelView.setManaged(true);
        }
        setBoardInteractivity(true);
        setPlayerInfoVisible(true);
        setPlayerInfoVisible(true);
        updateMatchInfoTexts();
        resetGame(false, false); // Don't start timer immediately when view opens
    }

    private void resetGame(boolean broadcast, boolean startTimerNow) {
        syncManager.syncGameState();
        moveItems.clear();
        renderBoardFromModel();
        updateStatusLabels();
        clearHighlights();
        
        // Reset time values but don't auto-start unless specified
        whiteTimeSeconds = GAME_TIME_SECONDS;
        blackTimeSeconds = GAME_TIME_SECONDS;
        updateTimerLabels();
        if (gameTimer != null) {
            gameTimer.stop(); // Stop any running timer
            if (startTimerNow) {
                gameTimer.play();
            }
        }

        if (broadcast && session.isConnected()) {
            // Gửi yêu cầu ván mới cho đối thủ
            requestNewGame();
        }
    }

    public void resetGameUI() {
        // Reset UI but DO NOT start timer yet (wait for opponent or game start signal)
        resetGame(false, false);
    }

    private void requestNewGame() {
        if (!session.isConnected()) {
            // Local game - reset ngay và start timer luôn
            resetGame(false, true);
            return;
        }

        if (session.getOpponentName() == null) {
            setTip("Chưa có đối thủ. Ván mới đã được thiết lập.");
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
            game.setResult(
                    session.getPlayerColor() == Color.WHITE ? GameResult.CHECKMATE_BLACK : GameResult.CHECKMATE_WHITE);
            moveItems.add("[Đầu hàng - " + session.getPlayerName() + "]");
            setTip("Bạn đã đầu hàng.");
            updateStatusLabels();
            renderBoardFromModel();
            stopTimer();
            return;
        }

        // P2P game - gửi resign signal
        // Set result cho bên thua (chính mình)
        Color winner = session.getPlayerColor().opposite();
        game.setResult(winner == Color.WHITE ? GameResult.CHECKMATE_WHITE : GameResult.CHECKMATE_BLACK);

        moveItems.add("[Đầu hàng - " + session.getPlayerName() + "]");
        setTip("Bạn đã đầu hàng. " + session.getOpponentName() + " thắng!");
        updateStatusLabels();
        renderBoardFromModel();
        stopTimer();

        // Gửi resign signal cho đối thủ
        // Gửi resign signal cho đối thủ
        sendSystemEvent("resign");
    }

    private void handleDrawOffer() {
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Ván cờ đã kết thúc.");
            return;
        }
        if (!session.isConnected()) {
            setTip("Chế độ chơi đơn không hỗ trợ cầu hòa.");
            return;
        }
        
        // Gửi yêu cầu cầu hòa
        sendSystemEvent("draw_offer");
        setTip("Đã gửi lời mời cầu hòa. Chờ đối thủ...");
    }

    public void onDrawOffer(String from) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cầu hòa");
            alert.setHeaderText(from + " muốn cầu hòa");
            alert.setContentText("Bạn có đồng ý hòa không?");

            alert.showAndWait().ifPresent(type -> {
                if (type == javafx.scene.control.ButtonType.OK) {
                    sendSystemEvent("draw_accept");
                    handleDrawResult();
                } else {
                    sendSystemEvent("draw_reject");
                }
            });
        });
    }

    public void onDrawAccept() {
        handleDrawResult();
        showNotification("Kết quả", "Hòa!", "Đối thủ đã đồng ý cầu hòa.");
    }

    public void onDrawReject() {
        setTip("Đối thủ đã từ chối cầu hòa.");
        showNotification("Thông báo", "Từ chối", "Đối thủ không đồng ý hòa.");
    }

    private void handleDrawResult() {
        game.setResult(GameResult.DRAW_BY_AGREEMENT);
        moveItems.add("[Hòa - Thỏa thuận]");
        updateStatusLabels();
        stopTimer();
        setTip("Ván đấu kết thúc với tỉ số HÒA.");
    }

    private void handleUndo() {
        // Kiểm tra điều kiện
        if (game.getResult() != GameResult.ONGOING) {
            setTip("Không thể hoàn tác khi ván cờ đã kết thúc.");
            return;
        }

        if (game.getHistory().isEmpty()) {
            setTip("Không còn nước nào để hoàn tác.");
            return;
        }

        // Kiểm tra trong P2P mode: chỉ undo được khi có ít nhất 2 moves (1 move của
        // mình + 1 move của đối thủ)
        // Hoặc cho phép undo ngay cả khi chỉ có 1 move (nếu là move của mình)
        if (session.isConnected() && game.getHistory().size() < 1) {
            setTip("Chưa có nước đi nào để hoàn tác.");
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
                setTip("Đã hoàn tác nước đi. Đang chờ đối thủ đồng bộ...");
            } else {
                setTip("Đã hoàn tác nước đi.");
            }
        } else {
            setTip("Không thể hoàn tác nước đi này.");
        }
    }

    private void handleCreateRoom() {
        if (!ensureConnected())
            return;
        setTip("Đang tạo phòng mới...");
        sendSystemEvent("create_room");
    }

    private void handleJoinRoom() {
        if (!ensureConnected())
            return;
        if (joinRoomField == null)
            return;
        String roomId = joinRoomField.getText();
        if (roomId == null || roomId.isBlank()) {
            setTip("Nhập mã phòng trước khi tham gia.");
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "join_room");
        payload.addProperty("roomId", roomId.trim());
        sendSystemCommand(payload);
        setTip("Đang vào phòng " + roomId + "...");
    }

    private void handleQuickMatch() {
        if (!ensureConnected())
            return;
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
            GameResult result = game.getResult();
            String base = translateGameResult(result);
            if (result == GameResult.ONGOING) {
                boolean inCheck = game.getBoard().isInCheck(game.getTurn());
                statusLabel.setText(inCheck ? "Chiếu" : base);
            } else {
                statusLabel.setText(base);
            }
        }
        if (selectedFrom == null) {
            highlightCheckIndicator();
        }
    }

    private String translateGameResult(GameResult result) {
        return switch (result) {
            case ONGOING -> "Đang diễn ra";
            case CHECKMATE_WHITE -> "Trắng thắng (chiếu hết)";
            case CHECKMATE_BLACK -> "Đen thắng (chiếu hết)";
            case STALEMATE -> "Hòa (bế tắc)";
            case DRAW_BY_AGREEMENT -> "Hòa do thỏa thuận";
            case DRAW_50_MOVES -> "Hòa (50 nước đi)";
            case DRAW_THREEFOLD_REPETITION -> "Hòa (lặp lại 3 lần)";
        };
    }

    private void highlightMoves(Position from, List<Move> moves) {
        clearHighlights();
        // highlight ô nguồn
        highlightSquare(from, "#3498db");
        // highlight các ô đích
        for (Move m : moves)
            highlightSquare(m.getTo(), "#27ae60");
    }

    private void clearHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Pane sq = getSquarePane(row, col);
                if (sq == null)
                    continue;
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
        if (sq == null)
            return;
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
                if (cc == col && rr == row)
                    return (Pane) n;
            }
        }
        return null;
    }

    private void handlePieceClicked(int row, int col) {
        if (!boardInteractive) {
            setTip("Bắt đầu một ván mới hoặc tham gia phòng để chơi.");
            return;
        }
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
        if (piece == null)
            return;
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
            highlightMoves(pos, currentLegalMoves);
        }
    }

    private void rebuildHistoryFromGame() {
        moveItems.clear();
        int index = 1;
        for (Move mv : game.getHistory()) {
            moveItems.add(index++ + ". " + mv.toAlgebraicNotation());
        }
    }

    // Public method để GameMessageHandler có thể access moveItems
    public ObservableList<String> getMoveItems() {
        return moveItems;
    }

    public void applyPlayerNames() {
        if (lNameWhite == null || lNameBlack == null)
            return;
        if (session.getPlayerColor() == Color.WHITE) {
            lNameWhite.setText(orDefault(session.getPlayerName(), "Bạn"));
            lNameBlack.setText(orDefault(session.getOpponentName(), "Đang chờ..."));
        } else {
            lNameWhite.setText(orDefault(session.getOpponentName(), "Đang chờ..."));
            lNameBlack.setText(orDefault(session.getPlayerName(), "Bạn"));
        }
        updateMatchInfoTexts();
    }


    private void highlightCheckIndicator() {
        if (game.getResult() != GameResult.ONGOING)
            return;
        Color checked = game.getTurn();
        if (!game.getBoard().isInCheck(checked))
            return;
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
        if (!boardInteractive) {
            setTip("Bắt đầu một ván mới hoặc tham gia phòng để chơi.");
            return;
        }
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

    private void setBoardInteractivity(boolean enabled) {
        boardInteractive = enabled;
        if (chessBoard != null) {
            chessBoard.setMouseTransparent(!enabled);
            chessBoard.setOpacity(enabled ? 1.0 : 0.8);
        }
        if (waitingOverlay != null) {
            waitingOverlay.setVisible(!enabled);
            waitingOverlay.setManaged(!enabled);
        }
    }

    private void setPlayerInfoVisible(boolean visible) {
        toggleNodeVisibility(whitePlayerName, visible);
        toggleNodeVisibility(lNameWhite, visible);
        toggleNodeVisibility(blackPlayerName, visible);
        toggleNodeVisibility(lNameBlack, visible);
        toggleNodeVisibility(playerInfoLabel, visible);
        toggleNodeVisibility(opponentInfoLabel, visible);
        toggleNodeVisibility(matchInfoCard, visible);
        if (visible) {
            updateMatchInfoTexts();
        } else {
            resetMatchInfoTexts();
        }
    }

    public void updateMatchInfoTexts() {
        if (playerInfoLabel != null) {
            String playerName = orDefault(session.getPlayerName(), "--");
            String colorText = session.getPlayerColor() == Color.WHITE ? "Trắng" : "Đen";
            playerInfoLabel.setText(playerName + " (" + colorText + ")");
        }
        if (opponentInfoLabel != null) {
            opponentInfoLabel.setText(orDefault(session.getOpponentName(), "Đang chờ..."));
        }
    }

    private void resetMatchInfoTexts() {
        if (playerInfoLabel != null) {
            playerInfoLabel.setText("--");
        }
        if (opponentInfoLabel != null) {
            opponentInfoLabel.setText("--");
        }
    }

    public void updateRoomId(String roomId) {
        if (roomIdLabel != null) roomIdLabel.setText(roomId);
        if (joinRoomField != null) joinRoomField.setText(roomId); // Tự động điền mã phòng
        setTip("Mã phòng: " + roomId + ". Chờ đối thủ...");
    }

    public void onPeerConnected() {
        setTip("Đã kết nối! Chuẩn bị bắt đầu...");
        updateMatchInfoTexts();

        // Nếu đang ở Home thì chuyển sang Game View
        if (homePanelView != null && homePanelView.isVisible()) {
            startGameView(session.getRoomId());
        }
        
        // Start countdown if game is ongoing
        if (game.getResult() == GameResult.ONGOING) {
            startCountdown();
        } else {
             setBoardInteractivity(true);
        }
    }

    private void startCountdown() {
        setBoardInteractivity(false); // Lock board
        if (countdownOverlay != null) {
            countdownOverlay.setVisible(true);
            countdownOverlay.setManaged(true);
        }
        
        javafx.animation.Timeline countdown = new javafx.animation.Timeline();
        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0), e -> {
            if (countdownLabel != null) countdownLabel.setText("3");
        }));
        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (countdownLabel != null) countdownLabel.setText("2");
        }));
        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
            if (countdownLabel != null) countdownLabel.setText("1");
        }));
        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
            if (countdownLabel != null) countdownLabel.setText("BẮT ĐẦU!");
        }));
        countdown.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3.5), e -> {
            if (countdownOverlay != null) {
                countdownOverlay.setVisible(false);
                countdownOverlay.setManaged(false);
            }
            setBoardInteractivity(true); // Unlock board
            setTip("Đến lượt: " + (game.getTurn() == Color.WHITE ? "Trắng" : "Đen"));
            if (gameTimer != null) gameTimer.play();
        }));
        countdown.play();
    }

    private void toggleNodeVisibility(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void sendMoveToPeer(Move move) {
        if (!session.isConnected()) return;

        // Attach current time to move for sync
        move.setTimes(whiteTimeSeconds, blackTimeSeconds);
        String json = new com.google.gson.Gson().toJson(move);
        System.out.println("[MainController] Sending MOVE: " + move + " from " + session.getPlayerName() + " to room " + session.getRoomId());

        // Sử dụng ChessClient để gửi
        session.getChessClient().send(new Message(session.getPlayerName(), "opponent", MessageType.MOVE, json));
        System.out.println("[MainController] MOVE sent successfully");
    }

    private void sendSystemEvent(String eventName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("event", eventName);
        sendSystemCommand(payload);
    }

    private void sendSystemCommand(JsonObject payload) {
        if (!session.isConnected()) return;
        // System message luôn gửi qua Server (Peer)
        session.getChessClient().send(new Message(session.getPlayerName(), "server", MessageType.SYSTEM, payload.toString()));
    }

    public void handleOpponentLeft(String sender) {
        javafx.application.Platform.runLater(() -> {
             javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
             alert.setTitle("Đối thủ thoát");
             alert.setHeaderText(sender + " đã thoát phòng!");
             alert.setContentText("Bạn thắng do đối thủ bỏ cuộc.");
             alert.showAndWait();
             
             // Xử lý thắng
             Color winner = session.getPlayerColor();
             game.setResult(winner == Color.WHITE ? GameResult.CHECKMATE_WHITE : GameResult.CHECKMATE_BLACK);
             moveItems.add("[Đối thủ thoát - Bạn thắng]");
             updateStatusLabels();
             stopTimer();
             setTip("Đối thủ đã thoát. Bạn thắng!");
             
             session.setOpponentName(null);
             applyPlayerNames();
        });
    }

    private boolean ensureConnected() {
        if (!session.isConnected()) {
            setTip("Bạn chưa kết nối tới máy chủ!");
            return false;
        }
        return true;
    }
    public void showNotification(String title, String header, String content) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void syncTimers(int whiteTime, int blackTime) {
        // Chỉ sync nếu chênh lệch đáng kể (>2s) để tránh giật cục do lag mạng
        if (Math.abs(this.whiteTimeSeconds - whiteTime) > 2) {
            this.whiteTimeSeconds = whiteTime;
        }
        if (Math.abs(this.blackTimeSeconds - blackTime) > 2) {
            this.blackTimeSeconds = blackTime;
        }
        updateTimerLabels();
    }
    
    public void attemptReconnect() {
        if (session.getChessClient() != null) {
            setTip("Mất kết nối! Đang tự động kết nối lại...");
            session.getChessClient().reconnect();
        }
    }
}

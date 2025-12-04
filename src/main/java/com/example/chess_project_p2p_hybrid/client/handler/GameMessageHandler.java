package com.example.chess_project_p2p_hybrid.client.handler;

import com.example.chess_project_p2p_hybrid.client.connection.Message;
import com.example.chess_project_p2p_hybrid.client.connection.MessageHandler;
import com.example.chess_project_p2p_hybrid.client.model.game.Game;
import com.example.chess_project_p2p_hybrid.client.model.game.GameResult;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.sync.GameSyncManager;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Xử lý messages liên quan đến game (MOVE, SYSTEM events).
 */
public class GameMessageHandler implements MessageHandler {
    private final GameSyncManager syncManager;
    private final ClientSession session;
    private final Gson gson = new Gson();
    private Consumer<String> statusCallback;
    private Runnable onGameStart;
    private Runnable onMoveReceived;
    private Runnable onGameViewSwitch;

    public GameMessageHandler(GameSyncManager syncManager, ClientSession session) {
        this.syncManager = syncManager;
        this.session = session;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setOnGameStart(Runnable callback) {
        this.onGameStart = callback;
    }

    public void setOnMoveReceived(Runnable callback) {
        this.onMoveReceived = callback;
    }

    public void setOnGameViewSwitch(Runnable callback) {
        this.onGameViewSwitch = callback;
    }

    private void updateStatus(String message) {
        if (statusCallback != null) {
            Platform.runLater(() -> statusCallback.accept(message));
        }
    }

    @Override
    public void onMove(Message message) {
        try {
            System.out.println("[GameMessageHandler] Received MOVE from " + message.getFrom() + " in room "
                    + session.getRoomId() + ": " + message.getContent());
            Move move = Move.fromJson(message.getContent());
            System.out.println("[GameMessageHandler] Parsed move: " + move + ", from player: " + message.getFrom()
                    + ", my name: " + session.getPlayerName());

            Platform.runLater(() -> {
                // Kiểm tra xem có phải move từ chính mình không (echo từ server)
                if (session.isConnected() && message.getFrom().equals(session.getPlayerName())) {
                    System.out.println("[GameMessageHandler] Ignoring own move (echo from server)");
                    return;
                }

                // Đảm bảo đây là move từ đối thủ
                if (session.isConnected() && !message.getFrom().equals(session.getOpponentName())) {
                    System.out.println("[GameMessageHandler] Warning: Move from unknown player: " + message.getFrom()
                            + ", expected: " + session.getOpponentName());
                }

                System.out.println("[GameMessageHandler] Before apply - Turn: " + syncManager.getGame().getTurn()
                        + ", My color: " + session.getPlayerColor());
                boolean applied = syncManager.applyRemoteMove(move, message.getFrom());
                System.out.println("[GameMessageHandler] After apply - Applied: " + applied + ", Turn: "
                        + syncManager.getGame().getTurn());

                if (applied) {
                    // Sync timers if provided
                    if (move.getWhiteTime() != -1 && move.getBlackTime() != -1) {
                         if (session.getMainController() != null) {
                             javafx.application.Platform.runLater(() -> 
                                 session.getMainController().syncTimers(move.getWhiteTime(), move.getBlackTime())
                             );
                         }
                    }

                    // Update UI - onMoveReceived callback đã bao gồm updateStatusLabels()
                    if (onMoveReceived != null) {
                        onMoveReceived.run();
                    }
                    System.out.println("[GameMessageHandler] UI updated for remote move");
                    
                    // Check game result for notification
                    GameResult result = syncManager.getGame().getResult();
                    if (result != GameResult.ONGOING && session.getMainController() != null) {
                        String title = "Kết thúc";
                        String header = "Ván đấu kết thúc";
                        String content = switch (result) {
                            case CHECKMATE_WHITE -> "Trắng bị chiếu hết. Đen thắng!";
                            case CHECKMATE_BLACK -> "Đen bị chiếu hết. Trắng thắng!";
                            case STALEMATE -> "Hòa do bế tắc (Stalemate).";
                            case DRAW_BY_AGREEMENT -> "Hòa do thỏa thuận.";
                            case DRAW_50_MOVES -> "Hòa do quy tắc 50 nước đi.";
                            case DRAW_THREEFOLD_REPETITION -> "Hòa do lặp lại 3 lần.";
                            default -> "Ván đấu kết thúc.";
                        };
                        session.getMainController().showNotification(title, header, content);
                    }
                } else {
                    System.err.println("[GameMessageHandler] Failed to apply move: " + move);
                }
            });
        } catch (Exception e) {
            System.err.println("[GameMessageHandler] Error processing move: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Lỗi xử lý nước đi: " + e.getMessage());
        }
    }

    @Override
    public void onChat(Message message) {
        // Chat được xử lý bởi ChatController
        if (session.getChatController() != null) {
            javafx.application.Platform.runLater(
                    () -> session.getChatController().appendMessage(message.getFrom() + ": " + message.getContent()));
        }
    }

    @Override
    public void onSystem(Message message) {
        try {
            JsonObject json = gson.fromJson(message.getContent(), JsonObject.class);
            String event = json.has("event") ? json.get("event").getAsString() : "";

            Platform.runLater(() -> handleSystemEvent(event, json, message.getFrom()));
        } catch (Exception e) {
            updateStatus("Lỗi xử lý thông điệp hệ thống: " + e.getMessage());
        }
    }

    @Override
    public void onError(Message message) {
        updateStatus("Lỗi từ máy chủ: " + message.getContent());
    }

    private void handleSystemEvent(String event, JsonObject payload, String sender) {
        // Ưu tiên lấy "from" từ payload (nếu có), nếu không thì dùng sender từ message
        String actualSender = payload.has("from") ? payload.get("from").getAsString() : sender;
        
        switch (event) {
            case "room_created" -> handleRoomCreated(payload);
            case "joined" -> handleJoined(payload);
            case "player_joined" -> handlePlayerJoined(payload);
            case "game_start" -> handleGameStart(payload);
            case "undo" -> handleUndo();
            case "new_game_request" -> handleNewGameRequest(actualSender);
            case "new_game_accept" -> handleNewGameAccept(actualSender);
            case "new_game_reject" -> handleNewGameReject(actualSender);
            case "new_game" -> handleNewGame(); // Legacy support
            case "resign" -> handleResign(actualSender);
            case "handshake" -> handleHandshake(payload);
            case "pause_request" -> handlePauseRequest(actualSender);
            case "pause_accept" -> handlePauseAccept();
            case "pause_reject" -> handlePauseReject();
            case "resume_request" -> handleResumeRequest(actualSender);
            case "resume_accept" -> handleResumeAccept();
            case "resume_reject" -> handleResumeReject();
            case "timeout" -> handleTimeout(actualSender);
            case "leave_room" -> handleLeaveRoom(actualSender);
            case "opponent_left" -> handleOpponentLeft();
            case "draw_offer" -> handleDrawOffer(actualSender);
            case "draw_accept" -> handleDrawAccept();
            case "draw_reject" -> handleDrawReject();
            case "error" -> handleError(payload);
            default -> {
            }
        }
    }

    private void handleRoomCreated(JsonObject payload) {
        if (payload.has("roomId")) {
            String roomId = payload.get("roomId").getAsString();
            session.setRoomId(roomId);
        }
        if (payload.has("color")) {
            session.setPlayerColor(Color.valueOf(payload.get("color").getAsString()));
        }
        if (payload.has("opponent")) {
            session.setOpponentName(payload.get("opponent").getAsString());
        }

        // Update UI
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                if (session.getRoomId() != null) {
                    session.getMainController().updateRoomId(session.getRoomId());
                }
                session.getMainController().updateMatchInfoTexts();
                session.getMainController().applyPlayerNames();
                
                // Reset game UI for new match
                session.getMainController().resetGameUI();
                
                // Ensure we are in game view
                if (onGameViewSwitch != null) {
                    onGameViewSwitch.run();
                }
            });
        }

        if (session.getOpponentName() != null) {
            updateStatus("Phòng: " + session.getRoomId() + ". Đối thủ: " + session.getOpponentName());
        } else {
            updateStatus("Phòng đã tạo: " + session.getRoomId() + ". Chờ người chơi thứ 2...");
        }
        System.out.println("Room created event received: " + session.getRoomId());
    }

    private void handleJoined(JsonObject payload) {
        // ... existing handleJoined logic seems unused or redundant if room_created covers it, 
        // but let's keep it safe or just rely on room_created. 
        // Actually, let's just leave handleJoined as is for now, it's not the main issue.
        // The issue is room_created not setting opponent.
        if (payload.has("roomId")) {
            String roomId = payload.get("roomId").getAsString();
            session.setRoomId(roomId);
            if (session.getMainController() != null) {
                javafx.application.Platform.runLater(() -> {
                    session.getMainController().updateRoomId(roomId);
                    session.getMainController().applyPlayerNames();
                });
            }
        }
        if (payload.has("color")) {
            Color assignedColor = Color.valueOf(payload.get("color").getAsString());
            session.setPlayerColor(assignedColor);
            System.out.println("[GameMessageHandler] Assigned color: " + assignedColor);
        }

        // Nếu có 2 người, sync game và chuyển sang game view
        if (payload.has("players") && payload.get("players").isJsonArray()) {
            int playerCount = payload.get("players").getAsJsonArray().size();
            if (playerCount >= 2) {
                // Lấy tên đối thủ từ danh sách players
                var playersArray = payload.get("players").getAsJsonArray();
                for (int i = 0; i < playersArray.size(); i++) {
                    String playerName = playersArray.get(i).getAsString();
                    if (!playerName.equals(session.getPlayerName())) {
                        session.setOpponentName(playerName);
                        break;
                    }
                }

                // Sync game state - đảm bảo turn = WHITE
                syncManager.syncGameState();
                if (session.getMainController() != null) {
                    javafx.application.Platform.runLater(() -> {
                        session.getMainController().applyPlayerNames();
                        // updateStatusLabels() sẽ được gọi qua onGameStart callback
                        if (onGameViewSwitch != null) {
                            onGameViewSwitch.run();
                        }
                    });
                }
                if (onGameStart != null) {
                    onGameStart.run();
                }
                
                // Start timer explicitly when 2 players are present
                if (session.getMainController() != null) {
                    javafx.application.Platform.runLater(() -> session.getMainController().startTimer());
                }

                // Thông báo rõ ràng ai đi trước
                if (session.getPlayerColor() == Color.WHITE) {
                    updateStatus("Đã vào phòng: " + session.getRoomId() + ". Bạn là Trắng - Bạn đi trước!");
                } else {
                    updateStatus(
                            "Đã vào phòng: " + session.getRoomId() + ". Bạn là Đen - Chờ đối thủ (Trắng) đi trước...");
                }
            } else {
                updateStatus("Đã vào phòng: " + session.getRoomId() + ". Chờ người chơi thứ 2...");
            }
        } else {
            updateStatus("Đã vào phòng: " + session.getRoomId());
        }
    }

    private void handlePlayerJoined(JsonObject payload) {
        if (payload.has("player")) {
            session.setOpponentName(payload.get("player").getAsString());
        }

        // Sync game khi có 2 người - đảm bảo turn = WHITE
        syncManager.syncGameState();

        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().applyPlayerNames();
                // updateStatusLabels() sẽ được gọi qua onGameStart callback
                // Chuyển sang game view khi có 2 người
                if (onGameViewSwitch != null) {
                    onGameViewSwitch.run();
                }
            });
        }

        if (onGameStart != null) {
            onGameStart.run();
        }
        
        // Start timer explicitly when 2 players are present
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> session.getMainController().startTimer());
        }

        // Thông báo rõ ràng ai đi trước
        if (session.getPlayerColor() == Color.WHITE) {
            updateStatus(session.getOpponentName() + " đã tham gia phòng. Bạn là Trắng - Bạn đi trước!");
        } else {
            updateStatus(
                    session.getOpponentName() + " đã tham gia phòng. Bạn là Đen - Chờ đối thủ (Trắng) đi trước...");
        }
    }

    private void handleGameStart(JsonObject payload) {
        if (payload.has("roomId") && payload.get("roomId").getAsString().equals(session.getRoomId())) {
            System.out.println("[GameMessageHandler] Game start signal received");
            System.out.println("[GameMessageHandler] My color: " + session.getPlayerColor());

            // Sync game state để đảm bảo cả 2 client có cùng board
            // Đảm bảo turn = WHITE (người đi trước)
            syncManager.syncGameState();

            if (session.getMainController() != null) {
                javafx.application.Platform.runLater(() -> {
                    // Chuyển sang game view nếu chưa ở đó
                    if (onGameViewSwitch != null) {
                        onGameViewSwitch.run();
                    }
                    if (onGameStart != null) {
                        onGameStart.run();
                    }
                    // updateStatusLabels() đã được gọi trong onGameStart callback
                    
                    // Start timer explicitly when game starts
                    session.getMainController().startTimer();
                });
            }

            // Thông báo rõ ràng ai đi trước
            if (session.getPlayerColor() == Color.WHITE) {
                updateStatus("Trận đấu bắt đầu! Bạn là Trắng - Bạn đi trước!");
            } else {
                updateStatus("Trận đấu bắt đầu! Bạn là Đen - Chờ đối thủ (Trắng) đi trước...");
            }
        }
    }

    private void handleUndo() {
        Game game = syncManager.getGame();

        System.out.println("[GameMessageHandler] Received undo signal from opponent");
        System.out.println("[GameMessageHandler] Current history size: " + game.getHistory().size());
        System.out.println("[GameMessageHandler] Current turn: " + game.getTurn());

        // Kiểm tra điều kiện
        if (game.getResult() != GameResult.ONGOING) {
            System.out.println("[GameMessageHandler] Cannot undo: Game already ended");
            updateStatus("Không thể hoàn tác: Ván cờ đã kết thúc.");
            return;
        }

        if (game.getHistory().isEmpty()) {
            System.out.println("[GameMessageHandler] Cannot undo: No moves in history");
            updateStatus("Không thể hoàn tác: Không còn nước đi nào.");
            return;
        }

        // Undo move
        boolean success = game.undoLastMove();
        if (success) {
            System.out.println(
                    "[GameMessageHandler] ✓ Remote undo successful - History size: " + game.getHistory().size());
            System.out.println("[GameMessageHandler] Turn after undo: " + game.getTurn());

            // Update UI - sử dụng callback thay vì gọi trực tiếp
            if (onMoveReceived != null) {
                javafx.application.Platform.runLater(() -> {
                    onMoveReceived.run();
                    // Hiển thị notification
                    if (session.getMainController() != null) {
                        session.getMainController().showNotification(
                            "Hoàn tác", 
                            "Đối thủ đã hoàn tác", 
                            "Đối thủ đã hoàn tác nước đi. Lượt hiện tại: " +
                                (game.getTurn() == Color.WHITE ? "Trắng" : "Đen")
                        );
                    }
                });
            }

            updateStatus("Đối thủ đã hoàn tác nước đi. Lượt hiện tại: " +
                    (game.getTurn() == Color.WHITE ? "Trắng" : "Đen"));
        } else {
            System.err.println("[GameMessageHandler] ✗ Failed to undo move");
            updateStatus("Lỗi: Không thể hoàn tác nước đi từ đối thủ.");
        }
    }

    private void handleNewGameRequest(String fromPlayer) {
        System.out.println("[GameMessageHandler] Received new game request from " + fromPlayer);

        // Kiểm tra xem có phải đối thủ không
        if (!fromPlayer.equals(session.getOpponentName())) {
            updateStatus("Người chơi " + fromPlayer + " yêu cầu ván mới.");
            return;
        }

        // Lưu request
        session.setNewGameRequestPending(true);
        session.setNewGameRequestFrom(fromPlayer);

        // Hiển thị dialog để đồng ý/từ chối
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Yêu cầu ván mới");
            alert.setHeaderText(fromPlayer + " muốn chơi ván mới");
            alert.setContentText("Bạn có muốn chơi lại từ đầu không?");

            javafx.scene.control.ButtonType acceptButton = new javafx.scene.control.ButtonType("Đồng ý",
                    javafx.scene.control.ButtonBar.ButtonData.YES);
            javafx.scene.control.ButtonType rejectButton = new javafx.scene.control.ButtonType("Từ chối",
                    javafx.scene.control.ButtonBar.ButtonData.NO);

            alert.getButtonTypes().setAll(acceptButton, rejectButton);

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == acceptButton) {
                    // Đồng ý - gửi accept và reset game
                    acceptNewGameRequest();
                } else {
                    // Từ chối
                    rejectNewGameRequest();
                }
            });
        });
    }

    private void acceptNewGameRequest() {
        if (!session.isConnected())
            return;

        // Gửi accept
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "new_game_accept");
        payload.addProperty("from", session.getPlayerName());

        // Gửi qua peer
        try {
            com.example.chess_project_p2p_hybrid.client.connection.Message msg = new com.example.chess_project_p2p_hybrid.client.connection.Message(
                    session.getPlayerName(),
                    "server",
                    com.example.chess_project_p2p_hybrid.client.connection.MessageType.SYSTEM,
                    payload.toString());
            session.getChessClient().send(msg);
        } catch (Exception e) {
            System.err.println("Error sending new_game_accept: " + e.getMessage());
        }

        // Reset game ngay khi accept
        session.clearNewGameRequest();
        syncManager.syncGameState();

        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                if (onGameStart != null) {
                    onGameStart.run();
                }
            });
        }

        updateStatus("Đã đồng ý ván mới. Ván cờ đã được khởi động lại!");
    }

    private void rejectNewGameRequest() {
        if (!session.isConnected())
            return;

        // Gửi reject
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "new_game_reject");
        payload.addProperty("from", session.getPlayerName());

        try {
            com.example.chess_project_p2p_hybrid.client.connection.Message msg = new com.example.chess_project_p2p_hybrid.client.connection.Message(
                    session.getPlayerName(),
                    "server",
                    com.example.chess_project_p2p_hybrid.client.connection.MessageType.SYSTEM,
                    payload.toString());
            session.getChessClient().send(msg);
        } catch (Exception e) {
            System.err.println("Error sending new_game_reject: " + e.getMessage());
        }

        session.clearNewGameRequest();
        updateStatus("Đã từ chối yêu cầu ván mới.");
    }

    private void handleNewGameAccept(String fromPlayer) {
        System.out.println("[GameMessageHandler] Received new game accept from " + fromPlayer);

        // Kiểm tra xem có phải đối thủ không
        if (!fromPlayer.equals(session.getOpponentName())) {
            updateStatus("Người chơi " + fromPlayer + " đã đồng ý ván mới.");
            return;
        }

        // Nếu mình đã gửi request, thì reset game
        if (session.isNewGameRequestPending()) {
            session.clearNewGameRequest();
            syncManager.syncGameState();

            if (session.getMainController() != null) {
                javafx.application.Platform.runLater(() -> {
                    if (onGameStart != null) {
                        onGameStart.run();
                    }
                    // Hiển thị notification
                    session.getMainController().showNotification(
                        "Ván mới", 
                        "Đã đồng ý! ✅", 
                        "Đối thủ đã đồng ý. Ván mới bắt đầu!"
                    );
                });
            }

            updateStatus("Đối thủ đã đồng ý. Ván mới bắt đầu!");
        } else {
            updateStatus("Đối thủ đã đồng ý ván mới.");
        }
    }

    private void handleNewGameReject(String fromPlayer) {
        System.out.println("[GameMessageHandler] Received new game reject from " + fromPlayer);

        if (!fromPlayer.equals(session.getOpponentName())) {
            updateStatus("Người chơi " + fromPlayer + " đã từ chối ván mới.");
            return;
        }

        // Nếu mình đã gửi request, clear nó
        if (session.isNewGameRequestPending()) {
            session.clearNewGameRequest();
            updateStatus("Đối thủ đã từ chối yêu cầu ván mới.");
            
            // Hiển thị notification
            if (session.getMainController() != null) {
                javafx.application.Platform.runLater(() -> {
                    session.getMainController().showNotification(
                        "Từ chối", 
                        "Không chơi ván mới ❌", 
                        "Đối thủ đã từ chối yêu cầu ván mới."
                    );
                });
            }
        } else {
            updateStatus("Đối thủ đã từ chối ván mới.");
        }
    }

    private void handleNewGame() {
        // Legacy support - tự động accept
        syncManager.syncGameState();
        updateStatus("Đối thủ đã khởi động ván mới.");
    }

    private void handleResign(String player) {
        Game game = syncManager.getGame();

        // Kiểm tra xem có phải đối thủ resign không
        if (!player.equals(session.getOpponentName()) && !player.equals(session.getPlayerName())) {
            updateStatus("Người chơi " + player + " đã đầu hàng.");
            return;
        }

        // Set game result - đối thủ resign nghĩa là mình thắng
        Color winner = session.getPlayerColor(); // Mình thắng
        game.setResult(winner == Color.WHITE ? GameResult.CHECKMATE_WHITE : GameResult.CHECKMATE_BLACK);

        System.out.println("[GameMessageHandler] Player " + player + " resigned. Winner: " + winner);

        // Update UI - sử dụng callback và public method
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                // Thêm resign vào move history
                session.getMainController().getMoveItems().add("[Đầu hàng - " + player + "]");
                // Update UI qua callback
                if (onMoveReceived != null) {
                    onMoveReceived.run();
                }
                // Hiển thị notification
                session.getMainController().showNotification(
                    "Kết thúc", 
                    player + " đã đầu hàng!", 
                    "Bạn giành chiến thắng! 🎉"
                );
            });
        }

        updateStatus(player + " đã đầu hàng. Bạn thắng!");
    }

    private void handleError(JsonObject payload) {
        String errorMsg = payload.has("message") ? payload.get("message").getAsString() : "Lỗi không xác định";
        updateStatus("Lỗi: " + errorMsg);
    }

    private void handleHandshake(JsonObject payload) {
        if (payload.has("name")) {
            String opponentName = payload.get("name").getAsString();
            session.setOpponentName(opponentName);
            updateStatus("Đã kết nối với đối thủ: " + opponentName);

            // Cập nhật UI MainController
            if (session.getMainController() != null) {
                javafx.application.Platform.runLater(() -> {
                    session.getMainController().updateMatchInfoTexts();
                    session.getMainController().applyPlayerNames(); // Cập nhật tên trên bàn cờ
                    session.getMainController().onPeerConnected(); // Kích hoạt bàn cờ
                });
            }
        }
    }

    private void handlePauseRequest(String sender) {
        if (session.getMainController() != null) {
            session.getMainController().onPauseRequest(sender);
        }
    }

    private void handlePauseAccept() {
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().setGamePaused(true);
                session.getMainController().showNotification(
                    "Tạm dừng", 
                    "Đã tạm dừng", 
                    "Đối thủ đã đồng ý tạm dừng trận đấu."
                );
            });
        }
        updateStatus("Đối thủ đã đồng ý tạm dừng.");
    }

    private void handlePauseReject() {
        updateStatus("Đối thủ từ chối tạm dừng.");
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().showNotification(
                    "Từ chối", 
                    "Không thể tạm dừng", 
                    "Đối thủ đã từ chối yêu cầu tạm dừng."
                );
            });
        }
    }

    private void handleResumeRequest(String sender) {
        if (session.getMainController() != null) {
            session.getMainController().onResumeRequest(sender);
        }
    }

    private void handleResumeAccept() {
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().setGamePaused(false);
                session.getMainController().showNotification(
                    "Tiếp tục", 
                    "Trận đấu tiếp tục", 
                    "Đối thủ đã đồng ý tiếp tục trận đấu."
                );
            });
        }
        updateStatus("Đối thủ đã đồng ý tiếp tục.");
    }

    private void handleResumeReject() {
        updateStatus("Đối thủ từ chối tiếp tục.");
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().showNotification(
                    "Từ chối", 
                    "Không thể tiếp tục", 
                    "Đối thủ đã từ chối yêu cầu tiếp tục."
                );
            });
        }
    }

    private void handleTimeout(String sender) {
        // Sender là người bị hết giờ
        Game game = syncManager.getGame();
        if (game.getResult() != GameResult.ONGOING) return;

        Color loserColor = sender.equals(session.getPlayerName()) ? session.getPlayerColor() : session.getPlayerColor().opposite();
        // Nếu sender là đối thủ, thì đối thủ hết giờ -> đối thủ thua
        if (sender.equals(session.getOpponentName())) {
            loserColor = session.getPlayerColor().opposite();
        }

        Color winner = loserColor.opposite();
        game.setResult(winner == Color.WHITE ? GameResult.CHECKMATE_WHITE : GameResult.CHECKMATE_BLACK);

        String msg = (loserColor == Color.WHITE ? "Trắng" : "Đen") + " hết giờ! " +
                (winner == Color.WHITE ? "Trắng" : "Đen") + " thắng.";

        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                session.getMainController().getMoveItems().add("[Hết giờ - " + sender + "]");
                if (onMoveReceived != null) onMoveReceived.run(); // Update UI
                // Hiển thị notification
                session.getMainController().showNotification(
                    "Kết thúc", 
                    "Hết giờ! ⏰", 
                    msg
                );
            });
        }
        updateStatus(msg);
    }


    private void handleLeaveRoom(String sender) {
        updateStatus(sender + " đã thoát phòng.");
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> {
                // Xử lý như resign
                handleResign(sender);
                session.setOpponentName(null);
                session.getMainController().applyPlayerNames();
            });
        }
    }

    private void handleOpponentLeft() {
        updateStatus("Đối thủ đã ngắt kết nối.");
        if (session.getMainController() != null) {
            session.getMainController().handleOpponentLeft("Đối thủ");
        }
    }

    private void handleDrawOffer(String sender) {
        if (session.getMainController() != null) {
            session.getMainController().onDrawOffer(sender);
        }
    }

    private void handleDrawAccept() {
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> session.getMainController().onDrawAccept());
        }
    }

    private void handleDrawReject() {
        if (session.getMainController() != null) {
            javafx.application.Platform.runLater(() -> session.getMainController().onDrawReject());
        }
    }
}

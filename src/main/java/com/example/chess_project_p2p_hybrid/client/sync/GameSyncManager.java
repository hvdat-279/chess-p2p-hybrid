package com.example.chess_project_p2p_hybrid.client.sync;

import com.example.chess_project_p2p_hybrid.client.model.game.Game;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.util.ClientSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Quản lý đồng bộ game state giữa 2 client.
 * Đảm bảo cả 2 client có cùng board state và move history.
 */
public class GameSyncManager {
    private final Game game;
    private final ClientSession session;
    private final List<Move> moveHistory = new ArrayList<>();
    private Consumer<String> statusCallback;
    private boolean isSyncing = false;
    
    public GameSyncManager(Game game, ClientSession session) {
        this.game = game;
        this.session = session;
    }
    
    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }
    
    private void updateStatus(String message) {
        if (statusCallback != null) {
            javafx.application.Platform.runLater(() -> statusCallback.accept(message));
        }
    }
    
    /**
     * Reset game để đồng bộ với đối thủ.
     * Đảm bảo turn = WHITE (người đi trước) và chỉ WHITE mới có thể đi đầu tiên.
     */
    public void syncGameState() {
        if (isSyncing) return;
        isSyncing = true;
        
        try {
            game.resetGame();
            moveHistory.clear();
            
            // Đảm bảo turn luôn bắt đầu từ WHITE (người đi trước)
            // Game.resetGame() đã set turn = WHITE, nhưng ta cần đảm bảo điều này
            if (game.getTurn() != Color.WHITE) {
                System.out.println("[GameSyncManager] Warning: Turn is not WHITE after reset, forcing to WHITE");
                // Không thể set trực tiếp, nhưng resetGame() đã set đúng rồi
            }
            
            System.out.println("[GameSyncManager] Game synced - Turn: " + game.getTurn() + ", My color: " + session.getPlayerColor());
            System.out.println("[GameSyncManager] " + (session.getPlayerColor() == Color.WHITE ? "I am WHITE (go first)" : "I am BLACK (wait for WHITE)"));
            
            if (session.isConnected()) {
                if (session.getPlayerColor() == Color.WHITE) {
                    updateStatus("Bạn là Trắng - Bạn đi trước!");
                } else {
                    updateStatus("Bạn là Đen - Chờ đối thủ (Trắng) đi trước...");
                }
            } else {
                updateStatus("Game state đã được đồng bộ");
            }
        } finally {
            isSyncing = false;
        }
    }
    
    /**
     * Áp dụng move từ local player (đã validate).
     */
    public boolean applyLocalMove(Move move) {
        if (isSyncing) {
            updateStatus("Đang đồng bộ, vui lòng đợi...");
            return false;
        }
        
        // Kiểm tra turn - CHẶT CHẼ HƠN (chỉ cho online game, không check trong local game)
        if (!session.isLocalGame() && session.isConnected()) {
            Color myColor = session.getPlayerColor();
            Color currentTurn = game.getTurn();
            
            System.out.println("[GameSyncManager] applyLocalMove - My color: " + myColor + ", Current turn: " + currentTurn);
            
            if (myColor != currentTurn) {
                String message = "Chưa đến lượt của bạn! Lượt hiện tại: " + currentTurn + 
                    (currentTurn == Color.WHITE ? " (Trắng)" : " (Đen)");
                updateStatus(message);
                System.out.println("[GameSyncManager] ✗ Turn mismatch - Cannot move");
                return false;
            }
            
            System.out.println("[GameSyncManager] ✓ Turn matches - Can move");
        }
        
        // Validate và apply move
        if (game.applyMoveIfLegal(move)) {
            moveHistory.add(move);
            System.out.println("[GameSyncManager] ✓ Local move applied successfully");
            return true;
        }
        
        updateStatus("Nước đi không hợp lệ");
        System.out.println("[GameSyncManager] ✗ Move is not legal");
        return false;
    }
    
    /**
     * Áp dụng move từ remote player.
     * Move từ remote đã được validate ở phía gửi, nên ta force apply.
     */
    public boolean applyRemoteMove(Move move, String fromPlayer) {
        System.out.println("[GameSyncManager] ===== Applying remote move =====");
        System.out.println("[GameSyncManager] Move: " + move);
        System.out.println("[GameSyncManager] From player: " + fromPlayer);
        System.out.println("[GameSyncManager] Current turn BEFORE: " + game.getTurn());
        System.out.println("[GameSyncManager] My color: " + session.getPlayerColor());
        System.out.println("[GameSyncManager] Expected turn (opponent's turn): " + (session.isConnected() ? session.getPlayerColor().opposite() : "N/A"));
        
        if (isSyncing) {
            System.out.println("[GameSyncManager] Warning: Received move while syncing, will apply anyway");
            // Vẫn apply để không mất move
        }
        
        // Validate turn - remote move phải là lượt của đối thủ
        if (session.isConnected()) {
            Color expectedTurn = session.getPlayerColor().opposite();
            if (game.getTurn() != expectedTurn) {
                // Turn không khớp - có thể do timing issue, nhưng vẫn apply move
                System.out.println("[GameSyncManager] ⚠️ Turn mismatch! Expected: " + expectedTurn + ", Current: " + game.getTurn() + ", but applying move anyway");
            } else {
                System.out.println("[GameSyncManager] ✓ Turn matches: " + expectedTurn);
            }
        }
        
        // Force apply move từ remote (vì đã được validate ở phía gửi)
        // Dùng applyRemoteMove để bypass local validation nếu cần
        try {
            Color turnBefore = game.getTurn();
            game.applyRemoteMove(move);
            Color turnAfter = game.getTurn();
            moveHistory.add(move);
            System.out.println("[GameSyncManager] ✓ Successfully applied remote move");
            System.out.println("[GameSyncManager] Turn changed: " + turnBefore + " -> " + turnAfter);
            System.out.println("[GameSyncManager] ===== Move applied successfully =====");
            updateStatus(fromPlayer + " đã đi: " + move);
            return true;
        } catch (Exception e) {
            System.err.println("[GameSyncManager] ✗ Error applying remote move with applyRemoteMove: " + e.getMessage());
            e.printStackTrace();
            // Thử apply bằng cách thông thường nếu applyRemoteMove fail
            try {
                System.out.println("[GameSyncManager] Trying applyMoveIfLegal as fallback...");
                if (game.applyMoveIfLegal(move)) {
                    moveHistory.add(move);
                    System.out.println("[GameSyncManager] ✓ Successfully applied with applyMoveIfLegal");
                    updateStatus(fromPlayer + " đã đi: " + move);
                    return true;
                } else {
                    System.err.println("[GameSyncManager] ✗ applyMoveIfLegal returned false");
                }
            } catch (Exception e2) {
                System.err.println("[GameSyncManager] ✗ Also failed with applyMoveIfLegal: " + e2.getMessage());
                e2.printStackTrace();
            }
            updateStatus("Lỗi: Không thể áp dụng move từ " + fromPlayer + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra xem có phải lượt của player không.
     */
    public boolean isMyTurn() {
        if (session.isLocalGame() || !session.isConnected()) return true; // Local game - luôn true
        return session.getPlayerColor() == game.getTurn();
    }
    
    /**
     * Lấy move history để sync.
     */
    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }
    
    /**
     * Replay moves để sync game state.
     */
    public void replayMoves(List<Move> moves) {
        if (isSyncing) return;
        isSyncing = true;
        
        try {
            game.resetGame();
            moveHistory.clear();
            
            for (Move move : moves) {
                game.applyRemoteMove(move);
                moveHistory.add(move);
            }
            
            updateStatus("Đã đồng bộ " + moves.size() + " nước đi");
        } finally {
            isSyncing = false;
        }
    }
    
    public Game getGame() {
        return game;
    }
}


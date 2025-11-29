package com.example.chess_project_p2p_hybrid.client.model.game;

import com.example.chess_project_p2p_hybrid.client.model.board.Board;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.model.piece.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.chess_project_p2p_hybrid.client.model.game.MoveType;
import com.example.chess_project_p2p_hybrid.client.model.piece.PieceType;

public class Game {
    private Board board;
    private Color turn = Color.WHITE;
    private GameResult result = GameResult.ONGOING;
    private List<Move> history = new ArrayList<>();
    
    // 50-move rule: đếm số bán nước đi (half-moves) không có bắt quân hoặc đi tốt
    private int halfMoveClock = 0;
    // 3-fold repetition: lưu trạng thái bàn cờ và số lần xuất hiện
    private Map<String, Integer> positionHistory = new HashMap<>();

    public Game() {
        board = new Board();
    }

    public Board getBoard() {
        return board;
    }

    public Color getTurn() {
        return turn;
    }

    public GameResult getResult() {
        return result;
    }
    
    public void setResult(GameResult result) {
        this.result = result;
    }

    /**
     * Trả về danh sách legal moves (đã lọc không để vua bị chiếu).
     *
     * @param from Vị trí quân cờ
     * @return Danh sách các nước đi hợp lệ (đã lọc)
     */
    public List<Move> legalMovesFor(Position from) {
        Piece p = board.getPiece(from);
        if (p == null || p.getColor() != turn) return List.of();

        List<Move> candidates = p.generateMoves(from, board);

        List<Move> legal = new ArrayList<>();
        for (Move m : candidates) {
            Board copy = board.clone();
            copy.applyMove(m);
            if (!copy.isInCheck(turn)) legal.add(m);
        }
        return legal;
    }

    /**
     * Tất cả legal moves cho player hiện tại (dùng để kiểm tra checkmate/stalemate)
     */
    public List<Move> allLegalMoves() {
        List<Move> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Position from = Position.of(r, c);
                Piece p = board.getPiece(from);
                if (p != null && p.getColor() == turn) {
                    all.addAll(legalMovesFor(from));
                }
            }
        return all;
    }

    /**
     * Áp dụng nước đi nếu nó hợp lệ.
     *
     * @param m Nước đi cần áp dụng
     * @return true nếu nước đi được áp dụng thành công
     */
    public boolean applyMoveIfLegal(Move m) {

        Piece p = board.getPiece(m.getFrom());
        if (p == null || p.getColor() != turn) return false;

        List<Move> legal = legalMovesFor(m.getFrom());
        if (!legal.contains(m)) return false;

        applyMoveInternal(m);
        return true;
    }

    private void applyMoveInternal(Move m) {
        // Check for 50-move rule reset (Pawn move or Capture)
        Piece p = board.getPiece(m.getFrom());
        boolean isPawnMove = p != null && p.getType() == PieceType.PAWN;
        boolean isCapture = m.getType() == MoveType.CAPTURE || m.getType() == MoveType.EN_PASSANT;
        
        if (isPawnMove || isCapture) {
            halfMoveClock = 0;
            positionHistory.clear(); // Quy tắc: 50-move reset thì thường repetition cũng bị ảnh hưởng (do không thể quay lại trạng thái trước khi tốt đã đi hoặc quân đã mất)
            // Tuy nhiên, 3-fold repetition tính trên toàn bộ ván, nhưng nếu tốt đi hoặc ăn quân thì trạng thái cũ không bao giờ lặp lại được nữa.
            // Nên clear map là hợp lý để tiết kiệm memory và đúng logic.
        } else {
            halfMoveClock++;
        }

        board.applyMove(m);
        history.add(m);
        turn = turn.opposite();
        
        // Update position history for 3-fold repetition
        String stateKey = generateStateKey();
        positionHistory.put(stateKey, positionHistory.getOrDefault(stateKey, 0) + 1);
        
        computeResult();
    }

    public boolean applyRemoteMove(Move move) {
        if (move == null) return false;

        // 1. Validate turn
        Piece p = board.getPiece(move.getFrom());
        if (p == null || p.getColor() != turn) {
            System.err.println("Invalid turn or empty piece for remote move: " + move);
            return false;
        }

        // 2. Validate move legality
        List<Move> legalMoves = legalMovesFor(move.getFrom());
        if (!legalMoves.contains(move)) {
            System.err.println("Illegal remote move detected: " + move);
            return false;
        }

        // 3. Apply
        applyMoveInternal(move);
        return true;
    }

    public boolean undoLastMove() {
        if (history.isEmpty()) return false;
        history.remove(history.size() - 1);
        Board fresh = new Board();
        Color currentTurn = Color.WHITE;
        List<Move> replay = new ArrayList<>(history);
        history.clear();
        this.board = fresh;
        this.board = fresh;
        this.turn = Color.WHITE;
        this.halfMoveClock = 0;
        this.positionHistory.clear();
        for (Move move : replay) {
            board.applyMove(move);
            history.add(move);
            currentTurn = currentTurn.opposite();
        }
        this.turn = currentTurn;
        computeResult();
        return true;
    }

    private void computeResult() {

        if (board.isInCheck(turn)) {
            var moves = allLegalMoves();
            if (moves.isEmpty()) {
                result = (turn == Color.WHITE) ? GameResult.CHECKMATE_BLACK : GameResult.CHECKMATE_WHITE;
                return;
            }
        } else {

            if (allLegalMoves().isEmpty()) {
                result = GameResult.STALEMATE;
                return;
            }
        }
        // Check 50-move rule
        if (halfMoveClock >= 100) { // 50 moves each side = 100 half moves
            result = GameResult.DRAW_50_MOVES;
            return;
        }

        // Check 3-fold repetition
        String stateKey = generateStateKey();
        if (positionHistory.getOrDefault(stateKey, 0) >= 3) {
            result = GameResult.DRAW_THREEFOLD_REPETITION;
            return;
        }

        result = GameResult.ONGOING;
    }

    private String generateStateKey() {
        StringBuilder sb = new StringBuilder();
        // 1. Piece placement
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(Position.of(r, c));
                if (p == null) {
                    sb.append("-");
                } else {
                    sb.append(p.getColor() == Color.WHITE ? "W" : "B");
                    sb.append(p.getType().toString().charAt(0));
                }
            }
        }
        // 2. Turn
        sb.append("|").append(turn);
        // 3. Castling rights (check hasMoved of Kings and Rooks)
        // Simple check: just append hasMoved status of relevant pieces
        // White King
        Piece wk = board.getPiece(Position.of(7, 4));
        sb.append("|WK").append(wk != null && !wk.hasMoved() ? "1" : "0");
        // White Rooks
        Piece wr1 = board.getPiece(Position.of(7, 0));
        sb.append("WR1").append(wr1 != null && !wr1.hasMoved() ? "1" : "0");
        Piece wr2 = board.getPiece(Position.of(7, 7));
        sb.append("WR2").append(wr2 != null && !wr2.hasMoved() ? "1" : "0");
        // Black King
        Piece bk = board.getPiece(Position.of(0, 4));
        sb.append("|BK").append(bk != null && !bk.hasMoved() ? "1" : "0");
        // Black Rooks
        Piece br1 = board.getPiece(Position.of(0, 0));
        sb.append("BR1").append(br1 != null && !br1.hasMoved() ? "1" : "0");
        Piece br2 = board.getPiece(Position.of(0, 7));
        sb.append("BR2").append(br2 != null && !br2.hasMoved() ? "1" : "0");
        
        // 4. En Passant target
        Position ep = board.getEnPassantTarget();
        sb.append("|EP").append(ep != null ? ep.toString() : "None");
        
        return sb.toString();
    }

    public List<Move> getHistory() {
        return history;
    }

    public void resetGame() {
        this.board = new Board();
        this.turn = Color.WHITE;
        this.result = GameResult.ONGOING;
        this.history.clear();
        this.halfMoveClock = 0;
        this.positionHistory.clear();
    }
}

package com.example.chess_project_p2p_hybrid.client.model.game;

import com.example.chess_project_p2p_hybrid.client.model.board.Board;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.model.piece.Piece;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Board board;
    private Color turn = Color.WHITE;
    private GameResult result = GameResult.ONGOING;
    private List<Move> history = new ArrayList<>();

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
        board.applyMove(m);
        history.add(m);
        turn = turn.opposite();
        computeResult();
    }

    public boolean applyRemoteMove(Move move) {
        // dùng cho nước đi nhận từ network, giả định đã hợp lệ
        board.applyMove(move);
        history.add(move);
        turn = turn.opposite();
        computeResult();
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
        this.turn = Color.WHITE;
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
        result = GameResult.ONGOING;
    }

    public List<Move> getHistory() {
        return history;
    }

    public void resetGame() {
        this.board = new Board();
        this.turn = Color.WHITE;
        this.result = GameResult.ONGOING;
        this.history.clear();
    }
}

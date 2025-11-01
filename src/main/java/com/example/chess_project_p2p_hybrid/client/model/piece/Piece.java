package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.List;

public abstract class Piece implements Cloneable {
    protected final Color color;
    protected final PieceType type;
    protected boolean hasMoved = false;

    public Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean moved) {
        this.hasMoved = moved;
    }

    /**
     * Sinh các pseudo-legal moves (cần lọc check sau)
     *
     * @param from       vị trí hiện tại
     * @param boardState tham chiếu để đọc ô
     */
    public abstract List<Move> generateMoves(Position from, BoardView boardState);

    @Override
    public Piece clone() {
        try {
            return (Piece) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

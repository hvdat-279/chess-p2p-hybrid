package com.example.chess_project_p2p_hybrid.client.model.game;

import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.piece.PieceType;

import java.io.Serializable;
import java.util.Objects;

public class Move implements Serializable {

    private final Position from;
    private final Position to;
    private final MoveType type;
    private final PieceType promotionTo; // null nếu ko phải promotion

    public Move(Position from, Position to, MoveType type, PieceType promotionTo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.promotionTo = promotionTo;
    }

    // Bình thường hoặc phong cấp.
    public static Move normal(Position f, Position t, PieceType promotion) {
        return new Move(f, t, promotion == null ? MoveType.NORMAL : MoveType.PROMOTION, promotion);
    }

    // Ăn quân hoặc phong cấp khi ăn.
    public static Move capture(Position f, Position t, PieceType promotion) {
        return new Move(f, t, promotion == null ? MoveType.CAPTURE : MoveType.PROMOTION, promotion);
    }

    // Tốt đi hai ô đầu.
    public static Move doublePawn(Position f, Position t) {
        return new Move(f, t, MoveType.DOUBLE_PAWN, null);
    }

    // Ăn tốt qua đường.
    public static Move enPassant(Position f, Position t) {
        return new Move(f, t, MoveType.EN_PASSANT, null);
    }

    // Nhập thành cánh vua.
    public static Move castleKingSide(Position f, Position t) {
        return new Move(f, t, MoveType.CASTLE_KINGSIDE, null);
    }

    // Nhập thành cánh hậu.
    public static Move castleQueenSide(Position f, Position t) {
        return new Move(f, t, MoveType.CASTLE_QUEENSIDE, null);
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public MoveType getType() {
        return type;
    }

    public PieceType getPromotionTo() {
        return promotionTo;
    }

    @Override
    public String toString() {
        return type + " " + from + "->" + to + (promotionTo != null ? ("=" + promotionTo) : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return Objects.equals(from, m.from) && Objects.equals(to, m.to) && type == m.type && promotionTo == m.promotionTo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, type, promotionTo);
    }
}

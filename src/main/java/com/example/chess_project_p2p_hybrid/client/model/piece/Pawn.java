package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho quân Tốt (Pawn) trong bàn cờ vua.
 */
public class Pawn extends Piece {
    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    /**
     * Sinh tất cả các nước đi hợp lệ cho quân Tốt dựa trên vị trí hiện tại.
     * Quân Tốt có quy tắc di chuyển và ăn quân đặc biệt:
     *   Đi thẳng về phía trước (không ăn quân).
     *   Ở vị trí xuất phát, có thể đi 2 ô nếu không bị cản.
     *   Ăn chéo 1 ô theo hướng tiến.
     *   Có thể phong cấp (promotion) khi tới hàng cuối.
     *   Có thể bắt chéo (en passant) trong trường hợp đặc biệt.
     *
     *
     * @param from Vị trí hiện tại của quân Tốt.
     * @param b    Bàn cờ (BoardView) — chứa thông tin quân cờ, lượt chơi, en passant, v.v.
     * @return Danh sách các nước đi hợp lệ (List<Move>).
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();

        // Hướng di chuyển: Trắng đi lên (-1), Đen đi xuống (+1)
        int dir = (color == Color.WHITE) ? -1 : 1;

        // Hàng bắt đầu (chưa di chuyển): hàng 6 với Trắng, hàng 1 với Đen
        int startRow = (color == Color.WHITE) ? 6 : 1;

        // Hàng phong cấp: hàng 0 (Trắng) hoặc hàng 7 (Đen)
        int promotionRow = (color == Color.WHITE) ? 0 : 7;

        // Di chuyển thẳng một ô phía trước
        Position one = Position.of(from.row() + dir, from.col());
        if (one.isValid() && b.isEmpty(one)) {
            boolean promote = one.row() == promotionRow;// đến hàng cuối → phong cấp
            moves.add(Move.normal(from, one, promote ? PieceType.QUEEN : null));
            // Hai ô từ vị trí xuất phát
            Position two = Position.of(from.row() + 2 * dir, from.col());
            if (from.row() == startRow && two.isValid() && b.isEmpty(two)) {
                moves.add(Move.doublePawn(from, two));
            }
        }

        // Ăn chéo
        int[] dc = {-1, 1};
        for (int d : dc) {
            Position cap = Position.of(from.row() + dir, from.col() + d);
            if (cap.isValid() && b.isEnemyPiece(cap, color)) {
                boolean promote = cap.row() == promotionRow;
                moves.add(Move.capture(from, cap, promote ? PieceType.QUEEN : null));
            }
        }

        // Bắt chéo
        Position epTarget = b.getEnPassantTarget();
        if (epTarget != null) {
            // Kiểm tra xem quân tốt hiện tại có thể bắt chéo tại vị trí này không
            if (epTarget.row() == from.row() + dir && Math.abs(epTarget.col() - from.col()) == 1) {
                moves.add(Move.enPassant(from, epTarget));
            }
        }

        return moves;
    }
}

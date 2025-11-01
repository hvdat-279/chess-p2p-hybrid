package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    /*
     * Vua chỉ đi được xung quanh nó.
     * */
    private static final int[][] DELTAS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    public King(Color color) {
        super(color, PieceType.KING);
    }


    /**
     * Sinh ra tất cả các nước đi khả dĩ (pseudo-legal) của quân vua.
     * Bao gồm cả di chuyển bình thường, ăn quân và nhập thành.
     * Vua chưa từng di chuyển.
     * Vua không đang bị chiếu.
     * Quân xe liên quan chưa di chuyển.
     * Không có quân nào đứng giữa vua và xe.
     * Các ô vua đi qua không bị chiếu.
     *
     * @param from vị trí hiện tại của vua
     * @param b    trạng thái bàn cờ
     * @return danh sách các nước đi hợp lệ tạm thời (sẽ được lọc check sau)
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();
        for (int[] d : DELTAS) {
            Position to = Position.of(from.row() + d[0], from.col() + d[1]);
            if (!to.isValid()) continue;
            if (b.isEmpty(to)) moves.add(Move.normal(from, to, null));
            else if (b.isEnemyPiece(to, color)) moves.add(Move.capture(from, to, null));
        }

        // Nhập thành
        if (!hasMoved() && !b.isInCheck(color)) {
            // King-side
            if (b.canCastleKingSide(color)) {
                moves.add(Move.castleKingSide(from, Position.of(from.row(), from.col() + 2)));
            }
            // Queen-side
            if (b.canCastleQueenSide(color)) {
                moves.add(Move.castleQueenSide(from, Position.of(from.row(), from.col() - 2)));
            }
        }

        return moves;
    }
}

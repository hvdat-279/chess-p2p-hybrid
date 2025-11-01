package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;


/**
 * Đại diện cho quân Tượng (Bishop) trong bàn cờ.
 * */
public class Bishop extends Piece {

    /**
     * 4 hướng di chuyển chéo của Tượng.
     *     (-1, -1) → Lên-trái
     *     (-1,  1) → Lên-phải
     *     ( 1, -1) → Xuống-trái
     *     ( 1,  1) → Xuống-phải
     * Mỗi phần tử là một vector (deltaRow, deltaCol).
     */
    private static final int[][] DIRS = {
            {-1, -1}, {-1, 1},
             {1, -1}, {1, 1}
    };

    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }


    /**
     * Sinh ra tất cả các nước đi khả dĩ (pseudo-legal moves) của Tượng.
     * Tượng có thể di chuyển theo 4 hướng chéo cho đến khi:
     *     Gặp rìa bàn cờ (vị trí không hợp lệ).
     *     Gặp quân đồng minh → dừng lại.
     *     Gặp quân địch → thêm nước ăn, rồi dừng lại.
     *
     * @param from vị trí hiện tại của Tượng
     * @param b    trạng thái bàn cờ
     * @return danh sách các nước đi hợp lệ tạm thời
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : DIRS) {
            int r = from.row() + dir[0], c = from.col() + dir[1];
            while (true) {
                Position to = Position.of(r, c);
                if (!to.isValid()) break;
                if (b.isEmpty(to)) {
                    moves.add(Move.normal(from, to, null));
                } else {
                    if (b.isEnemyPiece(to, color)) moves.add(Move.capture(from, to, null));
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return moves;
    }
}

package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho quân Xe (Rook) trong bàn cờ vua.
 */
public class Rook extends Piece {
    /**
     * 4 hướng di chuyển của Xe:
     *   Lên trên (-1, 0)
     *   Xuống dưới (1, 0)
     *   Sang trái (0, -1)
     *   Sang phải (0, 1)
     */
    private static final int[][] DIRS = {
            {-1, 0}, // lên
            {1, 0},  // xuống
            {0, -1}, // trái
            {0, 1}   // phải
    };

    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }

    /**
     * Sinh ra tất cả các nước đi hợp lệ (pseudo-legal moves) cho quân Xe.
     *   Xe di chuyển theo hàng ngang hoặc cột dọc, bao nhiêu ô tùy ý.
     *   Không thể nhảy qua quân khác.
     *   Có thể ăn quân địch nếu gặp quân đối phương trên đường đi.
     *
     * @param from vị trí hiện tại của Xe
     * @param b    trạng thái bàn cờ (BoardView)
     * @return danh sách các nước đi có thể thực hiện
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : DIRS) {
            int r = from.row() + dir[0], c = from.col() + dir[1];

            // Lặp cho đến khi ra khỏi bàn hoặc bị cản
            while (true) {
                Position to = Position.of(r, c);

                // Nếu vượt ra ngoài bàn cờ → dừng
                if (!to.isValid()) break;

                // Nếu ô trống → có thể di chuyển bình thường
                if (b.isEmpty(to)) moves.add(Move.normal(from, to, null));
                else {
                    // Nếu gặp quân đối phương → thêm nước ăn quân rồi dừng
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

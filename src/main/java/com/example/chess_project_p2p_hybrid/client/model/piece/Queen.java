package com.example.chess_project_p2p_hybrid.client.model.piece;

import com.example.chess_project_p2p_hybrid.client.model.board.BoardView;
import com.example.chess_project_p2p_hybrid.client.model.board.Position;
import com.example.chess_project_p2p_hybrid.client.model.game.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho quân Hậu (Queen) trong bàn cờ vua.
 */
public class Queen extends Piece {

    /**
     * 8 hướng di chuyển có thể của quân Hậu:
     *   4 hướng chéo: (-1,-1), (-1,1), (1,-1), (1,1)
     *   4 hướng thẳng: lên (-1,0), xuống (1,0), trái (0,-1), phải (0,1)
     */
    private static final int[][] DIRS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    public Queen(Color color) {
        super(color, PieceType.QUEEN);
    }

    /**
     * Sinh tất cả các nước đi hợp lệ cho quân Hậu.
     * Hậu là quân mạnh nhất, kết hợp khả năng di chuyển của Xe (ngang/dọc)
     * và Tượng (chéo).
     *   Có thể đi bất kỳ số ô theo 8 hướng khác nhau: lên, xuống, trái, phải, và 4 hướng chéo.
     *   Không thể nhảy qua quân khác.
     *   Có thể ăn quân địch nếu gặp quân đối phương trên đường đi.
     *
     * @param from vị trí hiện tại của quân Hậu
     * @param b    trạng thái bàn cờ (BoardView)
     * @return danh sách các nước đi có thể thực hiện (List<Move>)
     */
    @Override
    public List<Move> generateMoves(Position from, BoardView b) {
        List<Move> moves = new ArrayList<>();
        for (int[] dir : DIRS) {
            int r = from.row() + dir[0], c = from.col() + dir[1];

            // Lặp cho đến khi ra khỏi bàn hoặc gặp vật cản
            while (true) {
                Position to = Position.of(r, c);

                // Dừng nếu vị trí không hợp lệ (ngoài bàn cờ)
                if (!to.isValid()) break;

                // Nếu ô trống → di chuyển bình thường
                if (b.isEmpty(to)) moves.add(Move.normal(from, to, null));

                else {
                    // Nếu gặp quân địch → có thể ăn
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

package com.example.chess_project_p2p_hybrid.client.model.board;

import com.example.chess_project_p2p_hybrid.client.model.piece.Color;
import com.example.chess_project_p2p_hybrid.client.model.piece.Piece;

/**
 * Interface cung cấp cái nhìn về trạng thái bàn cờ cho Piece.
 * Các quân cờ dùng để kiểm tra:
 * - ô có trống hay không
 * - ô có chứa quân địch hay không
 * - vua có đang bị chiếu không
 * - điều kiện nhập thành
 * - en passant (bắt chéo)
 */
public interface BoardView {

    /**
     * Kiểm tra ô có trống không.
     *
     * @param p vị trí ô
     * @return true nếu ô không có quân
     */
    boolean isEmpty(Position p);

    /**
     * Kiểm tra ô có chứa quân địch không.
     *
     * @param p       vị trí ô
     * @param myColor màu quân đang xét
     * @return true nếu quân ở ô là quân địch
     */
    boolean isEnemyPiece(Position p, Color myColor);

    /**
     * Lấy quân ở vị trí p.
     *
     * @param p vị trí ô
     * @return Piece ở ô hoặc null nếu trống
     */
    Piece getPiece(Position p);

    /**
     * Lấy vị trí có thể thực hiện en passant (nếu có)
     *
     * @return Position ô en passant hoặc null nếu không có
     */
    Position getEnPassantTarget();

    /**
     * Kiểm tra vua của màu color có đang bị chiếu không.
     *
     * @param color màu vua cần kiểm tra
     * @return true nếu đang bị chiếu
     */
    boolean isInCheck(Color color);

    /**
     * Kiểm tra điều kiện nhập thành bên vua (King-side)
     *
     * @param color màu quân
     * @return true nếu có thể nhập thành
     */
    boolean canCastleKingSide(Color color);

    /**
     * Kiểm tra điều kiện nhập thành bên hậu (Queen-side)
     *
     * @param color màu quân
     * @return true nếu có thể nhập thành
     */
    boolean canCastleQueenSide(Color color);
}

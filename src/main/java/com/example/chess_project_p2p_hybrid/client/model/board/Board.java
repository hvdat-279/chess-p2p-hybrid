package com.example.chess_project_p2p_hybrid.client.model.board;

import com.example.chess_project_p2p_hybrid.client.model.game.Move;
import com.example.chess_project_p2p_hybrid.client.model.piece.*;

public class Board implements BoardView, Cloneable {
    // Mảng 8x8 lưu quân cờ, null = ô trống
    private final Piece[][] board = new Piece[8][8];

    // Ô mà quân tốt đối phương vừa đi 2 ô, dùng để bắt en passant
    private Position enPassantTarget = null;

    // Cờ nhập thành được xác định qua piece.hasMoved(), không dùng biến rời

    // Thiết lập vị trí ban đầu
    public Board() {
        setupInitial();
    }

    private void setupInitial() {
        // pawns
        for (int c = 0; c < 8; c++) {
            board[6][c] = new Pawn(Color.WHITE);
            board[1][c] = new Pawn(Color.BLACK);
        }
        // rooks
        board[7][0] = new Rook(Color.WHITE);
        board[7][7] = new Rook(Color.WHITE);
        board[0][0] = new Rook(Color.BLACK);
        board[0][7] = new Rook(Color.BLACK);
        // knights
        board[7][1] = new Knight(Color.WHITE);
        board[7][6] = new Knight(Color.WHITE);
        board[0][1] = new Knight(Color.BLACK);
        board[0][6] = new Knight(Color.BLACK);
        // bishops
        board[7][2] = new Bishop(Color.WHITE);
        board[7][5] = new Bishop(Color.WHITE);
        board[0][2] = new Bishop(Color.BLACK);
        board[0][5] = new Bishop(Color.BLACK);
        // queens & kings
        board[7][3] = new Queen(Color.WHITE);
        board[7][4] = new King(Color.WHITE);
        board[0][3] = new Queen(Color.BLACK);
        board[0][4] = new King(Color.BLACK);
        // others null
    }


    // Lấy quân cờ tại vị trí p
    public Piece getPiece(Position p) {
        return board[p.row()][p.col()];
    }

    // Đặt quân cờ p vào vị trí
    public void setPiece(Position p, Piece piece) {
        board[p.row()][p.col()] = piece;
    }

    // Xóa quân cờ tại vị trí
    public void clear(Position p) {
        board[p.row()][p.col()] = null;
    }

    // BoardView implementations
    @Override
    public boolean isEmpty(Position p) {
        return getPiece(p) == null;
    }

    @Override
    public boolean isEnemyPiece(Position p, Color myColor) {
        Piece piece = getPiece(p);
        return piece != null && piece.getColor() != myColor;
    }

    @Override
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    // Kiểm tra xem vua của màu color có đang bị chiếu không
    @Override
    public boolean isInCheck(Color color) {
        // find king pos
        Position kingPos = findKing(color);
        if (kingPos == null) return false; // không tìm thấy vua, lý thuyết không xảy ra

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor() != color) {
                    Position from = Position.of(r, c);
                    // Tránh đệ quy: riêng KING, tính ô tấn công trực tiếp (không xét nhập thành)
                    if (p.getType() == PieceType.KING) {
                        if (kingAdjacentAttacks(from, kingPos)) return true;
                    } else {
                        for (var mv : p.generateMoves(from, this)) {
                            if (mv.getTo().equals(kingPos)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Position findKing(Color color) {
        // Tìm vị trí vua theo màu
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) return Position.of(r, c);
            }
        return null;
    }

    // Kiểm tra điều kiện nhập thành bên vua
    @Override
    public boolean canCastleKingSide(Color color) {
        int row = (color == Color.WHITE) ? 7 : 0;

        Piece king = getPiece(Position.of(row, 4));
        Piece rook = getPiece(Position.of(row, 7));
        if (king == null || rook == null) return false;
        if (king.hasMoved() || rook.hasMoved()) return false;

        if (!isEmpty(Position.of(row, 5)) || !isEmpty(Position.of(row, 6))) return false;

        if (isInCheck(color)) return false;
        if (squareAttacked(Position.of(row, 5), color.opposite())) return false;
        if (squareAttacked(Position.of(row, 6), color.opposite())) return false;
        return true;
    }

    // Kiểm tra điều kiện nhập thành bên hậu
    @Override
    public boolean canCastleQueenSide(Color color) {
        int row = (color == Color.WHITE) ? 7 : 0;
        Piece king = getPiece(Position.of(row, 4));
        Piece rook = getPiece(Position.of(row, 0));
        if (king == null || rook == null) return false;
        if (king.hasMoved() || rook.hasMoved()) return false;
        if (!isEmpty(Position.of(row, 1)) || !isEmpty(Position.of(row, 2)) || !isEmpty(Position.of(row, 3)))
            return false;
        if (isInCheck(color)) return false;
        if (squareAttacked(Position.of(row, 3), color.opposite())) return false;
        if (squareAttacked(Position.of(row, 2), color.opposite())) return false;
        return true;
    }

    private boolean squareAttacked(Position pos, Color byColor) {
        // Kiểm tra xem ô pos có bị quân byColor tấn công không
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor() == byColor) {
                    Position from = Position.of(r, c);
                    if (p.getType() == PieceType.KING) {
                        if (kingAdjacentAttacks(from, pos)) return true;
                    } else {
                        for (var mv : p.generateMoves(from, this)) {
                            if (mv.getTo().equals(pos)) return true;
                        }
                    }
                }
            }
        return false;
    }

    private boolean kingAdjacentAttacks(Position from, Position target) {
        int[][] DELTAS = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
        };
        for (int[] d : DELTAS) {
            Position to = Position.of(from.row() + d[0], from.col() + d[1]);
            if (!to.isValid()) continue;
            if (to.equals(target)) return true;
        }
        return false;
    }

    // Áp dụng nước đi
    public void applyMove(Move m) {
        Position f = m.getFrom(), t = m.getTo();
        Piece p = getPiece(f);
        if (p == null) throw new IllegalStateException("No piece at from: " + f);

        switch (m.getType()) {
            case NORMAL -> {
                setPiece(t, p);
                clear(f);
                if (m.getPromotionTo() != null) {
                    // Phong cấp quân cờ
                    Piece promoted = switch (m.getPromotionTo()) {
                        case QUEEN -> new Queen(p.getColor());
                        case ROOK -> new Rook(p.getColor());
                        case BISHOP -> new Bishop(p.getColor());
                        case KNIGHT -> new Knight(p.getColor());
                        default -> new Queen(p.getColor());
                    };
                    setPiece(t, promoted);
                }
                p.setMoved(true);
                enPassantTarget = null;
            }
            case CAPTURE -> {
                setPiece(t, p);
                clear(f);
                p.setMoved(true);
                enPassantTarget = null;
            }
            case DOUBLE_PAWN -> {
                setPiece(t, p);
                clear(f);
                p.setMoved(true);
                // set en passant target to square behind pawn (where enemy pawn would land)
                int midRow = (f.row() + t.row()) / 2;
                enPassantTarget = Position.of(midRow, t.col());
            }
            case EN_PASSANT -> {
                // pawn moves to ep target, capture the pawn behind target
                setPiece(t, p);
                clear(f);
                int capturedRow = f.row(); // captured pawn is on same row as moving pawn originally
                clear(Position.of(capturedRow, t.col()));
                p.setMoved(true);
                enPassantTarget = null;
            }
            case CASTLE_KINGSIDE -> {
                // move king 2 to right, rook to left of king
                setPiece(t, p);
                clear(f);
                p.setMoved(true);
                int row = f.row();
                // rook from col 7 to col 5
                Piece rook = getPiece(Position.of(row, 7));
                setPiece(Position.of(row, 5), rook);
                clear(Position.of(row, 7));
                if (rook != null) rook.setMoved(true);
                enPassantTarget = null;
            }
            case CASTLE_QUEENSIDE -> {
                setPiece(t, p);
                clear(f);
                p.setMoved(true);
                int row = f.row();
                Piece rook = getPiece(Position.of(row, 0));
                setPiece(Position.of(row, 3), rook);
                clear(Position.of(row, 0));
                if (rook != null) rook.setMoved(true);
                enPassantTarget = null;
            }
            case PROMOTION -> {
                setPiece(t, p);
                clear(f);
                Piece promoted = switch (m.getPromotionTo()) {
                    case QUEEN -> new Queen(p.getColor());
                    case ROOK -> new Rook(p.getColor());
                    case BISHOP -> new Bishop(p.getColor());
                    case KNIGHT -> new Knight(p.getColor());
                    default -> new Queen(p.getColor());
                };
                setPiece(t, promoted);
                enPassantTarget = null;
            }
        }
    }

    @Override
    public Board clone() {
        // Tạo Board mới rồi sao chép từng phần tử để tránh chia sẻ mảng nội bộ
        Board copy = new Board();
        // Xóa thiết lập mặc định từ constructor
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy.board[r][c] = null;

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = this.board[r][c];
                copy.board[r][c] = (p == null) ? null : p.clone();
            }

        copy.enPassantTarget = this.enPassantTarget;
        return copy;
    }
}

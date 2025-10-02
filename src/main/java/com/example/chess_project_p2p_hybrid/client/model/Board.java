package com.example.chess_project_p2p_hybrid.client.model;

public class Board {
    private Piece[][] grid;

    public Board() {
        grid = new Piece[8][8];
        resetBoard();
    }

    public void resetBoard() {
        // Xóa sạch bàn cờ
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                grid[r][c] = null;
            }
        }

        // --- Quân Trắng ---
        grid[0][0] = new Piece(PieceType.ROOK, true);
        grid[0][1] = new Piece(PieceType.KNIGHT, true);
        grid[0][2] = new Piece(PieceType.BISHOP, true);
        grid[0][3] = new Piece(PieceType.QUEEN, true);
        grid[0][4] = new Piece(PieceType.KING, true);
        grid[0][5] = new Piece(PieceType.BISHOP, true);
        grid[0][6] = new Piece(PieceType.KNIGHT, true);
        grid[0][7] = new Piece(PieceType.ROOK, true);

        for (int c = 0; c < 8; c++) {
            grid[1][c] = new Piece(PieceType.PAWN, true);
        }

        // --- Quân Đen ---
        grid[7][0] = new Piece(PieceType.ROOK, false);
        grid[7][1] = new Piece(PieceType.KNIGHT, false);
        grid[7][2] = new Piece(PieceType.BISHOP, false);
        grid[7][3] = new Piece(PieceType.QUEEN, false);
        grid[7][4] = new Piece(PieceType.KING, false);
        grid[7][5] = new Piece(PieceType.BISHOP, false);
        grid[7][6] = new Piece(PieceType.KNIGHT, false);
        grid[7][7] = new Piece(PieceType.ROOK, false);

        for (int c = 0; c < 8; c++) {
            grid[6][c] = new Piece(PieceType.PAWN, false);
        }
    }

    public Piece getPiece(int row, int col) {
        return grid[row][col];
    }

    public void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        Piece moving = grid[fromRow][fromCol];
        if (moving == null) return;

        // Ăn quân (nếu có) hoặc di chuyển
        grid[toRow][toCol] = moving;
        grid[fromRow][fromCol] = null;
    }

    public void printBoard() {
        for (int r = 7; r >= 0; r--) {
            for (int c = 0; c < 8; c++) {
                if (grid[r][c] == null) {
                    System.out.print("-- ");
                } else {
                    System.out.print(grid[r][c].toString().charAt(0) + "" + grid[r][c].getType().toString().charAt(0) + " ");
                }
            }
            System.out.println();
        }
    }

    public void printBoard2() {
        System.out.println("    a   b   c   d   e   f   g   h");
        System.out.println("  +---+---+---+---+---+---+---+---+");
        for (int r = 7; r >= 0; r--) {
            System.out.print((r + 1) + " |"); // số hàng 1–8
            for (int c = 0; c < 8; c++) {
                if (grid[r][c] == null) {
                    System.out.print("   |");
                } else {
                    String pieceStr = grid[r][c].toString();
                    // in quân ngắn gọn: W-P (white pawn), B-K (black king)...
                    System.out.print(" " + pieceStr + " |");
                }
            }
            System.out.println(" " + (r + 1));
            System.out.println("  +---+---+---+---+---+---+---+---+");
        }
        System.out.println("    a   b   c   d   e   f   g   h");
    }

}
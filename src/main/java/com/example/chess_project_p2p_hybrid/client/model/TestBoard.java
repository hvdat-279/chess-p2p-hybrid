package com.example.chess_project_p2p_hybrid.client.model;

public class TestBoard {
    public static void main(String[] args) {
        Board board = new Board();
        board.printBoard();

        System.out.println("\nDi chuyển tốt trắng từ (1,0) -> (3,0)\n");
        board.movePiece(1,0,3,0);
        board.printBoard();
    }
}

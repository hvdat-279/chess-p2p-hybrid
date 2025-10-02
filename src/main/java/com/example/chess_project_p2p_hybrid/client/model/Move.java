package com.example.chess_project_p2p_hybrid.client.model;

public class Move {
    private int fromX, fromY;
    private int toX, toY;

    public Move(int fromX, int fromY, int toX, int toY) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }
}
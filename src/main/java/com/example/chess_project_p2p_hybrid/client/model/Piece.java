package com.example.chess_project_p2p_hybrid.client.model;

public class Piece {
    private PieceType type;
    private boolean white;

    public Piece(PieceType type, boolean white) {
        this.type = type;
        this.white = white;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }

    @Override
    public String toString() {
        return (white ? "W" : "B") + "-" + type;
    }
}
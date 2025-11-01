package com.example.chess_project_p2p_hybrid.client.model.piece;

public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

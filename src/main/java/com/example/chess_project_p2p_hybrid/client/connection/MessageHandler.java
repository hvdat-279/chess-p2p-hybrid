package com.example.chess_project_p2p_hybrid.client.connection;

public interface MessageHandler {
    default void onMove(Message message) {}
    default void onChat(Message message) {}
    default void onSystem(Message message) {}
    default void onError(Message message) {}
}
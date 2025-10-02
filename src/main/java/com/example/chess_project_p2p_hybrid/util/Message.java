package com.example.chess_project_p2p_hybrid.util;

public class Message {
    private String from;
    private String to;
    private MessageType type;
    private String content;

    public Message(String from, String to, MessageType type, String content) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.content = content;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }

}

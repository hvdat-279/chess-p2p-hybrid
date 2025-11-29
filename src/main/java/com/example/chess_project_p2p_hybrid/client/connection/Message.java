package com.example.chess_project_p2p_hybrid.client.connection;

import com.google.gson.Gson;

public class Message {
    private static final Gson GSON = new Gson();

    private String from;      // Người gửi
    private String to;        // Người nhận ("server", "all", hoặc tên đối thủ)
    private MessageType type; // Loại tin nhắn
    private String content;   // Nội dung (JSON string hoặc text)

    public Message() {
    }

    public Message(String from, String to, MessageType type, String content) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    @Override
    public String toString() {
        return "Message{type=" + type + ", from='" + from + "', content='" + content + "'}";
    }
}

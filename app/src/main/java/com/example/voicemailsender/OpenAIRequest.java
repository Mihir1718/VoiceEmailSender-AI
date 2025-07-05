package com.example.voicemailsender;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private int max_tokens;

    public OpenAIRequest(String model, List<Message> messages, int max_tokens) {
        this.model = model;
        this.messages = messages;
        this.max_tokens = max_tokens;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

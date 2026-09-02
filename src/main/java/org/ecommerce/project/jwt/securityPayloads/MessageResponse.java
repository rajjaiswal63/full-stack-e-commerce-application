package org.ecommerce.project.jwt.securityPayloads;

public class MessageResponse {
    public String message;
    public MessageResponse(String message) {
        this.message = message;
    }
    public MessageResponse() {
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}

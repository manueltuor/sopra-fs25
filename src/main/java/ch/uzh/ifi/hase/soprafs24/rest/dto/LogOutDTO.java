package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LogOutDTO {

    private String token;
    private Long userId;
    
    public String getToken() {
        return token;
    }

    public Long getId() {
        return userId;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setId(Long id) {
        this.userId = id;
    }
}
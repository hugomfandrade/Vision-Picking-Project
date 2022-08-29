package org.gtp.cocacolaproject.data;

public class LoginData {

    private String mUsername;
    private String mPassword;

    public LoginData(String username, String password) {
        this.mUsername = username;
        this.mPassword = password;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        this.mPassword = password;
    }

    public static class Entry {
        public static final String USERNAME = "Username";
        public static final String PASSWORD = "Password";
    }
}

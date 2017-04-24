package it.polito.mad.easysplit;

public class LoginException extends Exception {
    private String mUsername, mPassword;

    public LoginException(String username, String password) {
        this(username, password, "", null);
    }

    public LoginException(String username, String password, String message, Exception cause) {
        super(message, cause);
        mUsername = username;
        mPassword = password;
    }

    public String getmUsername() {
        return mUsername;
    }

    public String getmPassword() {
        return mPassword;
    }
}

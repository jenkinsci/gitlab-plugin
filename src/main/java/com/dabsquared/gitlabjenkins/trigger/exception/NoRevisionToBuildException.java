package com.dabsquared.gitlabjenkins.trigger.exception;

/**
 * @author Robin Müller
 */
public class NoRevisionToBuildException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoRevisionToBuildException() {
    }

    public NoRevisionToBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoRevisionToBuildException(String message) {
        super(message);
    }

    public NoRevisionToBuildException(Throwable cause) {
        super(cause);
    }

}

package com.example.java26.budget;

public class FileFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    public FileFormatException(int lineNumber, String message) {
        this(lineNumber, message, null);
    }
    public FileFormatException(int lineNumber, String message, Throwable cause) {
        super("Rad " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }
    public int getLineNumber() {
        return lineNumber;
    }
}

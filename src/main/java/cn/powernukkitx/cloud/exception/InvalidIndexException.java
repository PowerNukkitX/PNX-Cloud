package cn.powernukkitx.cloud.exception;

public class InvalidIndexException extends IllegalArgumentException {
    public InvalidIndexException() {
    }

    public InvalidIndexException(String s) {
        super(s);
    }

    public InvalidIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidIndexException(Throwable cause) {
        super(cause);
    }
}

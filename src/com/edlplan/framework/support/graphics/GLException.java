package com.edlplan.framework.support.graphics;

public class GLException extends RuntimeException {
    public GLException(String msg) {
        super(msg);
    }

    public GLException(String msg, Throwable cause) {
        super(msg, cause);
    }
}


package com.bitwig.extensions.framework.di;

public class DiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DiException(final String string) {
        super(string);
    }

    public DiException(final String message, final Throwable cause) {
        super(message, cause);
    }

}

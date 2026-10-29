package com.sigebox.central.db;

/** Error de persistencia sin checked exceptions: la UI y el servidor no pueden seguir sin la base. */
public final class DbException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}

package comdis;

import org.sqlite.SQLiteException;

public class PtpException extends Exception {
    public PtpException(String message) {
        super(message);
    }

    public static void logError(Throwable error) {
        if (error instanceof SQLiteException sqlError) {
            // Mostrar el codigo de error extendido con su representacion textual
            System.err.printf("[ERROR] SQLite Exception (code: %d): %s\n", sqlError.getResultCode().code, sqlError.getResultCode().toString());
        } else {
            System.err.printf("[ERROR] %s: %s\n", error.getClass().getName(), error.getMessage());
        }

        error.printStackTrace();
    }
}

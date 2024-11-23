package comdis;

public class AlreadyExistsException extends PtpException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}

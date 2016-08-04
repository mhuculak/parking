package parking.opencv;

public class DuplicateTopologyException extends Exception {

	public DuplicateTopologyException() {

	}

    public DuplicateTopologyException(String message) {
        super(message);
    }

    public DuplicateTopologyException(Throwable cause) {
        super(cause);
    }
}
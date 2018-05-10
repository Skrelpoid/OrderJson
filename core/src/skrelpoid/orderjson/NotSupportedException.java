package skrelpoid.orderjson;

import java.io.IOException;

public class NotSupportedException extends IOException {

	private static final long serialVersionUID = 5863814260599518183L;

	public NotSupportedException() {
		super();
	}

	public NotSupportedException(String message, Throwable cause) {
		super(message, cause);

	}

	public NotSupportedException(String message) {
		super(message);

	}

	public NotSupportedException(Throwable cause) {
		super(cause);

	}

}

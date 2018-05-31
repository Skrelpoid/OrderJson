package skrelpoid.orderjson;

import java.io.IOException;
import java.io.OutputStream;

import com.badlogic.gdx.Gdx;

public class ConsoleStream extends OutputStream {

	private OrderJson app;
	private char[] buffer;
	private int index;

	// Stream to redirect System.out and System.err
	public ConsoleStream(OrderJson app, int capacity) {
		this.app = app;
		buffer = new char[capacity];
		index = 0;
	}

	// probably not used, maybe doesnt even work
	@Override
	public void write(int b) throws IOException {
		char c = (char) b;
		buffer[index++] = c;
		writeIfNeeded();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		// get the string with the specified offset and length
		String str = new String(b, off, len);
		// write the string to the console on the next frame (to ensure thread
		// safety)
		Gdx.app.postRunnable(new ConsoleWrite(str));

	}

	private void writeIfNeeded() {
		if (index >= buffer.length) {
			index = 0;
			Gdx.app.postRunnable(new ConsoleWrite(String.copyValueOf(buffer)));
		}
	}

	public class ConsoleWrite implements Runnable {
		private String str;

		public ConsoleWrite(String str) {
			this.str = str;
		}

		@Override
		public void run() {
			// append the text and scroll top the bottom of the text box
			app.console.setText(app.console.getText().toString() + str);
			app.scroll.scrollTo(0, 0, 100, 100);
		}

	}

}

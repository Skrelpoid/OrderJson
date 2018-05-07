package skrelpoid.orderjson;

import java.io.IOException;
import java.io.OutputStream;

import com.badlogic.gdx.Gdx;

public class ConsoleStream extends OutputStream {

	private OrderJson app;
	private char[] buffer;
	private int index;

	public ConsoleStream(OrderJson app, int capacity) {
		this.app = app;
		buffer = new char[capacity];
		index = 0;
	}

	@Override
	public void write(int b) throws IOException {
		char c = (char) b;
		buffer[index++] = c;
		writeIfNeeded();
	}

	private void writeIfNeeded() {
		if (index >= buffer.length) {
			index = 0;
			Gdx.app.postRunnable(new ConsoleWrite(String.copyValueOf(buffer)));
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		String str = new String(b, off, len);
		Gdx.app.postRunnable(new ConsoleWrite(str));

	}

	public class ConsoleWrite implements Runnable {
		String str;

		public ConsoleWrite(String str) {
			this.str = str;
		}

		@Override
		public void run() {
			app.console.setText(app.console.getText().toString() + str);
			app.scroll.scrollTo(0, 0, 100, 100);
		}

	}

}

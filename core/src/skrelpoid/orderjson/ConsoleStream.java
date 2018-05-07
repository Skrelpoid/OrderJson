package skrelpoid.orderjson;

import java.io.IOException;
import java.io.OutputStream;

import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

public class ConsoleStream extends OutputStream {

	private TextArea console;
	private char[] buffer;
	private int index;

	public ConsoleStream(TextArea console, int capacity) {
		this.console = console;
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
			console.appendText(String.copyValueOf(buffer));
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		String str = new String(b, off, len);
		console.appendText(str);
	}

}

package skrelpoid.orderjson.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import skrelpoid.orderjson.OrderJson;

public class DesktopLauncher {
	public static void main(String[] arg) {
		// Launch OrderJson on Desktop in a window 800x480
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 800;
		config.height = 480;
		new LwjglApplication(new OrderJson(), config);
	}
}

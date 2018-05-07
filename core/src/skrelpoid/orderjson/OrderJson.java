package skrelpoid.orderjson;

import java.io.PrintStream;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class OrderJson implements ApplicationListener {

	public static final String SKIN = "uiskin.json";

	public Skin skin;
	public Stage stage;
	public Viewport view;
	public Table table;
	public TextButton start;
	public CheckBox backup;
	public Label console;
	public ScrollPane scroll;
	public Sorter sorter;

	private String readme;

	@Override
	public void create() {
		load();
		buildGUI();
		Gdx.input.setInputProcessor(stage);
		setUpConsole();
	}

	private void setUpConsole() {
		System.setOut(new PrintStream(new ConsoleStream(this, 16)));
		System.setErr(new PrintStream(new ConsoleStream(this, 16)));
	}

	private void load() {
		skin = new Skin(Gdx.files.internal(SKIN));
		readme = Gdx.files.internal("OrderJsonREADME.txt").readString() + "\n";
	}

	private void buildGUI() {
		view = new ExtendViewport(800, 480);
		stage = new Stage(view);
		table = new Table();
		stage.addActor(table);

		start = new TextButton("Start", skin);
		backup = new CheckBox("Create Backup", skin);
		sorter = new Sorter(this);
		start.addListener(sorter);
		console = new Label(readme, skin);
		console.setWrap(true);
		scroll = new ScrollPane(console, skin);
		scroll.setOverscroll(false, false);
		Table consoleTable = new Table();
		Label consoleLabel = new Label("Console", skin);
		consoleTable.top();
		consoleTable.add(consoleLabel).growX().left();
		consoleTable.row();
		consoleTable.add(scroll).grow();

		Table buttonTable = new Table();
		buttonTable.add(start).grow();
		buttonTable.add(backup).fill().expandY();

		table.add(consoleTable).grow();
		table.row();
		table.add(buttonTable).growX();
		table.top();
		table.setFillParent(true);

	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act();
		stage.draw();
		console.setText(console.getText().append(""));
	}

	@Override
	public void resize(int width, int height) {
		view.update(width, height, true);
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
		stage.dispose();
	}
}

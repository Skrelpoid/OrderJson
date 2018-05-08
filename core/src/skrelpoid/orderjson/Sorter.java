package skrelpoid.orderjson;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

public class Sorter extends ClickListener implements Runnable {

	private FileHandle[] folders;
	private JsonReader reader;
	private JsonComparator comparator;
	private Array<Exception> exceptions;
	private FileFilter folderFilter = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return file != null && file.exists() && file.isDirectory() && !endsWithBackup(file)
					&& file.listFiles(jsonFilter).length > 0;
		}

		private boolean endsWithBackup(File file) {
			String name = file.getName();
			return name.length() > 6 && name.substring(name.length() - 6).toLowerCase().equals("backup");
		}
	};

	private FileFilter jsonFilter = file -> file.getName().toLowerCase().contains(".json");
	private OrderJson app;

	public Sorter(OrderJson app) {
		comparator = new JsonComparator();
		reader = new JsonReader();
		this.app = app;
	}

	@Override
	public void clicked(InputEvent event, float x, float y) {
		new Thread(this).start();
	}

	private void sortJsonFolder(FileHandle f) {
		FileHandle[] jsonFiles = f.list(jsonFilter);
		System.out.println("\nSorting folder named " + f.name());
		for (FileHandle jsonFile : jsonFiles) {
			sortJson(jsonFile);
		}
	}

	private void sortJson(FileHandle jsonFile) {
		try {
			System.out.println("\nParsing file named " + jsonFile.path());
			JsonValue jv = reader.parse(jsonFile);

			ArrayList<JsonValue> list = new ArrayList<JsonValue>();
			JsonValue temp = jv.child();
			// Edge case for keywords.json
			boolean isKeywords = false;
			if (temp != null && temp.name().equals("Game Dictionary")) {
				temp = temp.child();
				isKeywords = true;
			}

			while (temp != null) {
				list.add(temp);
				temp = temp.next();
			}

			System.out.println("Sorting file named " + jsonFile.path());
			Collections.sort(list, comparator);

			if (isKeywords) {
				temp = jv.child.child = list.get(0);
				temp.parent = jv.child;
			} else {
				temp = jv.child = list.get(0);
				temp.parent = jv;
			}
			temp.prev = null;
			for (int i = 1; i < list.size(); i++) {
				temp.next = list.get(i);
				temp.prev = list.get(i - 1);
				temp = temp.next;
			}
			temp.next = null;

			System.out.println("Saving file named " + jsonFile.path());
			jsonFile.writeString(prettyPrint(jv), false, "UTF-8");

		} catch (Exception ex) {
			System.out.println("Encountered ERROR while parsing, sorting or saving");
			exceptions.add(ex);
		}
	}

	private String prettyPrint(JsonValue jv) {
		String str = jv.prettyPrint(JsonWriter.OutputType.json, 0);
		String[] lines = str.split("\n");
		StringBuffer buffer = new StringBuffer();
		lines[0] = lines[0] + "\n";
		buffer.append(lines[0]);
		for (int i = 1; i < lines.length - 1; i++) {
			lines[i] = "\t" + lines[i] + "\n";
			buffer.append(lines[i]);
		}
		buffer.append(lines[lines.length - 1]);
		return buffer.toString();
	}

	private void findFolders() {
		System.out.println("\n---------------------------\nSearching for folders with JSON files");
		FileHandle local = Gdx.files.local("");
		folders = local.list(folderFilter);

	}

	private void copyAsBackup() {
		for (FileHandle f : folders) {
			System.out.println("Creating backup copy of folder named " + f.name());
			FileHandle backupCopy = Gdx.files.absolute(f.file().getAbsolutePath() + "backup");
			if (!backupCopy.exists()) {
				f.copyTo(backupCopy);
			}
		}
	}

	@Override
	public void run() {
		exceptions = new Array<Exception>();
		try {
			findFolders();
			if (app.backup.isChecked()) {
				copyAsBackup();
			}
			for (FileHandle f : folders) {
				sortJsonFolder(f);
			}
			System.out.println("---------------------------\nDONE!\n");
		} catch (Exception ex) {
			exceptions.add(ex);
		} finally {
			printExceptions();
			Gdx.app.postRunnable(scrollLater);
		}
	}

	private void printExceptions() {
		int size = exceptions.size;
		System.out.println("Encountered " + (size > 0 ? size : "no") + " Errors");
		for (Exception ex : exceptions) {
			ex.printStackTrace();
			System.out.println("---------------------------");
		}
	}

	public Runnable scrollLater = new Runnable() {

		int i = 0;

		@Override
		public void run() {
			if (i < 10) {
				Gdx.app.postRunnable(scrollLater);
			} else {
				i = 0;
				app.scroll.setScrollPercentX(1);
				app.scroll.setScrollPercentY(1);
				app.scroll.scrollTo(0, 0, 100, 100);
			}
		}

	};

}

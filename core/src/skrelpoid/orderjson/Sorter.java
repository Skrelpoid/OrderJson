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
			return file != null && file.exists() && file.isDirectory() && !isBackup(file) && isJsonFolder(file);
		}

		private boolean isJsonFolder(File file) {
			File[] children = file.listFiles(jsonFilter);
			if (children == null) {
				exceptions.add(new NotSupportedException(
						file.getName() + " is an invalid format (not a supported directory). "));
				return false;
			}
			return children.length > 0;
		}

		private boolean isBackup(File file) {
			String name = file.getName();
			return name.equals("backup");
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
		if (f.name() != null && f.name().length() > 0) {
			System.out.println("\nSorting folder named " + f.name());
		} else {
			System.out.println("\nSorting files next to JAR");
		}
		for (FileHandle jsonFile : jsonFiles) {
			sortJson(jsonFile);
		}
	}

	private void sortJson(FileHandle jsonFile) {
		try {
			String spacing = getSpacing(jsonFile);

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
			jsonFile.writeString(prettyPrint(jv, spacing), false, "UTF-8");

		} catch (Exception ex) {
			System.out.println("Encountered ERROR while parsing, sorting or saving");
			exceptions.add(ex);
		}
	}

	private String prettyPrint(JsonValue jv, String spacing) {
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
		return buffer.toString().replaceAll("\t", spacing);
	}

	private void findFoldersAndFiles() {
		System.out.println("\n---------------------------\nSearching for JSON files and folders with JSON files");
		FileHandle local = Gdx.files.local("");
		folders = local.list(folderFilter);
		if (local.list(jsonFilter).length > 0) {
			FileHandle[] temp = new FileHandle[folders.length + 1];
			System.arraycopy(folders, 0, temp, 0, folders.length);
			temp[folders.length] = local;
			folders = temp;
		}

	}

	private void copyAsBackup() {
		for (FileHandle folder : folders) {
			if (folder.name() != null && folder.name().length() > 0) {
				System.out.println("Creating backup copy of " + folder.name());
				FileHandle backupCopy = Gdx.files.local("backup/" + folder.name());
				if (!backupCopy.exists()) {
					folder.copyTo(backupCopy);
				}
			} else {
				System.out.println("Creating backup copy of files next to JAR");
				for (FileHandle f : folder.list(jsonFilter)) {
					FileHandle backupCopy = Gdx.files.local("backup/" + f.name());
					if (!backupCopy.exists()) {
						f.copyTo(backupCopy);
					}
				}
			}
		}
	}

	@Override
	public void run() {
		exceptions = new Array<Exception>();
		try {
			findFoldersAndFiles();
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
			if (ex instanceof NotSupportedException) {
				System.out.println(Gdx.files.internal("Unsupported.txt").readString());
			}
			System.out.println("---------------------------");
		}
	}

	private String getSpacing(FileHandle jsonFile) {
		try {
			String spacing = "";
			String fileContent = jsonFile.readString("UTF-8");
			int i = fileContent.indexOf("\n") + 1;
			char c = fileContent.charAt(i++);
			spacing = String.valueOf(c);
			if ((Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) || c != '"') {
				return "  ";
			}
			while (true) {
				if (fileContent.charAt(i) == spacing.charAt(0)) {
					spacing += fileContent.charAt(i);
					i++;
				} else {
					return spacing;
				}
			}
		} catch (Exception ex) {
			exceptions.add(ex);
			return "  ";
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

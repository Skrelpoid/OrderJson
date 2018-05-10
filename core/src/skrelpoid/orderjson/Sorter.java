package skrelpoid.orderjson;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
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

		// If the file is not null, exists, is a directory, is not named backup
		// and is a supported json folder return true
		@Override
		public boolean accept(File file) {
			return file != null && file.exists() && file.isDirectory() && !isBackup(file) && isJsonFolder(file);
		}

		private boolean isJsonFolder(File file) {
			String[] children = file.list();
			File[] jsonChildren = file.listFiles(jsonFilter);
			if (jsonChildren == null) {

				exceptions.add(new NotSupportedException(file.getName() + " (" + file.getAbsolutePath()
						+ ") is an invalid format (not a supported directory). File seems to contain "
						+ Arrays.toString(children)));
				return false;
			}
			return jsonChildren.length > 0;
		}

		private boolean isBackup(File file) {
			String name = file.getName();
			return name.equals("backup");
		}
	};
	// if the file ends with .json return true
	private FileFilter jsonFilter = file -> file.getName().toLowerCase().contains(".json");
	private OrderJson app;

	public Sorter(OrderJson app) {
		// Compare using a comparator
		comparator = new JsonComparator();
		// reads the json files into json Values
		reader = new JsonReader();
		this.app = app;
	}

	// called from start button
	@Override
	public void clicked(InputEvent event, float x, float y) {
		// when clicked create a new thread and execute the run Method of this
		// class
		new Thread(this).start();
	}

	private void sortJsonFolder(FileHandle f) {
		// get every json file of the folder f
		FileHandle[] jsonFiles = f.list(jsonFilter);
		if (f.name() != null && f.name().length() > 0) {
			System.out.println("\nSorting folder named " + f.name());
		} else {
			System.out.println("\nSorting files next to JAR");
		}
		// if f is a supported folder, sort every json file from the array
		if (jsonFiles != null) {
			for (FileHandle jsonFile : jsonFiles) {
				sortJson(jsonFile);
			}
		}
	}

	private void sortJson(FileHandle jsonFile) {
		// Exceptions should be catched
		try {
			String spacing = getSpacing(jsonFile);

			System.out.println("\nParsing file named " + jsonFile.path());
			// Parse the file
			JsonValue jv = reader.parse(jsonFile);

			ArrayList<JsonValue> list = new ArrayList<JsonValue>();
			// get the children layer
			JsonValue temp = jv.child();
			// Edge case for keywords.json
			boolean isKeywords = false;
			// if this is a keywords.json, go 1 layer deeper
			if (temp != null && temp.name().equals("Game Dictionary")) {
				temp = temp.child();
				isKeywords = true;
			}

			// add all jsonValues of the current to the list (unsorted)

			while (temp != null) {
				list.add(temp);
				temp = temp.next();
			}

			// sort the list
			System.out.println("Sorting file named " + jsonFile.path());
			Collections.sort(list, comparator);

			// temp should be the first element of the list which should be the
			// child of jv
			if (isKeywords) {
				// if this is the keywords.json go 1 layer deeper
				temp = jv.child.child = list.get(0);
				temp.parent = jv.child;
			} else {
				temp = jv.child = list.get(0);
				temp.parent = jv;
			}
			// the first Element doesnt have a previous element
			temp.prev = null;
			for (int i = 1; i < list.size(); i++) {
				// relink the jsonValues so they know their parent, next and
				// prev
				temp.next = list.get(i);
				temp.prev = list.get(i - 1);
				// work on the next jsonValue
				temp = temp.next;
			}
			// the last element doesnt have a next element
			temp.next = null;

			System.out.println("Saving file named " + jsonFile.path());
			// write the sorted json file (prettyPrinted) to the original file
			// using UTF-8 Encoding
			jsonFile.writeString(prettyPrint(jv, spacing), false, "UTF-8");

		} catch (Exception ex) {
			// queue exception
			System.out.println("Encountered ERROR while parsing, sorting or saving");
			exceptions.add(ex);
		}
	}

	// Pretty print but keep original spacing and add additinal indentation
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
		// Get the current directory (The dir this jar is in)
		FileHandle local = Gdx.files.local("");
		// get all children of this folder that are not named backup, have
		// children thqt are json files and are supported directories
		folders = local.list(folderFilter);
		// Check if there are jsonFiles in this folder
		FileHandle[] localChildren = local.list(jsonFilter);
		if (localChildren != null && localChildren.length > 0) {
			// Copy this folder as the last folder to be sorted
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

	// Called in a new thread when start button is clicked
	@Override
	public void run() {
		// Create an empty array of exceptions to queue them later
		exceptions = new Array<Exception>();
		try {
			findFoldersAndFiles();
			// Backup files if Create backup is checked
			if (app.backup.isChecked()) {
				copyAsBackup();
			}
			// for every folder with json files you found, sort it
			for (FileHandle f : folders) {
				sortJsonFolder(f);
			}
			System.out.println("---------------------------\nDONE!\n");
		} catch (Exception ex) {
			exceptions.add(ex);
		} finally {
			// Always print exceptions that were queued
			printExceptions();
			Gdx.app.postRunnable(scrollLater);
		}
	}

	private void printExceptions() {
		// print exceptions to GUI
		int size = exceptions.size;
		System.out.println("Encountered " + (size > 0 ? size : "no") + " Errors");
		for (Exception ex : exceptions) {
			ex.printStackTrace();
			// If it is a NotSupportedException print explanation for a fix
			if (ex instanceof NotSupportedException) {
				System.out.println(Gdx.files.internal("Unsupported.txt").readString());
			}
			System.out.println("---------------------------");
		}
		// create a log if there are exceptions
		if (exceptions.size > 0) {
			app.createLog(60);
			System.out.println("Creating log, please wait");
			System.out.println("---------------------------");
		}
	}

	// get the spacing used in the json file
	private String getSpacing(FileHandle jsonFile) {
		// Exceptions are should be catched
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
			// print exceptions later
			exceptions.add(ex);
			return "  ";
		}
	}

	public Runnable scrollLater = new Runnable() {

		int i = 0;

		// Scroll in 11 frames
		@Override
		public void run() {
			if (i < 10) {
				i++;
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

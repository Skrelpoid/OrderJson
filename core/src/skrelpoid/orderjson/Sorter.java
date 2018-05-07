package skrelpoid.orderjson;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

public class Sorter extends ClickListener {

	private FileHandle[] folders;
	private JsonReader reader;
	private JsonComparator comparator;
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
	private CheckBox check;

	public Sorter(CheckBox check) {
		comparator = new JsonComparator();
		reader = new JsonReader();
		this.check = check;
	}

	@Override
	public void clicked(InputEvent event, float x, float y) {
		findFolders();
		if (check.isChecked()) {
			copyAsBackup();
		}
		for (FileHandle f : folders) {
			sortJsonFolder(f);
		}
		System.out.println("DONE!");
	}

	private void sortJsonFolder(FileHandle f) {
		FileHandle[] jsonFiles = f.list(jsonFilter);
		System.out.println("Sorting folder named " + f.name());
		for (FileHandle jsonFile : jsonFiles) {
			sortJson(jsonFile);
		}
	}

	private void sortJson(FileHandle jsonFile) {
		try {
			System.out.println("Parsing file named " + jsonFile.name());
			JsonValue jv = reader.parse(jsonFile);

			ArrayList<JsonValue> list = new ArrayList<JsonValue>();
			JsonValue temp = jv.child();

			while (temp != null) {
				list.add(temp);
				temp = temp.next();
			}

			System.out.println("Sorting file named " + jsonFile.name());
			Collections.sort(list, comparator);

			jv.child = list.get(0);
			temp = jv.child;
			temp.parent = jv;
			temp.prev = null;
			for (int i = 1; i < list.size(); i++) {
				temp.next = list.get(i);
				temp.prev = list.get(i - 1);
				temp = temp.next;
			}
			temp.next = null;

			System.out.println("Saving file named " + jsonFile.name());
			jsonFile.writeString(prettyPrint(jv), false, "UTF-8");

		} catch (Exception ex) {
			ex.printStackTrace();
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
		System.out.println("Searching for folders with JSON files");
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

}

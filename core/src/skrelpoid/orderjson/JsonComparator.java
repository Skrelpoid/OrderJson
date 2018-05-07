package skrelpoid.orderjson;

import java.util.Comparator;

import com.badlogic.gdx.utils.JsonValue;

public class JsonComparator implements Comparator<JsonValue> {

	@Override
	public int compare(JsonValue o1, JsonValue o2) {
		int compareValue = o1.name().compareTo(o2.name());
		return compareValue > 0 ? 1 : compareValue < 0 ? -1 : 0;
	}

}

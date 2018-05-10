package skrelpoid.orderjson;

import java.util.Comparator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonValue;

public class JsonComparator implements Comparator<JsonValue> {

	// returns 0 if o1 and 02 are equal, -1 if o1s name comes before o2 name in
	// the alphabet, 1 if o1s name comes after o2s name in the alphabet
	@Override
	public int compare(JsonValue o1, JsonValue o2) {
		int compareValue = o1.name().compareTo(o2.name());
		// limit return value to -1, 0 and 1
		return MathUtils.clamp(compareValue, -1, 1);
	}

}

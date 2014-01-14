package RSLBench.Helpers;

import java.util.HashMap;
import rescuecore2.worldmodel.EntityID;

public class SimpleID {	
	private static HashMap<EntityID, Integer> _map1 = new HashMap<EntityID, Integer>();
	private static HashMap<Integer, EntityID> _map2 = new HashMap<Integer, EntityID>();
	private static int count = 1;

	public static Integer conv(EntityID id) {
		if (!_map1.containsKey(id)) {
			_map1.put(id, count);
			_map2.put(count, id);
			count++;
		}
		return _map1.get(id);
	}
	
/*	public static EntityID conv(Integer num) {
		if (!_map2.containsKey(num)) {
			Logger.debugColor("Unknown number: " + num, Logger.BG_RED);
			return new EntityID(-1);
		}
		else return _map2.get(num);
	}
*/	
}

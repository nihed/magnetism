package com.dumbhippo.dm.filter;

import java.util.Set;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledSetFilter<K,T extends DMObject<K>,KI,TI extends DMObject<KI>> {
	Set<KI> filterKeys(DMViewpoint viewpoint, K key, Set<KI> items);
	Set<TI> filterObjects(DMViewpoint viewpoint, K key, Set<TI> items);
}

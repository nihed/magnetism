package com.dumbhippo.dm.filter;

import java.util.List;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledListFilter<K,T extends DMObject<K>,KI,TI extends DMObject<KI>> {
	List<KI> filter(DMViewpoint viewpoint, K key, List<KI> items);
	List<TI> filter(DMViewpoint viewpoint, T object, List<TI> items);
}

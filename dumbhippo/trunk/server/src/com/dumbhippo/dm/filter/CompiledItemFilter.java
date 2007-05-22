package com.dumbhippo.dm.filter;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledItemFilter<K,T extends DMObject<K>,KI,TI extends DMObject<KI>> {
	KI filter(DMViewpoint viewpoint, K key, KI item);
	TI filter(DMViewpoint viewpoint, T object, TI item);
}

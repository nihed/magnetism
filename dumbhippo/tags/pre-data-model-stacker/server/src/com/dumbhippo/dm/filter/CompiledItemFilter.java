package com.dumbhippo.dm.filter;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledItemFilter<K,T extends DMObject<K>,KI,TI extends DMObject<KI>> {
	KI filterKey(DMViewpoint viewpoint, K key, KI itemKey);
	TI filterObject(DMViewpoint viewpoint, K key, TI item);
}

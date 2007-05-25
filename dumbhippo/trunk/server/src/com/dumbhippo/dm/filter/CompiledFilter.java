package com.dumbhippo.dm.filter;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledFilter<K1,T1 extends DMObject<K1>> {
	K1 filterKey(DMViewpoint viewpoint, K1 key);
	T1 filterObject(DMViewpoint viewpoint, T1 object);
}

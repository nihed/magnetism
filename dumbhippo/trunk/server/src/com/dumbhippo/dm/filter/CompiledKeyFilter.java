package com.dumbhippo.dm.filter;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledKeyFilter<K1,T1 extends DMObject<K1>> {
	K1 filter(DMViewpoint viewpoint, K1 key);
	T1 filter(DMViewpoint viewpoint, T1 object);
}

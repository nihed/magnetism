package com.dumbhippo.dm.filter;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledItemFilter<K1,K2,T1 extends DMObject<K1>,T2 extends DMObject<K2>> {
	K2 filter(DMViewpoint viewpoint, K1 key, K2 item);
	T2 filter(DMViewpoint viewpoint, T1 object, T2 item);
}

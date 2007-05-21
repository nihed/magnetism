package com.dumbhippo.dm.filter;

import java.util.List;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;

public interface CompiledListFilter<K1,K2,T1 extends DMObject<K1>,T2 extends DMObject<K2>> {
	List<K2> filter(DMViewpoint viewpoint, K1 key, List<K2> items);
	List<T2> filter(DMViewpoint viewpoint, T1 object, List<T2> items);
}

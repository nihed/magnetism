package com.dumbhippo.services.caches;

import javax.ejb.Local;

@Local
public interface CacheFactory {

	public <CacheType> CacheType lookup(Class<CacheType> klass);
	
	public void injectCaches(Object o);
}

package com.dumbhippo.services.caches;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.util.EJBUtil;


/** 
 * A session bean implementing the Cache interface, which returns a single object (as opposed to a list) as the 
 * ResultType. Subclass AbstractBasicCacheWithStorageBean is usually a better choice unless your cache needs to 
 * implement its own custom handling of persistence objects.
 */
public abstract class AbstractBasicCacheBean<KeyType, ResultType> extends
		AbstractCacheBean<KeyType, ResultType, Cache<KeyType,ResultType>> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractBasicCacheBean.class);
	
	protected AbstractBasicCacheBean(Request defaultRequest, Class<? extends Cache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
	}	

	static private class AbstractBasicCacheTask<KeyType,ResultType> implements Callable<ResultType> {
		
		private Class<? extends Cache<KeyType,ResultType>> ejbIface;
		private KeyType key;

		public AbstractBasicCacheTask(KeyType key, Class<? extends Cache<KeyType,ResultType>> ejbIface) {
			this.key = key;
			this.ejbIface = ejbIface;
		}
		
		public ResultType call() {
			logger.debug("Entering AbstractBasicCacheTask thread for bean {} key {}", ejbIface.getName(), key);
		
			Cache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);					

			try {
				// Check again in case another node stored the data first				
				return cache.checkCache(key);
			} catch (NotCachedException e) {
				ResultType data = cache.fetchFromNet(key);

				return cache.saveInCache(key, data);				
			}
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ResultType getSync(KeyType key) {
		return ThreadUtils.getFutureResultNullOnException(getAsync(key));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public Future<ResultType> getAsync(KeyType key) {
		if (key == null) {
			throw new IllegalArgumentException("null key passed to " + getEjbIface().getName());
		}
		
		try {
			ResultType result = checkCache(key);
			// result may be null, but in that case we cached null
			return new KnownFuture<ResultType>(result);
		} catch (NotCachedException e) {
			return getExecutor().execute(key, new AbstractBasicCacheTask<KeyType,ResultType>(key, getEjbIface()));	
		}
	}
}

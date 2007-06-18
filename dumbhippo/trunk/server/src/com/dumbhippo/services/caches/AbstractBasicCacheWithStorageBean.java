package com.dumbhippo.services.caches;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CachedItem;
import com.dumbhippo.tx.TxUtils;

/** 
 * Base class for cache beans that store each result in one database row 
 * (vs. AbstractListCacheWithStorageBean for beans that store a list of result rows).
 * 
 * @author Havoc Pennington
 *
 * @param <KeyType>
 * @param <ResultType>
 * @param <EntityType>
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS) // hackaround for bug with method tx attr on generic methods
public abstract class AbstractBasicCacheWithStorageBean<KeyType,ResultType,EntityType extends CachedItem>
	extends AbstractBasicCacheBean<KeyType,ResultType>
	implements BasicCacheStorageMapper<KeyType,ResultType,EntityType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractBasicCacheWithStorageBean.class);
	
	private BasicCacheStorage<KeyType,ResultType,EntityType> storage;
	
	protected AbstractBasicCacheWithStorageBean(Request defaultRequest, Class<? extends Cache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
	}
	
	@PostConstruct
	public void init() {
		storage = new BasicCacheStorage<KeyType,ResultType,EntityType>(em, getExpirationTime(), this);
	}	

	public abstract EntityType queryExisting(KeyType key);
	
	public abstract ResultType resultFromEntity(EntityType entity);
	
	public abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	public abstract void updateEntityFromResult(KeyType key, ResultType result, EntityType entity);
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED) // would be the default, but we changed the class default
	public ResultType checkCache(KeyType key) throws NotCachedException {
		TxUtils.assertHaveTransaction();
		return storage.checkCache(key);
	}

	public abstract EntityType newNoResultsMarker(KeyType key);
	
	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public ResultType saveInCacheInsideExistingTransaction(KeyType key, ResultType data, Date now, boolean refetchedWithoutCheckingCache) {
		TxUtils.assertHaveTransaction();
		return storage.saveInCacheInsideExistingTransaction(key, data, now, refetchedWithoutCheckingCache);
	}
}

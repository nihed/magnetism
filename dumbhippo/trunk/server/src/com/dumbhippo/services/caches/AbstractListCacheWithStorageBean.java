package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CachedListItem;

public abstract class AbstractListCacheWithStorageBean<KeyType,ResultType,EntityType extends CachedListItem>
	extends AbstractListCacheBean<KeyType,ResultType>
	implements ListCache<KeyType, ResultType>, ListCacheStorageMapper<KeyType,ResultType,EntityType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheWithStorageBean.class);
	
	private ListCacheStorage<KeyType,ResultType,EntityType> storage;
	
	protected AbstractListCacheWithStorageBean(Request defaultRequest, Class<? extends ListCache<KeyType,ResultType>> ejbIface, long expirationTime, Class<ResultType> resultClass) {
		super(defaultRequest, ejbIface, expirationTime, resultClass);
	}
	
	@PostConstruct
	public void init() {
		storage = new ListCacheStorage<KeyType,ResultType,EntityType>(em, getExpirationTime(), getResultClass(), this);
	}

	public abstract List<EntityType> queryExisting(KeyType key);
	
	public abstract ResultType resultFromEntity(EntityType entity);
	
	public abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	public List<? extends ResultType> checkCache(KeyType key) throws NotCachedException {
		return storage.checkCache(key);
	}
	
	public abstract EntityType newNoResultsMarker(KeyType key);

	public void setAllLastUpdatedToZero(KeyType key) {
		throw new UnsupportedOperationException("Cache doesn't support manual expiration: " + getEjbIface().getName());
	}
	
	@Override
	public void expireCache(KeyType key) {
		storage.expireCache(key);
	}
	
	public void deleteCache(KeyType key) {
		storage.deleteCache(key);
	}
	
	// null means that we could not get the updated results, so leave the old results
	// empty list results means that we should save a no results marker
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public List<? extends ResultType> saveInCacheInsideExistingTransaction(KeyType key, List<? extends ResultType> newItems, Date now) {
		return storage.saveInCacheInsideExistingTransaction(key, newItems, now);
	}
}

package com.dumbhippo.dm;

import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

/**
 * DMObject is the base class that all "Data Model Object" (DMO) classes must derive from.
 * To implement a new DMO class, you derive an <i>abstract</i> class from DMObject and override the init() 
 * method. You must also mark your class with the @DMO annotation and mark any resource properties
 * with the @DMProperty annotation. Concrete instances of the DMO are actually instances of a 
 * wrapper class that the system derives from the abstract class you write.
 *
 * @param <KeyType>
 */
public abstract class DMObject<KeyType> {
	private KeyType key;
	private StoreKey<KeyType,? extends DMObject<KeyType>> storeKey;
	Guid guid;
	
	@SuppressWarnings("unchecked")
	protected DMObject(KeyType key) {
		this.storeKey = new StoreKey(getClassHolder(), key);
		this.key = key;
	}
	
	public final KeyType getKey() {
		return key;
	}
	
	public final StoreKey<KeyType,? extends DMObject<KeyType>> getStoreKey() {
		return storeKey;
	}
	
	/**
	 * Returns the absolute resource ID URI for this object.
	 * 
	 * @return the resource ID URI for this object
	 */
	@SuppressWarnings("unchecked")
	public final String getResourceId() {
		return getClassHolder().makeResourceId(key);
	}

	/**
	 * Do any heavy-weight initialization necessary before the object's property
	 * methods are called. The init() method will typically load the entity
	 * bean that corresponds to the resource. (A DMObject is only ever used
	 * within a single session and transaction.) 
	 * 
	 * @throws NotFoundException if no resource exists for the object's key
	 */
	protected abstract void init() throws NotFoundException;
	
	/**
	 * Check if an object has been initialized. Don't implement this; it's implemented
	 * in the wrapper that is derived from your object. 
	 * 
	 * @return
	 */
	protected abstract boolean isInitialized();
	
	/**
	 * Gets the DMClassHolder which contains information about this resource class. Don't
	 * implement this; it's implement in the wrapper that is derive from your object. 
	 * 
	 * @return
	 */
	public abstract <T extends DMObject<KeyType>> DMClassHolder<KeyType,T> getClassHolder();
}

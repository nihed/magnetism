package com.dumbhippo.dm.schema;

import java.util.Iterator;

import javassist.CtMethod;

import com.dumbhippo.dm.Cardinality;
import com.dumbhippo.dm.DMClient;
import com.dumbhippo.dm.DMFeed;
import com.dumbhippo.dm.DMFeedItem;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.PropertyType;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.filter.AndFilter;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;
import com.dumbhippo.dm.store.DMStore;
import com.dumbhippo.dm.store.StoreKey;

public class FeedPropertyHolder<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> extends ResourcePropertyHolder<K,T,KI,TI> {
	private CompiledItemFilter<K,T,KI,TI> itemFilter;

	public FeedPropertyHolder(DMClassHolder<K,T> declaringClassHolder, CtMethod ctMethod, DMClassInfo<KI,TI> classInfo, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, classInfo, annotation, filter, viewerDependent);
	}
	
	@Override
	public void complete() {
		super.complete();
		
		Filter classFilter = getResourceClassHolder().getUncompiledItemFilter();
		if (classFilter != null && propertyFilter == null) {
			itemFilter = getResourceClassHolder().getItemFilter();
		} else if (classFilter != null || propertyFilter != null) {
			Filter toCompile;
			if (classFilter != null)
				toCompile = new AndFilter(classFilter, propertyFilter);
			else
				toCompile = propertyFilter;
			
			itemFilter = FilterCompiler.compileItemFilter(declaringClassHolder.getModel(), 
													 	  declaringClassHolder.getKeyClass(), 
														  keyType, toCompile);
		}
	}

	@Override
	protected PropertyType getType() {
		return PropertyType.RESOURCE;
	}

	@Override
	public String getUnboxPrefix() {
		return "(com.dumbhippo.dm.DMFeed)";
	}
	
	@Override
	public String getUnboxSuffix() {
		return "";
	}

	@Override
	public Object dehydrate(Object value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object rehydrate(DMViewpoint viewpoint, K key, Object value, DMSession session, boolean filter) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object filter(DMViewpoint viewpoint, K key, Object value) {
		throw new UnsupportedOperationException();
	}
	
	private long updateFeedTimestamp(DMSession session, T object) {
		DMClient client = session.getClient();
		
		if (client != null) {
			DMClassHolder classHolder = object.getClassHolder();
			int feedPropertyIndex = classHolder.getFeedPropertyIndex(getName());
			DataModel model = classHolder.getModel();
			DMStore store = model.getStore();

			@SuppressWarnings("unchecked")
			StoreKey<K, T> storeKey = (StoreKey<K, T>)object.getStoreKey();
		
			return store.updateFeedTimestamp(storeKey, feedPropertyIndex, client.getStoreClient(), model.getTimestamp());
		} else {
			return -1;
		}
	}

	/**
	 * Like DMPropertyHolder.visitChildren(), but specialized for feeds. Recurses and visits
	 * children of the feed.
	 * 
	 * @param session
	 * @param children
	 * @param start start position in feed to visit from 
	 * @param max maximum of number of items to visit
	 * @param object the object whose property to visit
	 * @param visitor
	 * @param forceAll if true, fetch everything specified by start and max, even if we have a timestamp
	 *    indicating that we have already returned the current contents of the feed to the client
	 */
	@SuppressWarnings("unchecked")
	public long visitFeedChildren(DMSession session, Fetch<?,?> children, int start, int max, T object, FetchVisitor visitor, boolean forceAll) {
		long minTimestamp = updateFeedTimestamp(session, object);
		if (forceAll)
			minTimestamp = 0;
		
		Fetch<KI,TI> typedChildren = (Fetch<KI,TI>)children;
		
		Iterator<DMFeedItem<TI>> iter = ((DMFeed<TI>)getRawPropertyValue(object)).iterator(start, max, minTimestamp);
		while (iter.hasNext()) {
			DMFeedItem<TI> item = iter.next();
			visitChild(session, typedChildren, item.getValue(), visitor);
		}
		
		return minTimestamp;
	}

	/**
	 * Like DMPropertyHolder.visitProperty(), but specialized for feeds. Calls the feedProperty()
	 * method of the visitor for each visited value in the property.
	 * 
	 * @param session
	 * @param start start position in feed to visit from 
	 * @param max maximum of number of items to visit
	 * @param object the object whose property to visit
	 * @param visitor
	 * @param minTimestamp minimum timestamp for items to fetch. A value of -1 means to compute
	 *   this value from what is stored for this client and then update the stored information 
	 *   for subsequent fetches. Any other value is used literally without updating the stored
	 *   information. (So, '0' can be used to force fetch everything)
	 */
	@SuppressWarnings("unchecked")
	public void visitFeedProperty(DMSession session, int start, int max, T object, FetchVisitor visitor, long minTimestamp) {
		boolean seenAny = false;
		
		if (minTimestamp < 0)
			minTimestamp = updateFeedTimestamp(session, object);
		
		boolean fetchingAll = start == 0 && minTimestamp <= 0;

		Iterator<DMFeedItem<TI>> iter = ((DMFeed<TI>)getRawPropertyValue(object)).iterator(start, max, minTimestamp);
		while (iter.hasNext()) {
			DMFeedItem<TI> item = iter.next();
			seenAny = true;
			visitor.feedProperty(this, item.getValue().getKey(), item.getTime(), !fetchingAll);
		}
		
		if (!seenAny && fetchingAll) {
			visitor.emptyProperty(this);
		}
	}
	

	@Override
	public void visitChildren(DMSession session, Fetch<?, ?> children, T object, FetchVisitor visitor) {
		throw new UnsupportedOperationException("Must use visitFeedChildren");
	}

	@Override
	public void visitProperty(DMSession session, T object, FetchVisitor visitor, boolean forceEmpty) {
		throw new UnsupportedOperationException("Must use visitFeedProperty");
	}
	
	@Override
	public Cardinality getCardinality() {
		return Cardinality.ANY;
	}
	
	public CompiledItemFilter<K, T, KI, TI> getItemFilter() {
		return itemFilter;
	}
}

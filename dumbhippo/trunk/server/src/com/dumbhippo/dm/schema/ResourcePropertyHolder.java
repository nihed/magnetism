package com.dumbhippo.dm.schema;

import javassist.CtMethod;
import antlr.RecognitionException;
import antlr.TokenStreamException;

import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.fetch.FetchVisitor;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.identity20.Guid;

public abstract class ResourcePropertyHolder<K,T extends DMObject<K>> extends DMPropertyHolder {
	private boolean completed;
	private DMClassHolder<T> resourceClassHolder;
	private Fetch<K,T> defaultChildren;

	public ResourcePropertyHolder(DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, Class<T> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
	}

	@Override
	public void complete() {
		super.complete();
		if (completed)
			return;
		
		completed = true;
		
		resourceClassHolder = declaringClassHolder.getModel().getDMClass(getResourceType());

		if (!"".equals(annotation.defaultChildren())) {
			defaultInclude = true;
			
			try {
				FetchNode node = FetchParser.parse(annotation.defaultChildren());
				if (node.getProperties().length > 0) {
					defaultChildren = node.bind(resourceClassHolder);
				}
			} catch (RecognitionException e) {
				throw new RuntimeException(propertyId + ": failed to parse defaultChildren at char " + e.getColumn(), e);
			} catch (TokenStreamException e) {
				throw new RuntimeException(propertyId + ": failed to parse defaultChildren", e);
			}
		}
	}

	@Override
	public Fetch<K,T> getDefaultChildren() {
		return defaultChildren;
	}
	
	@SuppressWarnings("unchecked")
	public Class<T> getResourceType() {
		return elementType;
	}
	
	public DMClassHolder<T> getResourceClassHolder() {
		if (completed)
			return resourceClassHolder;
		else
			return declaringClassHolder.getModel().getDMClass(getResourceType());
	}
	
	protected Object dehydrateDMO(Object value) {
		Object key = ((DMObject)value).getKey();
		if ((key instanceof Guid) || (key instanceof String)) {
			return key;
		} else {
			return ((DMKey)key).clone();
		}
	}

	@SuppressWarnings("unchecked")
	protected Object rehydrateDMO(Object value, DMSession session) {
		try {
			return session.find(elementType, value);
		} catch (com.dumbhippo.server.NotFoundException e) {
			// FIXME: find() basically always has to exceed, because of lazy initialization
			throw new RuntimeException("Unexpectedly could not find object when rehydrating");
		}
	}
	
	protected void visitChild(DMSession session, Fetch<K,T> children, T value, FetchVisitor visitor) {
		children.visit(session, resourceClassHolder, value, visitor, true);
	}

	protected void visitResourceValue(DMSession session, T value, FetchVisitor visitor) {
		visitor.resourceProperty(this, value.getKey());
	}

	@Override
	public Class<?> getKeyClass() {
		return resourceClassHolder.getKeyClass();
	}
}

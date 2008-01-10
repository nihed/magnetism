package com.dumbhippo.dm.schema;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;

class PropertyInfo<K, T extends DMObject<K>, TI> {
	public enum ContainerType {
		SINGLE,
		LIST,
		SET,
		FEED
	};
	
	private DataModel model;
	private Class<T> declaringType; 
	private Class<K> keyType;
	private Class<TI> itemType;
	
	private CtMethod ctMethod;
	private DMProperty annotation;
	private DMFilter filter;
	private ViewerDependent viewerDependent;
	
	private ContainerType containerType;
	
	public PropertyInfo(Class<T> declaringType, Class<K> keyType, Class<TI> elementType) {
		this.declaringType = declaringType;
		this.keyType = keyType;
		this.itemType = elementType;
	}

	public ContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ContainerType containerType) {
		this.containerType = containerType;
	}

	public CtMethod getCtMethod() {
		return ctMethod;
	}

	public void setCtMethod(CtMethod ctMethod) {
		this.ctMethod = ctMethod;
	}

	public Class<T> getDeclaringType() {
		return declaringType;
	}

	public Class<TI> getItemType() {
		return itemType;
	}

	public DMFilter getFilter() {
		return filter;
	}

	public void setFilter(DMFilter filter) {
		this.filter = filter;
	}

	public Class<K> getKeyType() {
		return keyType;
	}

	public DataModel getModel() {
		return model;
	}

	public void setModel(DataModel model) {
		this.model = model;
	}

	public DMProperty getAnnotation() {
		return annotation;
	}

	public void setAnnotation(DMProperty property) {
		this.annotation = property;
	}

	public ViewerDependent getViewerDependent() {
		return viewerDependent;
	}

	public void setViewerDependent(ViewerDependent viewerDependent) {
		this.viewerDependent = viewerDependent;
	}
	
	public DMPropertyHolder<K,T,TI> createPropertyHolder() {
		switch (containerType) {
		case SINGLE:
			return new SinglePlainPropertyHolder<K,T,TI>(this);
		case LIST:
			return new ListPlainPropertyHolder<K,T,TI>(this);
		case SET:
			return new SetPlainPropertyHolder<K,T,TI>(this);
		case FEED:
			throw new RuntimeException("Feed properties must be resource-valued");
		}
		
		throw new RuntimeException("Unexpected container type");
	}
}

package com.dumbhippo.dm.fetch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMPropertyHolder;
import com.dumbhippo.dm.DMSession;

public final class Fetch<T extends DMObject> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(Fetch.class);
	
	private DMClassHolder<T> classHolder;
	private PropertyFetch[] properties;
	private boolean includeDefault;
	
	public Fetch(DMClassHolder<T> classHolder, PropertyFetch[] properties, boolean includeDefault) {
		this.classHolder = classHolder;
		this.properties = properties;
		this.includeDefault = includeDefault;
	}
	
	public PropertyFetch[] getProperties() {
		return properties;
	}
	
	public boolean getIncludeDefault() {
		return includeDefault;
	}
	
	private void recurseIntoValue(DMSession session, DMPropertyHolder propertyHolder, Fetch children, DMObject value, FetchVisitor visitor) {
		children.visit(session, value, visitor);
	}

	public void recurseIntoProperty(DMSession session, DMPropertyHolder propertyHolder, Fetch children, DMObject object, FetchVisitor visitor) {
		if (children == null)
			return;
		
		if (!propertyHolder.isResourceValued())
			return;
		
		Method method = propertyHolder.getMethod();
		try {
			if (propertyHolder.isMultiValued()) {
				for (DMObject value : (List<? extends DMObject>)method.invoke(object)) {
					recurseIntoValue(session, propertyHolder, children, value, visitor);
				}
			} else {
				DMObject value = (DMObject)method.invoke(object);
				recurseIntoValue(session, propertyHolder, children, value, visitor);
			}
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		}
	}
	
	private void emitResourceValue(DMSession session, DMPropertyHolder propertyHolder, DMObject value, FetchVisitor visitor) {
		visitor.resourceProperty(propertyHolder, value.getKey());
	}

	private void emitPlainValue(DMSession session, DMPropertyHolder propertyHolder, Object value, FetchVisitor visitor) {
		visitor.plainProperty(propertyHolder, value);
	}

	private void emitProperty(DMSession session, DMPropertyHolder propertyHolder, DMObject object, FetchVisitor visitor) {
		Method method = propertyHolder.getMethod();
		try {
			if (propertyHolder.isResourceValued()) {
				if (propertyHolder.isMultiValued()) {
					for (DMObject value : (List<? extends DMObject>)method.invoke(object)) {
						emitResourceValue(session, propertyHolder, value, visitor);
					}
				} else  {
					DMObject value = (DMObject)method.invoke(object);
					emitResourceValue(session, propertyHolder, value, visitor);
				}
			} else {
				if (propertyHolder.isMultiValued()) {
					for (Object value : (List<?>)method.invoke(object)) {
						emitPlainValue(session, propertyHolder, value, visitor);
					}
				} else {
					Object value = (Object)method.invoke(object);
					emitPlainValue(session, propertyHolder, value, visitor);
				}
			}
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error getting property value during visit", e);
		}
	}

	public void visit(DMSession session, DMObject object, FetchVisitor visitor) {
		DMPropertyHolder[] classProperties = classHolder.getProperties();

		int i = 0, j = 0;
		while (i < properties.length || j < classProperties.length) {
			// We assume that a property ordering of Long.MAX_VALUE will never occur
			long iOrdering = i < properties.length ? properties[i].getProperty().getOrdering() : Long.MAX_VALUE;
			long jOrdering = j < classProperties.length ? classProperties[j].getOrdering() : Long.MAX_VALUE;

//			logger.debug("{}: {}={},{}, {}={},{}",
//					new Object[] {
//						classHolder.getBaseClass().getSimpleName(),
//						i, (i < properties.length ? properties[i].getProperty().getName() : "xxx"), iOrdering,
//						j, (j < classProperties.length ? classProperties[j].getName() : "xxx"), jOrdering,
//					});
			
			if (iOrdering < jOrdering) {
				// property not in the class, ignore
				i++;
			} else if (iOrdering == jOrdering) {
				// fetched property in the class
				recurseIntoProperty(session, properties[i].getProperty(), properties[i].getChildren(), object, visitor);
				i++;
				j++;
			} else {
				// property in the class not explicitly fetched
				if (includeDefault && classProperties[j].getDefaultInclude()) {
					recurseIntoProperty(session, classProperties[j], classProperties[j].getDefaultChildren(), object, visitor);
				}
				j++;
			}
		}
		
		visitor.beginResource(classHolder, object.getKey(), this);

		i = 0; j = 0;
		while (i < properties.length || j < classProperties.length) {
			long iOrdering = i < properties.length ? properties[i].getProperty().getOrdering() : Long.MAX_VALUE;
			long jOrdering = j < classProperties.length ? classProperties[j].getOrdering() : Long.MAX_VALUE;
			
			if (iOrdering < jOrdering) {
				// property not in the class, ignore
				i++;
			} else if (iOrdering == jOrdering) {
				// fetched property in the class
				emitProperty(session, classProperties[j], object, visitor);
				i++;
				j++;
			} else {
				// property in the class not explicitly fetched
				if (includeDefault && classProperties[j].getDefaultInclude())
					emitProperty(session, classProperties[j], object, visitor);
				j++;
			}
		}
		
		visitor.endResource();
	}
	
	private void appendToFetchString(StringBuilder sb, DMPropertyHolder propertyHolder, boolean qualify) {
		if (sb.length() > 0)
			sb.append(";");
		if (qualify)
			sb.append(propertyHolder.getPropertyId());
		else
			sb.append(propertyHolder.getName());
	}

	public String makeFetchString(DMClassHolder<?> classHolder) {
		boolean allFetched = true;
		boolean noneFetched = false;
		
		DMPropertyHolder[] classProperties = classHolder.getProperties();
		
		int i = 0, j = 0;
		while (i < properties.length || j < classProperties.length) {
			long iOrdering = i < properties.length ? properties[i].getProperty().getOrdering() : Long.MAX_VALUE;
			long jOrdering = j < classProperties.length ? classProperties[j].getOrdering() : Long.MAX_VALUE;
			
			if (iOrdering < jOrdering) {
				// property not in the class, ignore
				i++;
			} else if (iOrdering == jOrdering) {
				// fetched property in the class
				noneFetched = false;
				i++;
				j++;
			} else {
				// property in the class not explicitly fetched
				if (includeDefault && classProperties[j].getDefaultInclude())
					noneFetched = false;
				else
					allFetched = false;
				j++;
			}
		}
		
		if (noneFetched)
			return "";
		else if (allFetched)
			return "*";
		
		StringBuilder sb = new StringBuilder();
		
		i = 0; j = 0;
		while (i < properties.length || j < classProperties.length) {
			long iOrdering = i < properties.length ? properties[i].getProperty().getOrdering() : Long.MAX_VALUE;
			long jOrdering = j < classProperties.length ? classProperties[j].getOrdering() : Long.MAX_VALUE;
			
			if (iOrdering < jOrdering) {
				// property not in the class, ignore
				i++;
			} else if (iOrdering == jOrdering) {
				// fetched property in the class
				appendToFetchString(sb, classProperties[j], classHolder.mustQualifyProperty(j));
				noneFetched = false;
				i++;
				j++;
			} else {
				// property in the class not explicitly fetched
				if (includeDefault && classProperties[j].getDefaultInclude())
					appendToFetchString(sb, classProperties[j], classHolder.mustQualifyProperty(j));
					noneFetched = false;
				j++;
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Fetch))
			return false;
		
		Fetch other = (Fetch)o;
		
		if (includeDefault != other.includeDefault)
			return false;
		
		if (properties.length != other.properties.length)
			return false;
		
		for (int i = 0; i < properties.length; i++)
			if (!properties[i].equals(other.properties[i]))
				return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int value = includeDefault ? 1 : 0;
 
		for (int i = 0; i < properties.length; i++)
			value = value * 31 + properties[i].hashCode();
		
		return value;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		if (includeDefault)
			b.append('+');
		
		// This method is primarily for our tests. For testing purposes, we want a
		// human-predictable ordering for properties, instead of a MD5-hash ordering
		//
		PropertyFetch[] sortedProperties = (PropertyFetch[])properties.clone();
		Arrays.sort(sortedProperties, new Comparator<PropertyFetch>() {
			public int compare(PropertyFetch a, PropertyFetch b) {
				return a.getProperty().getPropertyId().compareTo(b.getProperty().getPropertyId());
			}
		});
		
		for (int i = 0; i < sortedProperties.length; i++) {
			if (i != 0 || includeDefault)
				b.append(';');
			b.append(sortedProperties[i].toString());
		}
		
		return b.toString();
	}
}

package com.dumbhippo.dm.schema;

import java.util.List;

import javassist.CtMethod;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.annotations.DMFilter;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.annotations.ViewerDependent;
import com.dumbhippo.dm.fetch.FetchVisitor;

public class MultiPlainPropertyHolder  extends PlainPropertyHolder {
	public MultiPlainPropertyHolder(DMClassHolder<? extends DMObject> declaringClassHolder, CtMethod ctMethod, Class<?> elementType, DMProperty annotation, DMFilter filter, ViewerDependent viewerDependent) {
		super(declaringClassHolder, ctMethod, elementType, annotation, filter, viewerDependent);
	}

	@Override
	public String getUnboxPrefix() {
		return "(java.util.List)";
	}
	
	@Override
	public String getUnboxSuffix() {
		return "";
	}

	@Override
	public void visitProperty(DMSession session, DMObject object, FetchVisitor visitor) {
		for (Object value : (List<?>)getRawPropertyValue(object))
			visitor.plainProperty(this, value);
	}
}

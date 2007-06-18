package com.dumbhippo.jive;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.fetch.Fetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.dm.parser.ParseException;
import com.dumbhippo.dm.schema.DMClassHolder;
import com.dumbhippo.dm.schema.DMClassInfo;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

public class MultiQueryIQMethod<K, T extends DMObject<K>> extends QueryIQMethod {
	static final QName FETCH_QNAME = QName.get("fetch", Namespace.get("http://mugshot.org/p/system"));
	
	private DMClassHolder<K, T> classHolder;

	public MultiQueryIQMethod(DMClassHolder<K,T> classHolder, AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		super(handler, method, annotation);
		
		this.classHolder = classHolder;
	}

	@Override
	public void doIQ(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException, RetryException {
		@SuppressWarnings("unchecked")
		Collection<? extends T> resultObjects = (Collection)invokeMethod(getParams(viewpoint, request));
		
		DMSession session = DataService.currentSessionRO();
		Element root = reply.setChildElement(annotation.name(), handler.getInfo().getNamespace());
		String fetchString = request.getChildElement().attributeValue(FETCH_QNAME, "fetch");
		if (fetchString == null)
			fetchString = "+";
		
		FetchNode fetchNode;
		try {
			fetchNode = FetchParser.parse(fetchString);
		} catch (ParseException e) {
			throw IQException.createBadRequest("Error in fetch attribute: " + e.getMessage());
		} 
		
		Fetch<K,T> fetch = fetchNode.bind(classHolder);
		
		XmppFetchVisitor visitor = new XmppFetchVisitor(root, session.getModel());
		
		for (T object : resultObjects) {
			session.visitFetch(object, fetch, visitor);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static MultiQueryIQMethod<?,?> getForClassInfo(DMClassInfo<?,?> classInfo, AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		DMClassHolder classHolder = DataService.getModel().getClassHolder(classInfo.getClass());
		
		return new MultiQueryIQMethod(classHolder, handler, method, annotation);
	}
	
	public static MultiQueryIQMethod<?,?> getForMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		Type genericType = method.getGenericReturnType();
		
		if (!(genericType instanceof ParameterizedType))
			throw new RuntimeException(method + ": return type is not parameterized");
		
		ParameterizedType paramType = (ParameterizedType)genericType;
		
		// We should have checked before calling, but just in case
		Class<?> rawType = (Class<?>)paramType.getRawType();
		if (!Collection.class.isAssignableFrom(rawType))
			throw new RuntimeException(method + ": return type isn't a subclass of Collection");
		
		if (paramType.getActualTypeArguments().length != 1)
			throw new RuntimeException(method + ": couldn't understand type arguments to parameterized return type");
			
		Type genericElementType = paramType.getActualTypeArguments()[0];
		Class<?> elementType;
		
		if (genericElementType instanceof Class<?>)
			elementType = (Class<?>)genericElementType;
		else if (genericElementType instanceof ParameterizedType)
			elementType = (Class<?>)((ParameterizedType)genericElementType).getRawType();
		else
			throw new RuntimeException(method + ": unexpected non-class type");
		
		return getForClassInfo(DMClassInfo.getForClass(elementType), handler, method, annotation);
	}
}

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
import com.dumbhippo.dm.fetch.BoundFetch;
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

	@SuppressWarnings("unchecked")
	private T reattachInSession(T object, DMSession session) {
		return (T)session.findUnchecked(object.getStoreKey());
		
	}
	
	@Override
	public void doIQPhase2(UserViewpoint viewpoint, IQ request, IQ reply, Object resultObject) throws IQException, RetryException {
		@SuppressWarnings("unchecked")
		Collection<? extends T> resultObjects = (Collection)resultObject;
		
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
		
		BoundFetch<K,T> fetch = fetchNode.bind(classHolder);
		
		XmppFetchVisitor visitor = new XmppFetchVisitor(root, session.getModel());
		
		for (T object : resultObjects) {
			if (annotation.type() == IQ.Type.set)
				object = reattachInSession(object, session);
			
			session.visitFetch(object, fetch, visitor);
		}
		
		visitor.finish();
	}
	
	// This "casts" two classes K and T to be related, note we need a K and a T in order to construct
	// MultiQueryIQMethod without a warning about a bare type
	@SuppressWarnings("unchecked")
	private static <K, T extends DMObject<K>> MultiQueryIQMethod<K,T> newMultiQueryIQMethodHack(DMClassHolder<?,? extends DMObject<?>> classHolder,
			AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		return new MultiQueryIQMethod<K,T>((DMClassHolder<K,T>)classHolder, handler, method, annotation);
	}
	
	public static <K, T extends DMObject<K>> MultiQueryIQMethod<K,T> getForClassInfo(DMClassInfo<?,?> classInfo,
			AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		DMClassHolder<?,?> classHolder = DataService.getModel().getClassHolder(classInfo.getObjectClass());
		
		return newMultiQueryIQMethodHack(classHolder, handler, method, annotation);
	}
	
	public static MultiQueryIQMethod<?,? extends DMObject<?>> getForMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
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

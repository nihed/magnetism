package com.dumbhippo.jive;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMSession;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

public abstract class QueryIQMethod extends AnnotatedIQMethod {
	static final QName FETCH_QNAME = QName.get("fetch", Namespace.get("http://mugshot.org/p/system"));
	ParamInfo[] paramInfo;
	
	protected QueryIQMethod(AnnotatedIQHandler handler, Method method, IQMethod annotation) {
		super(handler, method, annotation);
		
		Class<?>[] paramTypes = method.getParameterTypes();
		paramInfo = new ParamInfo[paramTypes.length];
		
		IQParams paramsAnnotation = method.getAnnotation(IQParams.class);
		if (paramsAnnotation == null)
			throw new RuntimeException("IQParams annotation missing on method " + method.getName());
		
		int j = 0;
		for (int i = 0; i < paramTypes.length; i++) {
			Class<?> paramType = paramTypes[i];
			
			if (paramType == UserViewpoint.class || paramType == Viewpoint.class) {
				paramInfo[i] = new ViewpointParamInfo();
				continue;
			}
			 
			if (j == paramsAnnotation.value().length) 
				throw new RuntimeException(method + ": @IQParams annotation doesn't have enough elements");
			
			String nameAndDefault = paramsAnnotation.value()[j++];
			String name;
			String defaultValue;
			int equalPos = nameAndDefault.indexOf("=");
			if (equalPos >= 0) {
				name = nameAndDefault.substring(0, equalPos);
				defaultValue = nameAndDefault.substring(equalPos + 1);
			} else {
				name = nameAndDefault;
				defaultValue = null;
			}
			
			if (name.length() == 0) {
				throw new RuntimeException(method + ": Empty parameter name");
			}
			
			if (paramType == String.class) {
				paramInfo[i] = new StringParamInfo(name, defaultValue);
			} else if (paramType == Guid.class) {
				paramInfo[i] = new GuidParamInfo(name, defaultValue);
			} else if (paramType == Boolean.TYPE) {
				paramInfo[i] = new BooleanParamInfo(name, defaultValue);
			} else if (paramType == Integer.TYPE) {
				paramInfo[i] = new IntegerParamInfo(name, defaultValue);
			} else if (paramType.isAssignableFrom(UserViewpoint.class)) {
				paramInfo[i] = new ViewpointParamInfo();
			} else if (DMObject.class.isAssignableFrom(paramType)) {
				paramInfo[i] = new DMOParamInfo(name, defaultValue, paramType);
			} else {
				throw new RuntimeException(method + ": Unknown parameter type " + paramType);
			}
		}
		
		if (j != paramsAnnotation.value().length) {
			throw new RuntimeException(method + ": @IQParams annotation has too many elements");
		}
	}
	
	public FetchNode getFetchNode(IQ request) throws IQException {
		String fetchString = request.getChildElement().attributeValue(FETCH_QNAME, "+");

		try {
			return FetchParser.parse(fetchString);
		} catch (com.dumbhippo.dm.parser.ParseException e) {
			throw IQException.createBadRequest("Error in fetch attribute: " + e.getMessage());
		} 
	}
	
	public Object[] getParams(UserViewpoint viewpoint, IQ request) throws IQException {
		Object[] params = new Object[paramInfo.length];
		Map<String, String> paramMap = new HashMap<String, String>(paramInfo.length * 2);
		
		Iterator iterator = request.getChildElement().elementIterator("param");
		while (iterator.hasNext()) {
			Element element = (Element)iterator.next();
			String name = element.attributeValue("name");
			if (name != null)
				paramMap.put(name, element.getText());
		}
		
		for (int i = 0; i < params.length; i++) {
			params[i] = paramInfo[i].get(viewpoint, paramMap);
		}
		
		return params;
	}
	
	private static abstract class ParamInfo {
		protected String name;
		protected boolean optional; 
		protected Object defaultValue;
		
		protected ParamInfo(String name, String defaultValue) {
			this.name = name;
			
			if (defaultValue == null)
				optional = false;
			else {
				optional = true;
				
				if ("null".equals(defaultValue))
					defaultValue = null;
				else {
					try {
						this.defaultValue = parse(defaultValue);
					} catch (IQException e) {
						throw new RuntimeException("Cannot parse default value for parameter " + name, e);
					} 
				}
			}
		}

		public Object get(UserViewpoint viewpoint, Map<String, String> paramMap) throws IQException {
			String value = paramMap.get(name);
			if (value == null && !optional)
				throw IQException.createBadRequest("Parameter " + name + " is required");
			
			return parse(value);
		}
		
		public abstract Object parse(String value) throws IQException;
	}
	
	private static class GuidParamInfo extends ParamInfo {
		protected GuidParamInfo(String name, String defaultValue) {
			super(name, defaultValue);
		}

		@Override
		public Object parse(String value) throws IQException {
			try {
				return new Guid(value);
			} catch (ParseException e) {
				throw IQException.createBadRequest("Bad GUID value for parameter " + name);
			}
		}
	}
	
	private static class StringParamInfo extends ParamInfo {
		protected StringParamInfo(String name, String defaultValue) {
			super(name, defaultValue);
		}

		@Override
		public Object parse(String value) throws IQException {
			return value;
		}
	}
	
	private static class BooleanParamInfo extends ParamInfo {
		protected BooleanParamInfo(String name, String defaultValue) {
			super(name, defaultValue);
			if (optional && defaultValue == null)
				throw new RuntimeException("null is not a valid default for a boolean parameter");
		}

		@Override
		public Object parse(String value) throws IQException {
			return Boolean.parseBoolean(value);
		}
	}
	
	private static class IntegerParamInfo extends ParamInfo {
		protected IntegerParamInfo(String name, String defaultValue) {
			super(name, defaultValue);
			if (optional && defaultValue == null)
				throw new RuntimeException("null is not a valid default for an integer parameter");
		}

		@Override
		public Object parse(String value) throws IQException {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw IQException.createBadRequest("Bad integer value for parameter " + value);
			}
		}
	}
	
	private static class DMOParamInfo extends ParamInfo {
		private Class<?> clazz;
		
		protected DMOParamInfo(String name, String defaultValue, Class<?> clazz) {
			super(name, defaultValue);
			this.clazz = clazz;
		}

		@Override
		public Object parse(String value) throws IQException {
			try {
				DMSession session = DataService.getModel().currentSession();
				DMObject<?> dmo = session.find(value);
				
				if (clazz.isAssignableFrom(dmo.getClass()))
					return dmo;
				else
					throw IQException.createBadRequest("Resource is not of type " + clazz.getSimpleName());
			} catch (NotFoundException e) {
				throw new IQException(PacketError.Condition.item_not_found, PacketError.Type.cancel, e.getMessage());
			}
		}
	}
	
	private static class ViewpointParamInfo extends ParamInfo {
		protected ViewpointParamInfo() {
			super(null, null);
		}

		@Override
		public Object parse(String value) throws IQException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Object get(UserViewpoint viewpoint, Map<String,String> paramMap) throws IQException {
			return viewpoint;
		}
	}
}

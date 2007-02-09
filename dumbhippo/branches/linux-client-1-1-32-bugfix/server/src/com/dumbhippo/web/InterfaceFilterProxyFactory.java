package com.dumbhippo.web;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.dumbhippo.server.BanFromWebTier;


/**
 * 
 * This class just delegates to another class, but implements only the specified
 * interfaces. We use this to filter out interfaces we don't want sent over
 * XML-RPC.
 * 
 * @author hp
 * 
 */
public class InterfaceFilterProxyFactory {

	static class Handler implements InvocationHandler {

		//static private final Logger logger = GlobalSetup.getLogger(Handler.class);
		
		private Object delegate;

		Handler(Object delegate) {
			this.delegate = delegate;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.isAnnotationPresent(BanFromWebTier.class))
				throw new IllegalStateException("Method " + method.getName() + " on object "
						+ delegate.getClass().getCanonicalName() + " implementing "
						+ delegate.getClass().getInterfaces() + " is banned from the web tier");
			
			/* 
			logger.info("Invoking method " + method.getName());
			for (Class e : method.getExceptionTypes()) {
				logger.info("Method " + method.getName() + " throws " + e.getCanonicalName());
			}
			*/
			
			// method.invoke() wraps anything the method throws in an InvocationTargetException
			// and we have to unpack it again. This does not catch exceptions from method.invoke() itself
			try {
				return method.invoke(delegate, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}

	static public Object newProxyInstanceArray(Object delegate, Class<?>[] interfaces) {
		Handler h = new Handler(delegate);

		return Proxy.newProxyInstance(delegate.getClass().getClassLoader(), interfaces, h);
	}

	@SuppressWarnings("unchecked")
	static public <T> T newProxyInstance(T delegate, Class<T> iface) {
		return iface.cast(newProxyInstanceArray(delegate, new Class[] { iface }));
	}
}

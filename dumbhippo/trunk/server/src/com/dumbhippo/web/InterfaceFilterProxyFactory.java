package com.dumbhippo.web;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.dumbhippo.server.AjaxGlueXmlRpc;


/**
 * 
 * This class just delegates to another class, but implements only the specified interfaces.
 * We use this to filter out interfaces we don't want sent over XML-RPC. 
 * 
 * @author hp
 *
 */
public class InterfaceFilterProxyFactory {

	static class Handler implements InvocationHandler {

		private Object delegate;

		Handler(Object delegate) {
			this.delegate = delegate;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return method.invoke(delegate, args);
		}
	}
	
	static public Object newProxyInstance(Object delegate, Class<?>[] interfaces) {
		Handler h = new Handler(delegate);
		
		return Proxy.newProxyInstance(delegate.getClass().getClassLoader(), interfaces, h);
	}
}

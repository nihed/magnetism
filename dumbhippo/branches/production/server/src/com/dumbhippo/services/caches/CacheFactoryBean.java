package com.dumbhippo.services.caches;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

/** 
 * Because this factory is thread-local, all the objects it creates and caches are also thread-local, in the same way 
 * they would be if jboss managed them (hopefully).
 * 
 * The basic idea is to emulate a very feature-poor EJB container sufficient for the service cache beans, in order to avoid 
 * the jboss TransactionAttribute bug that fatally hoses us.
 * 
 * This is the least-intrusive way to work around the bug I could come up with... it's pretty scary though.
 * 
 * @author Havoc Pennington
 *
 */
@Stateless
public class CacheFactoryBean implements CacheFactory {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(CacheFactoryBean.class);
	
	private HashMap<Class<?>,Object> cachedObjects;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;

	public CacheFactoryBean() {
		cachedObjects = new HashMap<Class<?>,Object>();
	}
	
	private static class HippoTxInterceptor implements InvocationHandler {
		
		private TransactionRunner runner;
		private Object instance;
		
		HippoTxInterceptor(Object instance, TransactionRunner runner) {
			this.instance = instance;
			this.runner = runner;
		}
		
		public Object invoke(final Object proxy, final Method proxyMethod, final Object[] args) throws Throwable {
		
			//logger.debug("Invoking method {}.{}", proxyMethod.getDeclaringClass().getName(), proxyMethod.getName());

			// The method passed in is the one declared in the proxy class, but to get the annotations we need the 
			// one declared in the real class
			
			final Method instanceMethod;
			try {
				instanceMethod = instance.getClass().getMethod(proxyMethod.getName(), (Class[]) proxyMethod.getParameterTypes());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("could not find same method on delegate object, params " + Arrays.toString(proxyMethod.getParameterTypes()));
			}
			
			//logger.debug("Replacing method with method on delegate instance {}.{}", instanceMethod.getDeclaringClass().getName(),
			//		instanceMethod.getName());
			
			TransactionAttributeType txType;
			
			TransactionAttribute txAttribute = instanceMethod.getAnnotation(TransactionAttribute.class);
			if (txAttribute != null)
				; //logger.debug("found txAttribute on method {}, {}", instanceMethod.getName(), txAttribute.value());
			if (txAttribute == null) {
				txAttribute = instanceMethod.getDeclaringClass().getAnnotation(TransactionAttribute.class);
				if (txAttribute != null)
					; //logger.debug("found txAttribute on class {}, {}", instanceMethod.getDeclaringClass().getName(), txAttribute.value());
			}
			
			if (txAttribute != null) {
				txType = txAttribute.value();
			} else {
				; // logger.debug("using default txAttribute REQUIRED on method {} in class {}", instanceMethod.getName(), instanceMethod.getDeclaringClass().getName());
				txType = TransactionAttributeType.REQUIRED;
			}
			
			try {
				switch (txType) {
				case MANDATORY:
					EJBUtil.assertHaveTransaction();
					return instanceMethod.invoke(instance, args);
				case NEVER:
					EJBUtil.assertNoTransaction();
					return instanceMethod.invoke(instance, args);
				case NOT_SUPPORTED:
					throw new RuntimeException("we don't know how to suspend a transaction here");
					
				case REQUIRED:
					if (EJBUtil.isTransactionActive()) {
						return instanceMethod.invoke(instance, args);
					} else {
						return runner.runTaskInNewTransaction(new Callable<Object>() {
		
							public Object call() throws Exception {
								return instanceMethod.invoke(instance, args);
							}
							
						});
					}
				case REQUIRES_NEW:
					return runner.runTaskInNewTransaction(new Callable<Object>() {
	
						public Object call() throws Exception {
							return instanceMethod.invoke(instance, args);
						}
						
					});
				case SUPPORTS:
					return instanceMethod.invoke(instance, args);
				}
				
				throw new RuntimeException("Unhandled transaction attribute " + txType);
			} catch (InvocationTargetException e) {
				// The method itself threw an exception, propagate it back
				throw e.getCause();
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (Exception e) {
				// Should not be hit, runTaskInNewTransaction should only throw one of the above
				throw new RuntimeException("Unexpected exception invoking method with transaction", e);
			}
		}
	}
	
	private void setField(Object o, Field f, Object value) {
		/*
		if (logger.isDebugEnabled())
			logger.debug("injecting value of type " + (value != null ? value.getClass().getName() : "null")
					+ " into field " + f.getName() + " of object " + o.getClass().getName());
		*/
		
		try {
			f.setAccessible(true);
			f.set(o, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	private <T> T instantiateAndInject(Class<T> clazz) {
		// logger.debug("Instantiating " + clazz.getName());

		List<Method> postConstructs = null;
		
		T o;
		try {
			o = clazz.newInstance();
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Can't instantiate " + clazz.getName(), e);
		}
		
		for (Class<?> c = clazz; c.getPackage() == clazz.getPackage(); c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.isAnnotationPresent(EJB.class)) {
					Object ejb = EJBUtil.defaultLookup(f.getType());
					setField(o, f, ejb);
				} else if (f.isAnnotationPresent(PersistenceContext.class)) {
					// assume context=dumbhippo
					setField(o, f, em);
				}
			}
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(PostConstruct.class)) {
					if (postConstructs == null) {
						postConstructs = new ArrayList<Method>();
					}
					postConstructs.add(m);
				}
			}
		}
		
		if (postConstructs != null) {
			// we appended as we walked up the class hierarchy, so superclass 
			// initializers are last; we want them first
			Collections.reverse(postConstructs);
			for (Method m : postConstructs) {
				try {
					m.setAccessible(true);
					m.invoke(o);
				} catch (Exception e) {
					throw new RuntimeException("Problem in @PostConstruct of " + clazz.getName() + ": " + e.getMessage(), e);
				}
			}
		}
		
		return o;
	}

	
	/** 
	 * This is a simulation of EJB session beans that doesn't have the jboss 
	 * AOP buggy behavior when mixing generics with annotations. When the 
	 * jboss bug is fixed, this mess of code can be replaced with a normal 
	 * EJB lookup.
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public <CacheType> CacheType lookup(Class<CacheType> klass) {
		Object o = cachedObjects.get(klass);
		 
		if (o != null)
			return klass.cast(o);
		
		String beanName = klass.getName() + "Bean";
		
		//logger.debug("loading cache bean {} for interface {}", beanName, klass.getName());
		
		Class<?> beanClass;
		try {
			beanClass = getClass().getClassLoader().loadClass(beanName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		CacheType bean = klass.cast(instantiateAndInject(beanClass));
		
		CacheType proxy = klass.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { klass },
				new HippoTxInterceptor(bean, runner))); 
		
		cachedObjects.put(klass, proxy);
		
		return proxy;
	}
	
	static public <CacheType> CacheType defaultLookup(Class<CacheType> klass) {
		return EJBUtil.defaultLookup(CacheFactory.class).lookup(klass);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void injectCaches(Object o) {
		Class<?> clazz = o.getClass();
		for (Class<?> c = clazz; c.getPackage() == clazz.getPackage(); c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.isAnnotationPresent(WebServiceCache.class)) {
					logger.debug("Injecting cache into field {} of class {}", f.getName(), c.getName());
					Object cache = lookup(f.getType());
					setField(o, f, cache);
				}
			}
		}
	}
}

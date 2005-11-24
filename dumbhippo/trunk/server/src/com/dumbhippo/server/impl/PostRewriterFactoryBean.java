package com.dumbhippo.server.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.EJB;
import javax.ejb.PostConstruct;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostRewriter;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.PostRewriterFactory;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.rewriters.AmazonRewriter;

@Stateless
public class PostRewriterFactoryBean implements PostRewriterFactory {
	
	static private final Log logger = GlobalSetup.getLog(PostRewriterFactoryBean.class);
	
	private Map<String,Method> factoryMethods;
	private ExecutorService threadPool;
	
	@EJB
	private Configuration configuration;
	
	private String getDomain(String host) {
		//	get the "foo.com" in "bar.foo.com"
		int period = host.lastIndexOf('.');
		if (period > 0)
			period = host.lastIndexOf('.', period - 1);
		if (period < 0)
			period = 0;
		else
			period += 1; // skip back past the '.'
		return host.substring(period);
	}
	
	@PostConstruct
	public void init() {
		
		logger.debug("loading post rewriters");
		
		final Class<?> rewriters[] = {
				AmazonRewriter.class
		};
		
		factoryMethods = new HashMap<String,Method>();
	
		for (Class<?> c : rewriters) {
			Method getDomains;
			Method newInstance;
			try {
				getDomains = c.getMethod("getDomains");
				newInstance = c.getMethod("newInstance", Configuration.class);
			} catch (SecurityException e) {
				logger.trace(e);
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				logger.trace(e);
				throw new RuntimeException(e);
			}
			if (!String[].class.isAssignableFrom(getDomains.getReturnType()))
				throw new RuntimeException("getDomain() for " + c.getName() + " should return String[]");
				
			if (!PostRewriter.class.isAssignableFrom(newInstance.getReturnType()))
				throw new RuntimeException("newInstance() for " + c.getName() + " should return a PostRewriter");
			
			String[] domains;
			try {
				Object result = getDomains.invoke(null);
				domains = (String[]) result;
			} catch (IllegalArgumentException e) {
				logger.trace(e);
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				logger.trace(e);
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				logger.trace(e);
				throw new RuntimeException(e);
			}
			
			for (String d : domains) {
				if (factoryMethods.containsKey(d)) {
					throw new RuntimeException("two post rewriters both handle domain " + d);
				}
				
				if (!getDomain(d).equals(d))
					throw new RuntimeException("right now rewriters can't cover subdomains, " + d);
				
				logger.debug("mapping domain " + d + " to " + c.getName());
				factoryMethods.put(d, newInstance);
			}
		}
		
		threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("PostRewriter async");
				return t;
			}
		});
	}
	
	private PostRewriter newInstance(Method factoryMethod) {
		try {
			Object result = factoryMethod.invoke(null, configuration);
			return (PostRewriter) result;
		} catch (IllegalArgumentException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			logger.trace(e);
			throw new RuntimeException(e.getCause());
		}
	}
	
	public void loadRewriter(Viewpoint viewpoint, Post post) {
		Set<Resource> resources = post.getResources();

		String link = null;
		if (!resources.isEmpty()) {
			for (Resource r : resources) {
				if (r instanceof LinkResource) {
					link = ((LinkResource)r).getUrl();
					break;
				}
			}
		}
		
		if (link == null)
			return;
		
		URL url;
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			logger.debug("Not analyzing malformed url " + link, e);
			return;
		}
		
		String domain = getDomain(url.getHost());
		
		logger.debug("matching rewriter on '" + domain + "' from link '" + link + "'");
		
		Method factoryMethod = factoryMethods.get(domain);
		if (factoryMethod == null)
			return;
		
		logger.debug("rewriting post with " + factoryMethod.getDeclaringClass().getName());
		
		PostRewriter rewriter = newInstance(factoryMethod);
		post.bindRewriter(rewriter, url);
		
		Runnable asyncTask = rewriter.getAsyncTask();
		if (asyncTask != null)
			threadPool.execute(asyncTask);
	}
}

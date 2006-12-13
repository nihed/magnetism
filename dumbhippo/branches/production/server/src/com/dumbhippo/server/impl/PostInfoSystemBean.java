package com.dumbhippo.server.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.PostInfoSystem;
import com.dumbhippo.server.updaters.AmazonUpdater;
import com.dumbhippo.server.updaters.EbayUpdater;
import com.dumbhippo.server.updaters.PostUpdater;

@Stateless
public class PostInfoSystemBean implements PostInfoSystem {

	static private final Logger logger = GlobalSetup.getLogger(PostInfoSystemBean.class);
	static private final int MAX_UPDATE_RATE = 1000 * 60 * 30; // never update more often than every 30 minutes
	
	private ExecutorService threadPool;	
	private Map<String,Method> factoryMethods;
	
	/**
	 * This is a little tricky; it's a cache to avoid 
	 * starting too many updaters on the same post, as long 
	 * as the app server uses this same bean. Adding/removing 
	 * entries is just heuristic.
	 */
	private Map<Guid,Future<PostInfo>> cachedUpdaters;
	
	@EJB
	private Configuration configuration;
	
	private static class UpdateTask implements Callable<PostInfo> {
		private PostUpdater updater;
		
		UpdateTask(PostUpdater updater) {
			this.updater = updater;
		}
		
		public PostInfo call() throws Exception {
			logger.debug("Starting PostInfo retrieval");
			PostInfo info = updater.getUpdate();
			logger.debug("Completed PostInfo retrieval");			
			return info;
		}
	}
	
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
		
		final Class<?> updaters[] = {
				AmazonUpdater.class,
				EbayUpdater.class
		};
		
		factoryMethods = new HashMap<String,Method>();
	
		for (Class<?> c : updaters) {
			Method getDomains;
			Method newInstance;
			try {
				getDomains = c.getMethod("getDomains");
				newInstance = c.getMethod("newInstance", Configuration.class);
			} catch (SecurityException e) {
				logger.error("Security exception loading post updater", e);
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				logger.error("Missing expected methods loading post updater", e);
				throw new RuntimeException(e);
			}
			if (!String[].class.isAssignableFrom(getDomains.getReturnType()))
				throw new RuntimeException("getDomain() for " + c.getName() + " should return String[]");
				
			if (!PostUpdater.class.isAssignableFrom(newInstance.getReturnType()))
				throw new RuntimeException("newInstance() for " + c.getName() + " should return a PostUpdater");
			
			String[] domains;
			try {
				Object result = getDomains.invoke(null);
				domains = (String[]) result;
			} catch (IllegalArgumentException e) {
				logger.error("error", e);
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				logger.error("error", e);
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				logger.error("error", e);
				throw new RuntimeException(e);
			}
			
			for (String d : domains) {
				if (factoryMethods.containsKey(d)) {
					throw new RuntimeException("two post updaters both handle domain " + d);
				}
				
				if (!getDomain(d).equals(d))
					throw new RuntimeException("right now updaters can't cover subdomains, " + d);
				
				logger.debug("mapping domain {} to {}", d, c.getName());
				factoryMethods.put(d, newInstance);
			}
		}
		
		threadPool = ThreadUtils.newCachedThreadPool("PostInfoSystemBean");
	
		// note that this isn't global; it's per-stateless-bean. i.e. 
		// it's just an optimization and you don't want to rely on it 
		// for semantics
		cachedUpdaters = new HashMap<Guid,Future<PostInfo>>();
	}
	
	private PostUpdater newInstance(Method factoryMethod) {
		try {
			Object result = factoryMethod.invoke(null, configuration);
			return (PostUpdater) result;
		} catch (IllegalArgumentException e) {
			logger.error("Failed to invoke PostUpdater factory method {}", e.getMessage());
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			logger.error("Failed to invoke PostUpdater factory method {}", e.getMessage());
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			logger.error("Failure in PostUpdater factory method {}", e.getCause().getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private Future<PostInfo> getUpdateTask(Post post) {
		
		Guid guid = post.getGuid();
		
		if (cachedUpdaters.containsKey(guid)) {
			logger.debug("Pulling post update task from cachedUpdaters");
			Future<PostInfo> task = cachedUpdaters.get(guid);
			return task;
		}
		
		// the individual updaters do their own check; but they can't 
		// force us to update more often than this. This avoids loading 
		// the updaters every single time.
		Date maxUpdateAgo = new Date(System.currentTimeMillis() - MAX_UPDATE_RATE);
		Date lastUpdate = post.getInfoDate();
		if (lastUpdate != null && lastUpdate.after(maxUpdateAgo)) {
			// Don't enable this for now, it's spammy
			// logger.debug("Post was updated in the last {} minutes, not updating", 
			//		MAX_UPDATE_RATE / 1000 / 60);
			return null; // nothing to do
		}

		URL url = post.getUrl();
		if (url == null) {
			logger.debug("post has no url");
			return null;
		}
		
		String domain = getDomain(url.getHost());
		
		//logger.debug("matching updater on '{}' from link '{}'", domain, url);
		
		Method factoryMethod = factoryMethods.get(domain);
		if (factoryMethod == null)
			return null;
		
		logger.debug("updating post with {}", factoryMethod.getDeclaringClass().getName());
		
		final PostUpdater updater = newInstance(factoryMethod);
		
		if (updater == null)
			return null;
		
		updater.bind(post, url);
		
		FutureTask<PostInfo> task = new FutureTask<PostInfo>(new UpdateTask(updater));
		
		logger.debug("Adding update task to cachedUpdaters");
		cachedUpdaters.put(guid, task);
		
		if (updater.isUpdated()) {
			// just synchronously finish the task
			//logger.debug("Running updater synchronously");
			task.run();
		} else {
			// we'll have to do some async work, so get it started.
			logger.debug("Starting updater asynchronously");
			threadPool.execute(task);
		}
		
		return task;
	}
	
	public void hintWillUpdateSoon(Post post) {
		getUpdateTask(post); // called for side effect of creating cachedUpdaters entry and starting update thread
	}

	public void updatePostInfo(Post post) {
		if (post == null)
			throw new IllegalArgumentException("null post");
		/*
		String info = post.getInfo();

		if (info != null)
			logger.debug("Updating, old post info: {}", info.replace("\n",""));
		else
			logger.debug("Updating, no previous post info");
		*/
		
		Future<PostInfo> task = getUpdateTask(post);
		PostInfo newPostInfo = null;
		if (task != null) {
			while (true) {
				try {
					//logger.debug("Getting result from updater for {}", post);
					newPostInfo = task.get();
					cachedUpdaters.remove(post.getGuid());
					break;
				} catch (InterruptedException e) {
					// just retry
				} catch (ExecutionException e) {
					throw new RuntimeException("updater task failed", e);
				}
			}
		} else {
			//logger.debug("No updater for {}", post);
		}
		
		if (newPostInfo == null) {
			//logger.debug("No new post info for post {} keeping old: {}", post, post.getInfo());
			return; // nothing to update
		}
		
		logger.debug("Got new post info for post {}: {}", post, newPostInfo);
		post.setInfoDate(new Date());
		post.setPostInfo(newPostInfo);
	}
}

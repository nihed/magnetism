package com.dumbhippo.server.updaters;

import java.net.URL;
import java.util.Date;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.postinfo.PostInfoType;

abstract public class AbstractUpdater implements PostUpdater {

	static private final Log logger = GlobalSetup.getLog(AbstractUpdater.class);
	
	private PostInfo postInfo;
	private URL boundUrl;
	private boolean updated;
	
	protected AbstractUpdater() {
	}

	/**
	 * The timeout to wait to get the update; this can be relatively long 
	 * since normally we'll cache the update (or failure to update) and we 
	 * don't want to break things for the whole cache period too lightly
	 * @return the timeout for the update
	 */
	protected int getUpdateTimeoutMilliseconds() {
		return 1000 * 12;
	}
	
	/** 
	 * Override this to change from the default
	 * @return the max age of a post before we try to update it
	 */
	protected int getMaxAgeMilliseconds() {
		return 1000 * 60 * 30; // 30 minutes
	}
	
	/**
	 * Override this to create a post info of type other than generic
	 * @return the type of post info we need to update
	 */
	protected PostInfoType getType() {
		return PostInfoType.GENERIC;
	}
	
	public void bind(Post post, URL url) {
		PostInfo old = post.getPostInfo();
		if (old != null) {
			postInfo = PostInfo.newInstance(old, getType());
		} else {
			postInfo = PostInfo.newInstance(getType());
		}
		
		logger.debug("Old post info: " + old + " new post info base: " + postInfo);
		
		boundUrl = url;
		
		Date maxAgeAgo = new Date(System.currentTimeMillis() - getMaxAgeMilliseconds());
		Date lastUpdate = post.getInfoDate();
		
		if (lastUpdate != null  && lastUpdate.after(maxAgeAgo)) {
			logger.debug("Post info is recent enough, not updating (" + lastUpdate + " is after " + maxAgeAgo + ")");
			updated = true;
		}
		
		// Note that we DO NOT save the Post object, since that would create crazy concurrency issues
		// and we don't want to directly modify it anyhow. We only save the URL (immutable) and a copy 
		// of the PostInfo
	}

	public boolean isUpdated() {
		return updated;
	}
	
	abstract protected void update(PostInfo postInfo, URL url);

	public PostInfo getUpdate() {
		if (!updated) {
			update(postInfo, boundUrl);
			postInfo.makeImmutable();
			updated = true;
		}
		return postInfo;
	}
}

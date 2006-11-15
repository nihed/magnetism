package com.dumbhippo.search;

import org.jboss.annotation.ejb.Service;

import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.SimpleServiceMBean;

@Service
public class SearchSystemBean implements SearchSystem, SimpleServiceMBean {
	private JmsProducer queue;

	public void start() throws Exception {
		 queue = new JmsProducer(IndexTask.QUEUE_NAME, JmsConnectionType.TRANSACTED_IN_SERVER);
	}

	public void stop() throws Exception {
		queue.close();
		queue = null;
	}

	public void indexPost(Post post, boolean reindex) {
		queue.sendObjectMessage(new IndexTask(IndexTask.Type.INDEX_POST, post.getGuid(), reindex));
	}

	public void indexGroup(Group group, boolean reindex) {
		queue.sendObjectMessage(new IndexTask(IndexTask.Type.INDEX_GROUP, group.getGuid(), reindex));
	}

	public void indexTrack(Track track) {
		queue.sendObjectMessage(new IndexTask(IndexTask.Type.INDEX_TRACK, track.getId(), false));
	}

	public void reindexAll() {
		queue.sendObjectMessage(new IndexTask(IndexTask.Type.INDEX_ALL, true));
	}
}

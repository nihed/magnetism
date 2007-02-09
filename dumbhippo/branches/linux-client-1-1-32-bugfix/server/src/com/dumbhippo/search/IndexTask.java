package com.dumbhippo.search;

import java.io.Serializable;

/**
 * This class represents a task that is queued via JMS for the 
 * cluster-singleton indexer service.
 *  
 * @author otaylor
 */
public class IndexTask implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String QUEUE_NAME = "queue/IndexQueue";

	public enum Type {
		INDEX_GROUP,
		INDEX_POST,
		INDEX_TRACK,
		INDEX_ALL
	};
	
	private Type type;
	private Object id;
	private boolean reindex;
	
	public IndexTask(Type type, boolean reindex) {
		this.type = type;
		this.reindex = reindex;
	}

	public IndexTask(Type type, Object id, boolean reindex) {
		this.type = type;
		this.id = id;
		this.reindex = reindex;
	}

	public Type getType() {
		return type;
	}
	
	public Object getId() {
		return id;
	}
	
	public boolean isReindex() {
		return reindex;
	}
	
	@Override
	public String toString() {
		return "[IndexTask type=" + type + " id=" + id + "]";
	}
}

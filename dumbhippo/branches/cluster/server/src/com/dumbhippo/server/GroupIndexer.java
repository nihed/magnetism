package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexWriter;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.util.EJBUtil;

public final class GroupIndexer extends Indexer<Group> {
	static GroupIndexer instance = new GroupIndexer();
	
	public static GroupIndexer getInstance() {
		return instance;
	}
	
	private GroupIndexer() {
		super(Group.class);
	}
	
	@Override
	protected String getIndexName() {
		return "Groups";
	}
	
	@Override
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		EJBUtil.defaultLookup(GroupSystem.class).indexGroups(writer, getBuilder(), ids);
	}
	
	@Override
	protected void doIndexAll(IndexWriter writer) throws IOException {
		EJBUtil.defaultLookup(GroupSystem.class).indexAllGroups(writer, getBuilder());
	}
}

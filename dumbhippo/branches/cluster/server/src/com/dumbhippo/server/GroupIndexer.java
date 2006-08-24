package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.util.EJBUtil;

public final class GroupIndexer extends Indexer<Group> {
	private DocumentBuilder<Group> builder;
	
	static GroupIndexer instance = new GroupIndexer();
	
	public static GroupIndexer getInstance() {
		return instance;
	}
	
	private GroupIndexer() {
		builder = new DocumentBuilder<Group>(Group.class, createAnalyzer(), null);
	}
	
	@Override
	protected String getIndexName() {
		return "Groups";
	}
	
	@Override
	protected DocumentBuilder<Group> getBuilder() {
		return builder; 
	}
	
	@Override
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		EJBUtil.defaultLookup(GroupSystem.class).indexGroups(writer, builder, ids);
	}
	
	@Override
	protected void doIndexAll(IndexWriter writer) throws IOException {
		EJBUtil.defaultLookup(GroupSystem.class).indexAllGroups(writer, builder);
	}
}

package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import com.dumbhippo.identity20.Guid;
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
	
	@Override
	protected void doDelete(IndexReader reader, List<Object> ids) throws IOException {
		for (Object o : ids) {
			Guid guid = (Guid)o;
			Term term = new Term("id", guid.toString());
			reader.deleteDocuments(term);
		}
	}	
}

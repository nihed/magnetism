package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;

public final class GroupIndexer extends GuidPersistableIndexer<Group> {
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
	protected List<Guid> loadAllIds() {
		return EJBUtil.defaultLookup(GroupSystem.class).getAllGroupIds();
	}

	@Override
	protected Group loadObject(Guid guid) {
		try {
			return EJBUtil.defaultLookup(GroupSystem.class).lookupGroupById(SystemViewpoint.getInstance(), guid);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
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

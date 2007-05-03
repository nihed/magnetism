package com.dumbhippo.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.util.EJBUtil;

public final class ApplicationIndexer extends UniqueObjectIndexer<Application> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ApplicationIndexer.class);
	
	static ApplicationIndexer instance = new ApplicationIndexer();
	
	public static ApplicationIndexer getInstance() {
		return instance;
	}
	
	private ApplicationIndexer() {
		super(Application.class);
	}
	
	@Override
	protected String getIndexName() {
		return "Applications";
	}
	
	@Override
	protected void doDelete(IndexReader reader, List<Object> ids) throws IOException {
		for (Object o : ids) {
			String id = (String)o;
			Term term = new Term("id", id);
			reader.deleteDocuments(term);
		}
	}

	@Override
	protected List<Serializable> loadAllIds() {
		ApplicationSystem appsys = EJBUtil.defaultLookup(ApplicationSystem.class);		
		return TypeUtils.castList(Serializable.class, appsys.getAllApplicationIds());
	}

	@Override
	protected Application loadObject(Serializable id) {
		ApplicationSystem appsys = EJBUtil.defaultLookup(ApplicationSystem.class);
		Application obj;
		try {
			obj = appsys.lookupById((String) id);
		} catch (NotFoundException e1) {
			throw new RuntimeException(e1);
		}
		return obj;		
	}	
}

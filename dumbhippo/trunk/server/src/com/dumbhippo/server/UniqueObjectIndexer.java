package com.dumbhippo.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.server.util.EJBUtil;

public abstract class UniqueObjectIndexer<T> extends Indexer<T> {
	
	protected UniqueObjectIndexer(Class<T> clazz) {
		super(clazz);
	}

	protected abstract T loadObject(Serializable id);
	
	protected abstract List<Serializable> loadAllIds();

	private void doIndexObjects(final IndexWriter writer, final DocumentBuilder<T> builder, final List<Serializable> ids) {		
		for (final Serializable id : ids) {
			EJBUtil.defaultLookup(TransactionRunner.class).runTaskInNewTransaction(new Runnable() {
				public void run() {
					T obj = loadObject(id);
					// loadObject returns null for objects to skip
					if (obj == null)
						return;
					Document document = builder.getDocument(obj, id);
					try {
						writer.addDocument(document);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}					
				}
			});
		}		
	}
	
	@Override
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		doIndexObjects(writer, getBuilder(), TypeUtils.castList(Serializable.class, ids));
	}

	@Override
	protected void doIndexAll(IndexWriter writer) throws IOException {
		doIndexObjects(writer, getBuilder(), loadAllIds());
	}
}

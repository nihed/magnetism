package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.server.util.EJBUtil;

public abstract class GuidPersistableIndexer<T extends GuidPersistable> extends Indexer<T> {
	
	protected GuidPersistableIndexer(Class<T> clazz) {
		super(clazz);
	}

	protected abstract T loadObject(Guid guid);
	
	protected abstract List<Guid> loadAllIds();

	private void doIndexObjects(final IndexWriter writer, final DocumentBuilder<T> builder, List<Guid> ids) {		
		for (final Guid id : ids) {
			EJBUtil.defaultLookup(TransactionRunner.class).runTaskInNewTransaction(new Runnable() {
				public void run() {
					T obj = loadObject(id);
					// loadObject returns null for objects to skip
					if (obj == null)
						return;
					Document document = builder.getDocument(obj, obj.getId());
					try {
						writer.addDocument(document);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}					
				}
			});
		}		
	}
	
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		doIndexObjects(writer, getBuilder(), TypeUtils.castList(Guid.class, ids));
	}

	protected void doIndexAll(IndexWriter writer) throws IOException {
		doIndexObjects(writer, getBuilder(), loadAllIds());
	}
}

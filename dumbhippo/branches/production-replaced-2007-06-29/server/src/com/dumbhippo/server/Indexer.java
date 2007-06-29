package com.dumbhippo.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.hibernate.lucene.DocumentBuilder;
import org.hibernate.lucene.store.FSDirectoryProvider;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.util.EJBUtil;

public abstract class Indexer<T> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(Indexer.class);

	private static final long READER_EXPIRATION_TIME = 20 * 1000;

	public BlockingQueue<Object> pending;

	private DocumentBuilder<T> builder;

	private IndexerThread indexerThread;	
	
	private static abstract class Expirable<T> {
		private long expirationTime;
		private long duration;
		private T obj;
		public Expirable(long duration) {
			this.duration = duration;
			expirationTime = new Date().getTime() + duration;
		}
		
		public void clear() {
			this.obj = null;
		}
		
		public T get() {
			if (obj == null || new Date().getTime() > expirationTime) {
				if (obj != null)
					expire(obj);
				obj = create();
				expirationTime = new Date().getTime() + duration;
			}
			return obj;
		}
		
		public T getNoCreate() {
			return obj;
		}
		
		protected abstract T create();
		
		protected abstract void expire(T obj);
	}	
	
	private Expirable<IndexReader> reader = new Expirable<IndexReader> (READER_EXPIRATION_TIME) {
		@Override
		protected IndexReader create() {
			try {
				return IndexReader.open(getBuilder().getDirectoryProvider().getDirectory());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}	
		}
		
		@Override
		protected void expire(IndexReader reader) {
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("Couldn't close IndexReader", e);
			}
		}
	};

	private Expirable<IndexSearcher> searcher = new Expirable<IndexSearcher> (READER_EXPIRATION_TIME) {
		@Override
		protected IndexSearcher create() {
			try {
				return new IndexSearcher(getReader());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		protected void expire(IndexSearcher searcher) {
			try {
				searcher.close();
			} catch (IOException e) {
				logger.error("Couldn't close IndexSearcher", e);
			}
		}		
	};

	private static class Reindex {
		private Object id;

		public Reindex(Object id) {
			this.id = id;
		}

		public Object getId() {
			return id;
		}
	}

	private static class ReindexAllMarker {
	}

	protected Indexer(Class<T> clazz) {
		pending = new LinkedBlockingQueue<Object>();

		Configuration config = EJBUtil.defaultLookup(Configuration.class);
		String indexBase = config
				.getPropertyFatalIfUnset(HippoProperty.LUCENE_INDEXDIR);
		Properties properties = new Properties();
		properties.setProperty("indexBase", indexBase);

		FSDirectoryProvider directory = new FSDirectoryProvider();
		directory.initialize(clazz, null, properties);
		builder = new DocumentBuilder<T>(clazz, createAnalyzer(), directory);
	}

	private synchronized void ensureThread() {
		if (indexerThread == null) {
			indexerThread = new IndexerThread();
			indexerThread.start();
		}
	}

	public synchronized void shutdown() {
		if (indexerThread == null) {
			indexerThread.interrupt();
			try {
				indexerThread.join();
			} catch (InterruptedException e) {
				// shouldn't happen, just ignore
			}
		}
	}

	public void index(Object id, boolean reindex) {
		if (reindex)
			pending.add(new Reindex(id));
		else
			pending.add(id);
		ensureThread();
	}

	public void reindexAll() {
		pending.add(new ReindexAllMarker());
		ensureThread();
	}

	public synchronized IndexReader getReader() throws IOException {
		return reader.get();
	}

	public synchronized Searcher getSearcher() throws IOException {
		return searcher.get();
	}

	private synchronized void clearSearcher() {
		if (searcher.getNoCreate() != null) {
			try {
				searcher.getNoCreate().close();
			} catch (IOException e) {
			}
			searcher.clear();
		}
		if (reader.getNoCreate() != null) {
			try {
				reader.getNoCreate().close();
			} catch (IOException e) {
			}
			reader.clear();
		}
	}

	public Analyzer createAnalyzer() {
		return new StopAnalyzer();
	}

	protected DocumentBuilder<T> getBuilder() {
		return builder;
	}

	protected abstract String getIndexName();

	protected abstract void doDelete(IndexReader reader, List<Object> ids)
			throws IOException;
	
	protected abstract void doIndex(IndexWriter writer, List<Object> ids)
			throws IOException;
	
	protected abstract void doIndexAll(IndexWriter writer) throws IOException;

	private class IndexerThread extends Thread {
		public IndexerThread() {
			super("Indexer/" + getIndexName());
		}

		@Override
		public void run() {
			try {
				while (true) {
					List<Object> toDelete = new ArrayList<Object>();
					List<Object> toIndex = new ArrayList<Object>();
					boolean reindex = false;

					Object item = pending.take();
					while (item != null) {
						if (item instanceof ReindexAllMarker) {
							reindex = true;
							toDelete.clear();
							toIndex.clear();
						} else if (item instanceof Reindex) {
							toDelete.add(((Reindex) item).getId());
							toIndex.add(((Reindex) item).getId());
						} else {
							toIndex.add(item);
						}
						item = pending.poll();
					}

					IndexWriter writer = null;					
					try {
						if (reindex)
							logger.info("Reindexing {}", getIndexName());

						if (!reindex && !toDelete.isEmpty()) {
							doDelete(getReader(), toDelete);
							clearSearcher();
						}

						// It's not completely clear that passing 'create =
						// true' here when
						// reindexing is safe when there is an existing
						// IndexReader open,
						// but transient errors during reindexing aren't a big
						// problem
						writer = new IndexWriter(getBuilder()
								.getDirectoryProvider().getDirectory(),
								getBuilder().getAnalyzer(), reindex);

						if (reindex) {
							doIndexAll(writer);
						} else {
							doIndex(writer, toIndex);
						}
						
						if (reindex)
							logger.info("Finished reindexing {}",
									getIndexName());

					} catch (IOException e) {
						logger.error("IOException while indexing "
								+ getIndexName(), e);
					} catch (RuntimeException e) {
						logger.error("Unexpected exception while indexing "
								+ getIndexName(), e);
					} finally {
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException e) {
								logger.error("IOException closing index writer " + getIndexName(), e);
							}
						}

						// If we have an IndexReader cached, close it so new
						// searches
						// will see the post or posts we just indexed.
						clearSearcher();						
					}
				}
			} catch (InterruptedException e) {
				// We're done
			}
		}
	}
}

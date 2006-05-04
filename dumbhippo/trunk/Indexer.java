package com.dumbhippo.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.hibernate.lucene.DocumentBuilder;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public abstract class Indexer<T> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(Indexer.class);
	
	public BlockingQueue<Object> pending;
	
	private IndexReader reader;
	private IndexSearcher searcher;
	private IndexerThread indexerThread;
	
	private static class ReindexMarker {}
	
	public Indexer() {
		pending = new LinkedBlockingQueue<Object>();
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
	
	public void index(List<Object> ids) {
		pending.addAll(ids);
		ensureThread();
	}

	public void reindex() {
		pending.add(new ReindexMarker());
		ensureThread();
	}
	
	public void indexAfterTransaction(final List<Object> ids) {
		TransactionManager tm;
		try {
			tm = (TransactionManager) (new InitialContext()).lookup("java:/TransactionManager");
			tm.getTransaction().registerSynchronization(new Synchronization() {
				public void beforeCompletion() {}

				public void afterCompletion(int arg0) {
					index(ids);
				}
			});
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		} catch (RollbackException e) {
			throw new RuntimeException(e);
		} catch (SystemException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public void indexAfterTransaction(Object id) {
		indexAfterTransaction(Collections.singletonList(id));
	}
	
	public synchronized Searcher getSearcher() throws IOException {
		if (searcher == null) {
			reader = IndexReader.open(getBuilder().getFile());
			searcher = new IndexSearcher(reader);
		}
		
		return searcher;
	}
	
	private synchronized void clearSearcher() {
		if (searcher != null) {
			try {
				searcher.close();
				searcher = null;
				reader.close();
				reader = null;
			} catch (IOException e) {
			}
		}
	}
	
	public Analyzer createAnalyzer() {
		// FIXME: StopAnalyzer is quite crude; it doesn't do any stemming, for example
		return new StopAnalyzer();
	}
	
	
	protected abstract String getIndexName();
	protected abstract DocumentBuilder<T> getBuilder();
	protected abstract void doIndex(IndexWriter writer, List<Object> ids) throws IOException;
	protected abstract void doIndexAll(IndexWriter writer) throws IOException;
	
	private class IndexerThread extends Thread {
		public IndexerThread() {
			super("Indexer/" + getIndexName());
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					List<Object> ids = new ArrayList<Object>();
					boolean reindex = false;
					
					Object id = pending.take();
					while (id != null) {
						if (id instanceof ReindexMarker) {
							reindex = true;
							ids.clear();
						} else {
							ids.add(id);
						}
						id = pending.poll();
					}
					
					try {
						// FIXME: passing create=true to new IndexWriter creates a
						// new index replacing anything existing. We need to check
						// to make sure that this doesn't break searches that go
						// on concurrently. Searching on the old index or on the new
						// index would be fine, but crashes isn't.
						//
						if (reindex)
							logger.info(L"Reindexing {}", getIndexName());

						IndexWriter writer = new IndexWriter(getBuilder().getFile(), createAnalyzer(), reindex);
						
						if (reindex) {
							// Close the existing searcher which had the old index open; searches for a
							// while will return nothing useful ... it might be better to do this *after*
							// we finish the reindex, but I'm not sure it's OK to access the old index
							// after we've replaced
							clearSearcher();
							
							doIndexAll(writer);
						} else {
							doIndex(writer, ids);
						}
						writer.close();
						
						if (reindex)
							logger.info(L"Finished reindexing {}", getIndexName());
						
					} catch (IOException e) {
					}
				}
			} catch (InterruptedException e) {
				// We're done
			}
		}
	}
}

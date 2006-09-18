package com.dumbhippo.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.hibernate.lucene.DocumentBuilder;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.util.EJBUtil;

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

	public void index(Object id) {
		pending.add(id);
		ensureThread();
	}
	
	public void reindex() {
		pending.add(new ReindexMarker());
		ensureThread();
	}
	
	public void indexAfterTransaction(final List<Object> ids) {
		EJBUtil.defaultLookup(TransactionRunner.class).runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				index(ids);
			}
		});
	}
	
	public void indexAfterTransaction(Object id) {
		indexAfterTransaction(Collections.singletonList(id));
	}
	
	public synchronized Searcher getSearcher() throws IOException {
		if (searcher == null) {
			reader = IndexReader.open(getBuilder().getDirectoryProvider().getDirectory());
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
						if (reindex)
							logger.info("Reindexing {}", getIndexName());

						// It's not completely clear that passing 'create = true' here when
						// reindexing is safe when there is an existing IndexReader open,
						// but transient errors during reindexing aren't a big problem
						IndexWriter writer = new IndexWriter(getBuilder().getDirectoryProvider().getDirectory(), getBuilder().getAnalyzer(), reindex);
						
						if (reindex) {
							doIndexAll(writer);
						} else {
							doIndex(writer, ids);
						}
						writer.close();

						// If we have an IndexReader cached, close it so new searches
						// will see the post or posts we just indexed.
						clearSearcher();
						
						if (reindex)
							logger.info("Finished reindexing {}", getIndexName());
						
					} catch (IOException e) {
					}
				}
			} catch (InterruptedException e) {
				// We're done
			}
		}
	}
}

package com.dumbhippo.search;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.util.ContextHelper;
import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jms.JmsConnectionType;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.Application;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.applications.ApplicationSystem;

@Service
public class SearchSystemBean implements SearchSystem, SimpleServiceMBean {
	static private final Logger logger = GlobalSetup.getLogger(SearchSystemBean.class);

	// Number of entities to include in each transaction (and thus in each JMS message)
	// when reindexing.
	private static final int REINDEX_BATCH_SIZE = 100; 
	
	// How long to keep a reader open (in milleseconds). The reason for closing
	// readers periodically is that we are sharing the indexes over NFS and
	// the nodes that are just reading don't have any idea when the node
	// that is writing closes the index.
	private static final long READER_EXPIRATION_TIME = 20 * 1000;

	private JmsProducer queue;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	// Note that to avoid dependency loops, when these beans refer back to
	// SearchSystem to perform searches, they need @IgnoreDependency
	
	@EJB
	ApplicationSystem applicationSystem;

	@EJB
	GroupSystem groupSystem;
	
	@EJB
	MusicSystem musicSystem;
	
	@EJB
	PostingBoard postingBoard;;
	
	@EJB
	TransactionRunner runner;
	
	private Map<Class, Expirable<IndexReader>> readers = new HashMap<Class, Expirable<IndexReader>>();
	private Map<Class, Expirable<IndexSearcher>> searchers = new HashMap<Class, Expirable<IndexSearcher>>();
	
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
	
	private synchronized IndexReader getReader(final Class<?> clazz) {
		Expirable<IndexReader> reader = readers.get(clazz);
		if (reader == null) {
			reader = new Expirable<IndexReader> (READER_EXPIRATION_TIME) {
				@Override
				protected IndexReader create() {
					try {
						return IndexReader.open(getBuilder(clazz).getDirectoryProvider().getDirectory());
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
		}
		
		return reader.get();
	}

	private synchronized IndexSearcher getSearcher(final Class<?> clazz) {
		Expirable<IndexSearcher> searcher = searchers.get(clazz);

		if (searcher == null) {
			searcher = new Expirable<IndexSearcher> (READER_EXPIRATION_TIME) {
				@Override
				protected IndexSearcher create() {
					return new IndexSearcher(getReader(clazz));
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
		}

		return searcher.get();
	}
	
	private synchronized void clearSearcher(Class<?> clazz) {
		Expirable<IndexSearcher> searcher = searchers.get(clazz);
		if (searcher != null && searcher.getNoCreate() != null) {
			try {
				searcher.getNoCreate().close();
			} catch (IOException e) {
			}
			searcher.clear();
		}
		
		Expirable<IndexReader> reader = readers.get(clazz);
		if (reader != null && reader.getNoCreate() != null) {
			try {
				reader.getNoCreate().close();
			} catch (IOException e) {
			}
			reader.clear();
		}
	}

	public void start() throws Exception {
		// Items are inserted into the queue by Hibernate search from a synchronization,
		// so we don't want transactional logic in the queue as well
		queue = new JmsProducer(IndexerService.QUEUE_NAME, JmsConnectionType.NONTRANSACTED_IN_SERVER);
	}

	public void stop() throws Exception {
		queue.close();
		queue = null;
	}

	// Retrieve the HibernateSession for the EntityManager
	private Session getSession() {
		Object delegate = em;
		while (delegate != null && delegate instanceof EntityManager) {
			delegate = ((EntityManager)delegate).getDelegate();
		}
		if (!(delegate instanceof Session))
			throw new RuntimeException("Unable to find Hibernate sesion from EntityManager via getDelegate()");
		
		return (Session)delegate;
	}
	
	private SearchFactory getSearchFactory() {
		return ContextHelper.getSearchFactory(getSession());
	}
		
	private DocumentBuilder getBuilder(Class<?> clazz) {
		SearchFactory sf = ContextHelper.getSearchFactory(getSession());
		
		return sf.getDocumentBuilders().get(clazz);
	}
	
	private <KeyClass,EntityClass> void reindexBatch(Class<EntityClass> clazz, Iterator<KeyClass> keyIterator) {
		FullTextSession fullTextSession = Search.createFullTextSession(getSession());
		
		for (int i = 0; i < REINDEX_BATCH_SIZE && keyIterator.hasNext(); i++) {
			KeyClass key = keyIterator.next();
			EntityClass entity;
			if (key instanceof Guid)
				entity = em.find(clazz, ((Guid)key).toString());
			else
				entity = em.find(clazz, key);
			
			fullTextSession.index(entity);
		}
	}
	
	private <KeyClass,EntityClass> void reindexItems(final Class<EntityClass> clazz, List<KeyClass> items) {
		logger.info("Starting to queue items to reindex for {}", clazz.getSimpleName());
		
		// The first message sent over JMS triggers clearing the index. There's a 
		// race in that items added between the point we got the list of all items
		// to index and the point where the "clear" command is processed on the
		// indexing node will get discarded, but losing a few items from the
		// index isn't a huge deal in general.
		//
		// In an older version, we did the queries on the indexing node after 
		// clearing the index to avoid the race; changes in Hibernate's search
		// integration made that impractical, at least without getting even
		// deeper into the internal API then we already are.
		//
		queue.sendObjectMessage(clazz);
		
		// We don't want to create absolutely huge JMS messages with all the
		// documents, so we reindex chunk-by-chunk. This means that we create
		// instead of one absolutely huge JMS message, we create an enormous
		// number of quite large JMS messages. Once again, doing the query
		// for entities on the indexing node and building the Lucene Documents
		// there would be a lot better than doing it on the node where the
		// reindex is triggered.
		//
		final Iterator<KeyClass> iterator = items.iterator();
		while (iterator.hasNext()) {
			runner.runTaskInNewTransaction(new Runnable() {
				public void run() {
					reindexBatch(clazz, iterator);
				}
			});
		}
		
		logger.info("Finished queueing items to reindex for {}", clazz.getSimpleName());
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void reindexAll(String what) {
		List<String> types = null;
		if (what != null)
			types = Arrays.asList(what.trim().split("\\s*,\\s*"));
		if (what == null || types.contains("Group"))
			reindexItems(Group.class, groupSystem.getAllGroupIds());
		if (what == null || types.contains("Application"))
			reindexItems(Application.class, applicationSystem.getAllApplicationIds());
		if (what == null || types.contains("Post"))
			reindexItems(Post.class, postingBoard.getAllPostIds());
		if (what == null || types.contains("Track"))
			reindexItems(Track.class, musicSystem.getAllTrackIds()); 
	}

	public Hits search(Class<?> clazz, String[] fields, String queryString) throws IOException, ParseException {
		QueryParser queryParser = new MultiFieldQueryParser(fields, getBuilder(clazz).getAnalyzer());
		queryParser.setDefaultOperator(Operator.AND);
		org.apache.lucene.search.Query query;

		query = queryParser.parse(queryString);
		return getSearcher(clazz).search(query);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void queueIndexWork(List<LuceneWork> workQueue) {
		queue.sendObjectMessage((Serializable)workQueue);
	}
	
	public void doIndexWork(List<LuceneWork> queue) {
		if (queue.size() == 1)
			logger.debug("Got one item of class {} to index", queue.get(0).getClass().getSimpleName());
		else
			logger.debug("Got a batch of {} items to index", queue.size());
		
		Set<Class> classes = new HashSet<Class>();
		for (LuceneWork work : queue)
			classes.add(work.getClass());
		
		try {
			SearchFactory sf = getSearchFactory();

			// We happen to know that the LuceneBackendQueueProcessorFactory object is
			// empty, and just wraps the SearchFactory, so we don't bother to try
			// and cache it.
			BackendQueueProcessorFactory factory = new LuceneBackendQueueProcessorFactory();
			factory.initialize(null, sf);
			
			Runnable processor = factory.getProcessor(queue);
			processor.run();
		} finally {
			// Clear any IndexReaders we have cached, so that new searches will
			// will see the items we just indexed
			for (Class clazz : classes)
				clearSearcher(clazz);
		}

		logger.debug("Finished indexing items");
	}
	
	public void clearIndex(Class<?> clazz) {
		logger.info("Got a request to clear the index for class {}", clazz.getSimpleName());
		
		DocumentBuilder builder = getBuilder(clazz);
		
		IndexWriter writer;
		try {
			writer = new IndexWriter(builder.getDirectoryProvider().getDirectory(),
									 builder.getAnalyzer(),
									 true);
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Error when deleting index", e);
		} finally {
			clearSearcher(clazz);
		}
	}
}

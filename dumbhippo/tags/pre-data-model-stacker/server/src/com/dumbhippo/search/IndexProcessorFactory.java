package com.dumbhippo.search;

import java.util.List;
import java.util.Properties;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;

public class IndexProcessorFactory implements BackendQueueProcessorFactory {
	public Runnable getProcessor(List<LuceneWork> queue) {
		return new IndexProcessor(queue);
	}

	public void initialize(Properties properties, SearchFactory searchFactory) {
	}
}

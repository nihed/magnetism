package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.util.EJBUtil;

public class TrackIndexer extends Indexer<Track> {
	static TrackIndexer instance = new TrackIndexer();
	
	public static TrackIndexer getInstance() {
		return instance;
	}
	
	private TrackIndexer() {
		super(Track.class);
	}
	
	@Override
	protected String getIndexName() {
		return "Tracks";
	}

	@Override
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		EJBUtil.defaultLookup(MusicSystemInternal.class).indexTracks(writer, getBuilder(), ids);
	}
	
	@Override
	protected void doIndexAll(IndexWriter writer) throws IOException {
		EJBUtil.defaultLookup(MusicSystemInternal.class).indexAllTracks(writer, getBuilder());
	}
	
	@Override
	protected void doDelete(IndexReader reader, List<Object> ids) throws IOException {
		throw new UnsupportedOperationException("Tracks are unmodifiable, so reindexing them doesn't make sense");
	}	
}

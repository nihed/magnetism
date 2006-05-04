package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.persistence.Track;
import com.dumbhippo.server.util.EJBUtil;

public class TrackIndexer extends Indexer<Track> {
	DocumentBuilder<Track> builder;
	
	static TrackIndexer instance = new TrackIndexer();
	
	public static TrackIndexer getInstance() {
		return instance;
	}
	
	private TrackIndexer() {
		builder = new DocumentBuilder<Track>(Track.class);
	}
	
	protected String getIndexName() {
		return "Tracks";
	}
	
	protected DocumentBuilder<Track> getBuilder() {
		return builder; 
	}
	
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		EJBUtil.defaultLookup(MusicSystemInternal.class).indexTracks(writer, builder, ids);
	}
	
	protected void doIndexAll(IndexWriter writer) throws IOException {
		EJBUtil.defaultLookup(MusicSystemInternal.class).indexAllTracks(writer, builder);
	}
}

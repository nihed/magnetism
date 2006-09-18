package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;
import org.hibernate.lucene.store.FSDirectoryProvider;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.util.EJBUtil;

public class PostIndexer extends Indexer<Post> {
	private DocumentBuilder<Post> builder;
	
	static PostIndexer instance = new PostIndexer();
	
	public static PostIndexer getInstance() {
		return instance;
	}
	
	private PostIndexer() {
		FSDirectoryProvider directory = new FSDirectoryProvider();
		directory.initialize(Post.class, null, new Properties());
		builder = new DocumentBuilder<Post>(Post.class, createAnalyzer(), directory);
	}
	
	@Override
	protected String getIndexName() {
		return "Posts";
	}
	
	@Override
	protected DocumentBuilder<Post> getBuilder() {
		return builder; 
	}
	
	@Override
	protected void doIndex(IndexWriter writer, List<Object> ids) throws IOException {
		EJBUtil.defaultLookup(PostingBoard.class).indexPosts(writer, builder, ids);
	}
	
	@Override
	protected void doIndexAll(IndexWriter writer) throws IOException {
		EJBUtil.defaultLookup(PostingBoard.class).indexAllPosts(writer, builder);
	}
}

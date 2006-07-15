package com.dumbhippo.server;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.util.EJBUtil;

public class PostIndexer extends Indexer<Post> {
	private DocumentBuilder<Post> builder;
	
	static PostIndexer instance = new PostIndexer();
	
	public static PostIndexer getInstance() {
		return instance;
	}
	
	private PostIndexer() {
		builder = new DocumentBuilder<Post>(Post.class);
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

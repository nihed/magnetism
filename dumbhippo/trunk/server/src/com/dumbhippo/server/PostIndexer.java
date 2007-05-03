package com.dumbhippo.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;

public class PostIndexer extends UniqueObjectIndexer<Post> {
	static PostIndexer instance = new PostIndexer();
	
	public static PostIndexer getInstance() {
		return instance;
	}
	
	private PostIndexer() {
		super(Post.class);
	}
	
	@Override
	protected String getIndexName() {
		return "Posts";
	}

	// We never actually reindex posts currently, implement just for completeness
	@Override
	protected void doDelete(IndexReader reader, List<Object> ids) throws IOException {
		for (Object o : ids) {
			Guid guid = (Guid)o;
			Term term = new Term("id", guid.toString());
			reader.deleteDocuments(term);
		}
	}

	@Override
	protected List<Serializable> loadAllIds() {
		return TypeUtils.castList(Serializable.class, EJBUtil.defaultLookup(PostingBoard.class).getAllPostIds());
	}

	@Override
	protected Post loadObject(Serializable guid) {
		try {
			PostingBoard board = EJBUtil.defaultLookup(PostingBoard.class);
			Post post = board.loadRawPost(SystemViewpoint.getInstance(), (Guid) guid);
			// Filter group notifications from search results
			if (board.postIsGroupNotification(post))
				return null;
			return post;
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}	
}

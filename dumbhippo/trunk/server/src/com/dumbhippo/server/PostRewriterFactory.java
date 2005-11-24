package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Post;

@Local
public interface PostRewriterFactory {
	public void loadRewriter(Viewpoint viewpoint, Post post);
}

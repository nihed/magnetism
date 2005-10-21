package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;

@Local
public interface PostingBoard {
	
	public Set<Post> getPostedURLsFor(Person poster);
	
	public Post createURLPost(Person poster, String title, String text, String link, Set<String> recipientGuids);
	
	public Post createPost(Person poster, String title, String text, Set<Resource> resources, Set<Person> recipients);
}

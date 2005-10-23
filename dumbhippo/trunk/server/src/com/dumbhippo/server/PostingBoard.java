package com.dumbhippo.server;

import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Local
public interface PostingBoard {
	
	public List<Post> getPostsFor(Person poster, int max);
	
	public List<String> getPostedUrlsFor(Person poster, int max);
	
	public Post createURLPost(Person poster, String title, String text, String link, Set<String> recipientGuids) throws ParseException, GuidNotFoundException;
	
	public Post createPost(Person poster, String title, String text, Set<Resource> resources, Set<Person> recipients);
}

package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Rating;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface RecommenderSystem {

	public void addRatingForPostViewedBy(Post post, User viewer);
	
	public void addRatingForPostCreatedBy(Post post, User viewer);
	
	public List<Rating> getRatingsForUser(User user);
	
	public List<PostView> getRecommendedPosts(UserViewpoint viewpoint, int howMany);
	
}
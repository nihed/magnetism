package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Rating;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.RecommenderSystem;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * @author dff
 *
 * Display recommendations for a user, in a format similar to the home page.
 */

public class SuggestPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(SuggestPage.class);
	static private final int MAX_RECOMMENDED_POSTS_SHOWN = 10;
	
	@Signin
	private UserSigninBean signin;
	
	private PersonViewer personViewer;
	private PersonView person;
	private RecommenderSystem recommenderSystem;
	
	private ListBean<PostView> recommendedPosts;
	private ListBean<Rating> ratings;

	public SuggestPage() {
		personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);			
		recommenderSystem = WebEJBUtil.defaultLookup(RecommenderSystem.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = personViewer.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public ListBean<PostView> getRecommendedPosts() {
		if (recommendedPosts == null) {
			recommendedPosts = new ListBean<PostView>(recommenderSystem.getRecommendedPosts(signin.getViewpoint(), MAX_RECOMMENDED_POSTS_SHOWN));
		}
		return recommendedPosts;
	}
	
	public ListBean<Rating> getRatings() {
		if (ratings == null) {
			ratings = new ListBean<Rating>(recommenderSystem.getRatingsForUser(signin.getUser()));
		}
		return ratings;
	}
	
	public int getMaxRecommendedPostsShown() {
		return MAX_RECOMMENDED_POSTS_SHOWN;
	}

}

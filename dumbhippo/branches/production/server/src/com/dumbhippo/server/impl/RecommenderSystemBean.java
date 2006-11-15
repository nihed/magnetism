package com.dumbhippo.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Rating;
import com.dumbhippo.persistence.User;
import com.dumbhippo.recommender.HippoRecommender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RecommenderSystem;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.UserViewpoint;
import com.planetj.taste.common.TasteException;
import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.ItemFilter;
import com.planetj.taste.recommender.RecommendedItem;
import com.planetj.taste.recommender.Recommender;

/**
 * A stateless bean that implements a simple recommender system.
 * 
 * @author dff
 */

@Stateless
public class RecommenderSystemBean implements RecommenderSystem {

	static private final Logger logger = GlobalSetup.getLogger(RecommenderSystemBean.class);
	
	private static double VIEW_SCORE = 0.5;
	private static String VIEW_REASON = "Viewed resource";
	private static double CREATE_POST_SCORE = 1.0;
	private static String CREATE_POST_REASON = "Created resource";
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private PostingBoard postingBoard;	
	
	@EJB
	private TransactionRunner runner;
	
	private Recommender recommender;
	private DataSource dataSource;
	
	public RecommenderSystemBean() throws IOException, TasteException, NamingException {
		InitialContext initialContext = new InitialContext();
		dataSource = (DataSource)initialContext.lookup("java:/DumbHippoDS");
		recommender = new HippoRecommender(dataSource);
	}

	/**
	 * Creates a Rating at the specified level, but only if there was
	 * no previous rating.
	 * 
	 * @param user a User
	 * @param item a Post
	 * @return true if there was not previously a rating
	 */
	private boolean updateRatingData(final User user, final Post item, final double score, final String reason, final String type) {
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<Boolean>() {

				public Boolean call() {
					
					logger.debug("Update rating data for user={} item={}", user.getId(), item.getId());
					
					Rating rating;
					
					// TODO: maybe more efficient to use INSERT IGNORE; is that MySQL specific?
					try {
						rating = (Rating)em.createQuery("SELECT rating FROM Rating rating " +
							                            "WHERE rating.item = :item AND rating.user = :user")
				            .setParameter("item", item)
				            .setParameter("user", user)
				            .getSingleResult();
						return false;
					} catch (NoResultException e) {
						rating = new Rating(user, item, score, reason, type); 
						em.persist(rating);
						return true;
					}
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return false; // not reached
		}
	}

	
	/**
	 * Add a rating reflecting a clickthrough of a post by a user.
	 * @param post	  The post clicked through on
	 * @param viewer  The user who clicked through on it
	 */
	public void addRatingForPostViewedBy(Post post, User viewer) {
		if (updateRatingData(viewer, post, VIEW_SCORE, VIEW_REASON, Rating.POST_TYPE)) {
			//logger.debug("created new rating");
		} else {
			//logger.debug("left old rating");
		}
	}
	
	/**
	 * Add a rating reflecting the initial share of a post by a user.
	 * @param post    The post shared
	 * @param viewer  The user who shared it
	 */
	public void addRatingForPostCreatedBy(Post post, User viewer) { 
		if (updateRatingData(viewer, post, CREATE_POST_SCORE, CREATE_POST_REASON, Rating.POST_TYPE)) {
			//logger.debug("created new rating");
		} else {
			//logger.debug("left old rating");
		}
	}
	
	/**
	 * Get a list of the Ratings by a specific user.
	 * 
	 * @param user
	 * @return
	 */
	
	public List<Rating> getRatingsForUser(User user) {
		// TODO: add a timestamp to rating so we can sort this
		Query q = em.createQuery("SELECT rating FROM Rating rating " +
        						 "WHERE rating.user = :user");
		q.setParameter("user", user);

		@SuppressWarnings("unchecked")
		List<Rating> ratings = q.getResultList();
		return ratings;
	}
	
	/**
	 * Get a list of recommended posts for a user.
	 * 
	 * @param personId Person GUID
	 * @param howMany How many post objects to return.
	 * @return a List<PostView>, or null if there are no recommendatinos
	 */
	
	public List<PostView> getRecommendedPosts(UserViewpoint viewpoint, int howMany) {
		HippoAccessItemFilter viewpointAccessFilter = new HippoAccessItemFilter(viewpoint);
		
		List<PostView> recommendedPostViews = new ArrayList<PostView>();
		try {
			String personId = viewpoint.getViewer().getId();
			List<RecommendedItem> items = recommender.recommend(personId, howMany, viewpointAccessFilter);
			for (RecommendedItem item: items) {
				String postIdStr = (String)(item.getItem().getID());
				Guid postIdGuid = new Guid(postIdStr);
				try {
					recommendedPostViews.add(postingBoard.loadPost(viewpoint, postIdGuid));
				} catch (NotFoundException nfe) {
					logger.warn("NotFoundException in getRecommendedPosts for {}: {}", postIdGuid, nfe.getMessage());
					logger.warn("post not found trace", nfe);
				}
			}
			return recommendedPostViews;
		} catch (NoSuchElementException nsee) {
			// this is probably because there were no Ratings for the specified user; we should
			//  probably detect this further up the stack and throw a more specific exception
			//  so it can be reported to the user
			logger.warn("NoSuchElementException in getRecommendedPosts", nsee);
			return recommendedPostViews;
		} catch (TasteException te) {
			logger.warn("TasteException in getRecommendedPosts", te);
			return recommendedPostViews;
		} catch (Guid.ParseException gpe) {
			logger.warn("Guid.ParseException in getRecommendedPosts", gpe);
			return recommendedPostViews;
		}
	}
	
	/**
	 * Filter out items that the current user can't see.
	 */

	public class HippoAccessItemFilter implements ItemFilter {

		private UserViewpoint viewpoint;
		
		private HippoAccessItemFilter(UserViewpoint viewpoint) {
			this.viewpoint = viewpoint;
		}

		/**
		 * Check if the item can be seen by this Filter's viewpoint
		 * @param item  Item to check
		 * @return true if permission granted, false otherwise
		 */
		public boolean isAccepted(final Item item) {
			// TODO: make this always return false if a user has "banned" an item
				
			// do an access check for the particular post using the PostingBoard EJB
			try {
				String postId = (String)item.getID();				
				Post post = postingBoard.loadRawPost(viewpoint, new Guid(postId));
				return postingBoard.canViewPost(viewpoint, post);				
			} catch (NotFoundException nfe) {
				logger.warn("NotFoundException for {} in isAccepted; defaulting false: {}", item.getID(), nfe.getMessage());
				return false;
			} catch (Guid.ParseException gpe) {
				logger.error("GuidParseException for {} in isAccepted; defaulting false: {}", item.getID(), gpe.getMessage());
				return false;
			}
		}

	}
	
}
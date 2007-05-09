package com.dumbhippo.server.blocks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AmazonUpdater;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.services.AmazonItemView;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.caches.AmazonItemCache;
import com.dumbhippo.services.caches.AmazonListItemsCache;
import com.dumbhippo.services.caches.AmazonReviewsCache;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.WebServiceCache;

@Stateless
public class AmazonActivityBlockHandlerBean extends
		AbstractBlockHandlerBean<AmazonActivityBlockView> implements
		AmazonActivityBlockHandler {

	static private final Logger logger = GlobalSetup.getLogger(AmazonActivityBlockHandlerBean.class);	

	@EJB
	private AmazonUpdater amazonUpdater;	
	
	@WebServiceCache
	private AmazonReviewsCache reviewsCache;

	@WebServiceCache
	private AmazonListItemsCache listItemsCache;

	@WebServiceCache
	private AmazonItemCache itemCache;
	
	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}

	protected AmazonActivityBlockHandlerBean() {
		super(AmazonActivityBlockView.class);
	}	
	
	@Override
	protected void populateBlockViewImpl(AmazonActivityBlockView blockView)
			throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = getData1User(block);
		PersonView userView = personViewer.getPersonView(viewpoint, user);
		
		AmazonActivityStatus activityStatus = em.find(AmazonActivityStatus.class, block.getData2AsGuid().toString());
		
		AmazonReviewView reviewView = null;
		AmazonListItemView listItemView = null;
		switch (activityStatus.getActivityType()) {
	        case REVIEWED :
			    reviewView = reviewsCache.queryExisting(activityStatus.getAmazonUserId(), activityStatus.getItemId());		
			    break;
	        case WISH_LISTED :
			    listItemView = listItemsCache.queryExisting(new Pair<String, String>(activityStatus.getAmazonUserId(), activityStatus.getListId()), activityStatus.getItemId());		
			    break;
		}
		
		// we don't maintain all items updated in cache, so sometimes we will have to make a request to web services
		// at this point
		// we do get the item before we create a notification about it, so that should help
		// we can also create create an AmazonItemUpdater that periodically updates items for recent Amazon activity statuses
		AmazonItemView itemView = itemCache.getSync(activityStatus.getItemId());
		List<ChatMessageView> messageViews = chatSystem.viewMessages(chatSystem.getNewestMessages(block, BlockView.RECENT_MESSAGE_COUNT), viewpoint);
		
		int messageCount;
		if (messageViews.size() < BlockView.RECENT_MESSAGE_COUNT) // Optimize out a query
			messageCount = messageViews.size();
		else
			messageCount = chatSystem.getMessageCount(block);
		
		blockView.populate(userView, activityStatus, reviewView, listItemView, itemView, messageViews, messageCount);
	}

	public BlockKey getKey(User user, AmazonActivityStatus activityStatus) {
		switch (activityStatus.getActivityType()) {
		    case REVIEWED :
		        return new BlockKey(BlockType.AMAZON_REVIEW, user.getGuid(), activityStatus.getGuid());
		    case WISH_LISTED :
			    return new BlockKey(BlockType.AMAZON_WISH_LIST_ITEM, user.getGuid(), activityStatus.getGuid());
	    } 
		
		throw new RuntimeException("Unexpected AmazonActivityType for activity status " + activityStatus);
	}

	public Set<User> getInterestedUsers(Block block) {
		return super.getUsersWhoCareAboutData1UserAndExternalAccount(block, ExternalAccountType.AMAZON);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return super.getGroupsData1UserIsInIfExternalAccount(block, ExternalAccountType.AMAZON);
	}

	public void onAmazonActivityCreated(AmazonActivityStatus activityStatus) {
		logger.debug("new activity status to stack: " + activityStatus);
		long now = System.currentTimeMillis();
		Collection<User> users = amazonUpdater.getAccountLovers(activityStatus.getAmazonUserId());
		for (User user : users) {
			Block block = stacker.createBlock(getKey(user, activityStatus));
			stacker.stack(block, now, user, false, StackReason.NEW_BLOCK);
		}
	}

	public void onExternalAccountCreated(User user, ExternalAccount external) {
		// nothing to do, just wait for a photoset to appear in periodic job updater
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		if (external.getAccountType() != ExternalAccountType.AMAZON)
			return;
		if (external.getHandle() == null)
			return;
		Collection<AmazonActivityStatus> statuses = amazonUpdater.getActivityStatusesForAmazonAccount(external.getHandle());
		for (AmazonActivityStatus status : statuses) {
			stacker.refreshDeletedFlags(getKey(user, status));
		}
	}
}

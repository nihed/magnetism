package com.dumbhippo.server.blocks;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.NetflixQueueMoviesCache;
import com.dumbhippo.services.caches.WebServiceCache;

@Stateless
public class NetflixBlockHandlerBean extends AbstractBlockPerFeedEntryHandlerBean<NetflixBlockView> implements NetflixBlockHandler {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(NetflixBlockHandlerBean.class);
	
	// we don't want to include the netflix queue in blocks that are older than one week
	static private final long NETFLIX_QUEUE_INCLUSION_PERIOD = 1000 * 60 * 60 * 24 * 7;
	
	@WebServiceCache
	private NetflixQueueMoviesCache moviesCache;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@EJB
	private CacheFactory cacheFactory;
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	public NetflixBlockHandlerBean() {
		super(NetflixBlockView.class);
	}

	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.NETFLIX;
	}

	@Override
	protected BlockType getBlockType() {
		return BlockType.NETFLIX_MOVIE;
	}

	@Override
	public void onNewBlockCreated(User user, ExternalAccount external) {
	    moviesCache.getAsync(external.getHandle(), true);    	
	}
	
	@Override
	protected void populateBlockViewImpl(NetflixBlockView blockView) throws BlockNotVisibleException {
	    super.populateBlockViewImpl(blockView);
	    Block block = blockView.getBlock();
	    long now = System.currentTimeMillis();
	    if (block.getTimestampAsLong() + NETFLIX_QUEUE_INCLUSION_PERIOD > now) {
			User user = getData1User(block);
			try {
			    ExternalAccount netflixAccount = 
				    externalAccounts.lookupExternalAccount(blockView.getViewpoint(), user, ExternalAccountType.NETFLIX);	
				blockView.setQueuedMovies(moviesCache.getSync(netflixAccount.getHandle()));	
			} catch (NotFoundException e) {    
				throw new RuntimeException("Did not find a Netflix account for user " + user + 
						                   ", while populating a view for users' Netflix block", e);
			}	
	    }	
	}
}

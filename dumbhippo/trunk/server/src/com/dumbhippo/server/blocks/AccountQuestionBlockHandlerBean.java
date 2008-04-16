package com.dumbhippo.server.blocks;

import java.util.Collections;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Stateless
public class AccountQuestionBlockHandlerBean extends AbstractBlockHandlerBean<AccountQuestionBlockView>
		implements AccountQuestionBlockHandler {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AccountQuestionBlockHandlerBean.class);

	@EJB
	protected AccountSystem accountSystem;
	
	@EJB
	protected FacebookSystem facebookSystem;
	
	public BlockKey getKey(User user, AccountQuestion question) {
		return new BlockKey(BlockType.ACCOUNT_QUESTION, user.getGuid(), null, question.ordinal());
	}

	public AccountQuestionBlockHandlerBean() {
		super(AccountQuestionBlockView.class);		
	}
	
	private String getApplicationUsageAnswer(AccountQuestionBlockView blockView) {
		User user = getData1User(blockView.getBlock());
		Boolean applicationUsageEnabled = user.getAccount().isApplicationUsageEnabled();

		if (applicationUsageEnabled == null)
			return null;
		else if (applicationUsageEnabled)
			return "yes";
		else
			return "no";
	}

	private String getFacebookApplicationAnswer(AccountQuestionBlockView blockView) {
		User user = getData1User(blockView.getBlock());
		Boolean applicationEnabled = null;
		
		try {
		    applicationEnabled = 
			    facebookSystem.lookupFacebookAccount(blockView.getViewpoint(), user).isApplicationEnabled();
		} catch (NotFoundException e) {
			logger.warn("Did not find a FacebookAccount for user {} when checking their applicationEnabled status");
		}

		if (applicationEnabled == null)
			return null;
		else if (applicationEnabled)
			return "yes";
		else
			return "no";
	}
	
	@Override
	protected void populateBlockViewImpl(AccountQuestionBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		String answer = null;
		String linkParam = null;
		
		if (!(viewpoint instanceof SystemViewpoint || viewpoint.isOfUser(getData1User(blockView.getBlock()))))
			throw new BlockNotVisibleException("AccountQuestion block only visible to the user it was sent to");
		
		switch (blockView.getQuestion()) {
		case APPLICATION_USAGE:
			answer = getApplicationUsageAnswer(blockView);
			linkParam = "";
			break;
		case FACEBOOK_APPLICATION:
			answer = getFacebookApplicationAnswer(blockView);
			linkParam = facebookSystem.getApiKey();
			break;
	    }
		
		blockView.populate(answer, linkParam);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return Collections.emptySet();
	}

	public Set<User> getInterestedUsers(Block block) {
		return Collections.singleton(getData1User(block));
	}
	
	public void createApplicationUsageBlocks() throws RetryException {
		long when = System.currentTimeMillis();
		
		Query q = em.createQuery("SELECT a FROM Account a " +
								 " WHERE a.applicationUsageEnabled IS NULL " +
								 "   AND EXISTS (SELECT uci FROM UserClientInfo uci " +
								 "                WHERE uci.user = a.owner " +
								 "                  AND uci.platform = 'linux')");
		
		for (Account account : TypeUtils.castList(Account.class, q.getResultList())) {
			Block block = stacker.getOrCreateBlock(getKey(account.getOwner(), AccountQuestion.APPLICATION_USAGE));
			stacker.stack(block, when, null, false, StackReason.BLOCK_UPDATE);
		}
	}
	
	public void createFacebookApplicationBlocks() throws RetryException {
		long when = System.currentTimeMillis();
		for (FacebookAccount facebookAccount : facebookSystem.getAllAccounts()) {
			if (facebookAccount.isApplicationEnabled() == null) {
			    Block block = stacker.getOrCreateBlock(getKey(facebookAccount.getExternalAccount().getAccount().getOwner(), AccountQuestion.FACEBOOK_APPLICATION));
			    stacker.stack(block, when, null, false, StackReason.BLOCK_UPDATE);
			}
		}
	}

	public void onAccountAdminDisabledToggled(Account account) {
		// Don't care
	}

	public void onAccountDisabledToggled(Account account) {
		// Don't care
		
	}

	public void onApplicationUsageToggled(UserViewpoint viewpoint) {
		try {
			Block block = stacker.queryBlock(getKey(viewpoint.getViewer(), AccountQuestion.APPLICATION_USAGE));
			questionAnswered(viewpoint, block);
		} catch (NotFoundException e) {
			// No block, nothing to do
		}
	}
	
	public void onFacebookApplicationEnabled(UserViewpoint viewpoint) {
		try {
			Block block = stacker.queryBlock(getKey(viewpoint.getViewer(), AccountQuestion.FACEBOOK_APPLICATION));
			questionAnswered(viewpoint, block);
		} catch (NotFoundException e) {
			// No block, nothing to do
		}		
	}

	public void onMusicSharingToggled(UserViewpoint viewpoint) {
		// Don't care
	}

	private void handleApplicationUsageResponse(UserViewpoint viewpoint, String response) throws BadResponseCodeException {
		if (response.equals("yes"))
			identitySpider.setApplicationUsageEnabled(viewpoint, true);
		else if (response.equals("no"))
			identitySpider.setApplicationUsageEnabled(viewpoint, false);
		else
			throw new BadResponseCodeException("Bad response code in response to APPLICATION_USAGE question");
	}
	
	private void handleFacebookApplicationResponse(UserViewpoint viewpoint, String response) throws BadResponseCodeException {
		// we handle a positive response in FacebookTrackerBean when the person adds the application
		if (response.equals("no"))
			try {
				FacebookAccount facebookAccount = facebookSystem.lookupFacebookAccount(viewpoint, viewpoint.getViewer());
				// only set applicationEnabled to false based on this if it was not previously true
                if (facebookAccount.isApplicationEnabled() == null)
                	facebookAccount.setApplicationEnabled(false);
			} catch (NotFoundException e) {
				logger.warn("Did not find a FacebookAccount for user {} when trying to set their applicationEnabled status to false");
			}
		else
			throw new BadResponseCodeException("Bad response code in response to FACEBOOK_APPLICATION question");
	}
	
	public void handleResponse(UserViewpoint viewpoint, Guid blockId, String response) throws NotFoundException, BadResponseCodeException {
		UserBlockData ubd;
		AccountQuestionBlockView blockView;
		
		ubd = stacker.lookupUserBlockData(viewpoint, blockId);
		if (ubd.getBlock().getBlockType() != BlockType.ACCOUNT_QUESTION)
			throw new NotFoundException("Bad block type for Account question response");
		blockView = (AccountQuestionBlockView)stacker.loadBlock(viewpoint, ubd);

		if (!viewpoint.isOfUser(getData1User(ubd.getBlock()))) {
			// Paranoid check; UserBlockData should only exist for the owning user
			throw new NotFoundException("Attempt to answer someone else's AcountQuestion");
		}
		
		// We don't restack the block ourselves here; instead the question-type-specific
		// handler should make changes to the user's account resulting in a notification
		// to us. And then in response to the notification we restack the block. Doing
		// it this way makes sure that the question is still marked as answered even if
		// the user makes the change through the account page or other parts of the
		// user interface.
		
		switch (blockView.getQuestion()) {
		case APPLICATION_USAGE:
			handleApplicationUsageResponse(viewpoint, response);
			break;
		case FACEBOOK_APPLICATION:
			handleFacebookApplicationResponse(viewpoint, response);
		}
	}
	
	private void questionAnswered(UserViewpoint viewpoint, Block block) {
		long when = System.currentTimeMillis();
		
		try {
			UserBlockData ubd = stacker.lookupUserBlockData(viewpoint, block.getGuid());
			
			// We recycle "clicked" to mean "answered the question"; this is a little dubious
			// but will allow us to quickly query for unanswered question sent to a user
			// without having to extend Block.
			stacker.blockClicked(ubd, when);
		} catch (NotFoundException e) {
			logger.warn("UserBlockData didn't exist for the account's owner when answering an account question", e);
		}

		BlockDMOKey dmoKey = new BlockDMOKey(block); 
		DataService.currentSessionRW().changed(BlockDMO.class, dmoKey, "answer");
		DataService.currentSessionRW().changed(BlockDMO.class, dmoKey, "buttons");
		DataService.currentSessionRW().changed(BlockDMO.class, dmoKey, "description");
		
		// The call to blockClicked wont restack block since the clicked count 
		// can only ever be 1, so we need restack the block ourselves to change
		// it to a confirmation block
		stacker.stack(block, when, StackReason.BLOCK_UPDATE);
	}
}

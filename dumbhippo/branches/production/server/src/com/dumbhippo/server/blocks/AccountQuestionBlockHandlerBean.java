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
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class AccountQuestionBlockHandlerBean extends AbstractBlockHandlerBean<AccountQuestionBlockView>
		implements AccountQuestionBlockHandler {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AccountQuestionBlockHandlerBean.class);

	@EJB
	protected AccountSystem accountSystem;
	
	public BlockKey getKey(User user, AccountQuestion question) {
		return new BlockKey(BlockType.ACCOUNT_QUESTION, user.getGuid(), null, question.ordinal());
	}

	public AccountQuestionBlockHandlerBean() {
		super(AccountQuestionBlockView.class);		
	}
	
	private String getApplicationUsageAnswer(AccountQuestionBlockView blockView) {
		Viewpoint viewpoint = blockView.getViewpoint();
		Boolean applicationUsageEnabled = null;
		
		if (viewpoint instanceof UserViewpoint)
			applicationUsageEnabled = ((UserViewpoint)viewpoint).getViewer().getAccount().isApplicationUsageEnabled();

		if (applicationUsageEnabled == null)
			return null;
		else if (applicationUsageEnabled)
			return "yes";
		else
			return "no";
	}

	@Override
	protected void populateBlockViewImpl(AccountQuestionBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		String answer = null;
		
		if (!viewpoint.isOfUser(getData1User(blockView.getBlock())))
			throw new BlockNotVisibleException("AccountQuestion block only visible to the user it was sent to");
		
		switch (blockView.getQuestion()) {
		case APPLICATION_USAGE:
			answer = getApplicationUsageAnswer(blockView);
			break;
		}
		
		blockView.populate(answer);
	}

	public Set<Group> getInterestedGroups(Block block) {
		return Collections.emptySet();
	}

	public Set<User> getInterestedUsers(Block block) {
		return Collections.singleton(getData1User(block));
	}
	
	public void createApplicationUsageBlocks() {
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
	

	public void onAccountAdminDisabledToggled(Account account) {
		// Don't care
	}

	public void onAccountDisabledToggled(Account account) {
		// Don't care
		
	}

	public void onApplicationUsageToggled(Account account) {
		try {
			Block block = stacker.queryBlock(getKey(account.getOwner(), AccountQuestion.APPLICATION_USAGE));
			questionAnswered(new UserViewpoint(account.getOwner()), block);
		} catch (NotFoundException e) {
			// No block, nothing to do
		}
	}

	public void onMusicSharingToggled(Account account) {
		// Don't care
	}

	private void handleApplicationUsageResponse(UserViewpoint viewpoint, String response) throws BadResponseCodeException {
		if (response.equals("yes"))
			identitySpider.setApplicationUsageEnabled(viewpoint.getViewer(), true);
		else if (response.equals("no"))
			identitySpider.setApplicationUsageEnabled(viewpoint.getViewer(), false);
		else
			throw new BadResponseCodeException("Bad response code in response to APPLICATION_USAGE question");
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

		
		// The call to blockClicked wont restack block since the clicked count 
		// can only ever be 1, so we need restack the block ourselves to change
		// it to a confirmation block
		stacker.stack(block, when, StackReason.BLOCK_UPDATE);
	}
}

package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.listeners.AccountStatusListener;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

@Local
public interface AccountQuestionBlockHandler extends BlockHandler, AccountStatusListener {
	public BlockKey getKey(User user, AccountQuestion question);
	
	public void createApplicationUsageBlocks() throws RetryException;
	
	public void createFacebookApplicationBlocks() throws RetryException;
	
	public class BadResponseCodeException extends Exception {
		private static final long serialVersionUID = 1L;

		public BadResponseCodeException(String message) {
			super(message);
		}
	}
	
	public void handleResponse(UserViewpoint viewpoint, Guid blockId, String response) throws NotFoundException, BadResponseCodeException;
}

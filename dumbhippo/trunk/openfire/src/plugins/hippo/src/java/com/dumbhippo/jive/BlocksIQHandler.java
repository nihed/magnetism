package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;

import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.dm.DMSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.AccountQuestionBlockHandler;
import com.dumbhippo.server.blocks.AccountQuestionBlockHandler.BadResponseCodeException;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=BlocksIQHandler.BLOCKS_NAMESPACE)
public class BlocksIQHandler extends AnnotatedIQHandler {
	static final String BLOCKS_NAMESPACE = "http://mugshot.org/p/blocks"; 
	
	@EJB
	private AccountQuestionBlockHandler accountQuestionBlockHandler;
	
	@EJB
	private Stacker stacker;
	
	public BlocksIQHandler() {
		super("Hippo Blocks IQ Handler");
		Log.debug("creating BlocksIQHandler");
	}
	
	
	// See docs for Stacker.getOldBlocks() for details
	
	@IQMethod(name="getOldBlocks", type=IQ.Type.get)
	@IQParams({ "filter=null", "stackedBefore=-1", "desiredCount=20" })
	public List<BlockDMO> getOldBlocks(UserViewpoint viewpoint, String filter, long stackedBefore, int desiredCount) throws IQException {
		List<BlockDMO> results = new ArrayList<BlockDMO>();
		
		DMSession session = DataService.currentSessionRO();
		
		for (Block block : stacker.getOldBlocks(viewpoint, filter, stackedBefore, desiredCount)) {
			try {
				results.add(session.find(BlockDMO.class, new BlockDMOKey(block)));
			} catch (NotFoundException e) {
			}
		}
		
		return results;
	}
	
	@IQMethod(name="setStackFilter", type=IQ.Type.set)
	@IQParams({ "filter=null" })
	public void setStackFilter(UserViewpoint viewpoint, String filter) throws IQException {
		stacker.setUserStackFilterPrefs(viewpoint.getViewer(), filter);
	}

	@IQMethod(name="setBlockHushed", type=IQ.Type.set)
	@IQParams({ "blockId", "hushed" })
	public void setBlockHushed(UserViewpoint viewpoint, Guid blockId, boolean hushed) throws IQException {
        UserBlockData userBlockData;
        try {
			userBlockData = stacker.lookupUserBlockData(viewpoint, blockId);
		} catch (NotFoundException e) {
        	throw IQException.createBadRequest("blockId attribute doesn't refer to a recognized block for this user");
		}
        
        stacker.setBlockHushed(userBlockData, hushed);
	}
	
	@IQMethod(name="accountQuestionResponse", type=IQ.Type.set)
	@IQParams({ "blockId", "response" })
	public void accountQuestionResponse(UserViewpoint viewpoint, Guid blockId, String response) throws IQException {
        try {
			accountQuestionBlockHandler.handleResponse(viewpoint, blockId, response);
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("blockId attribute doesn't point to a question for this user");
		} catch (BadResponseCodeException e) {
			throw IQException.createBadRequest("response attribute had an unexpected value");
		}
	}
}

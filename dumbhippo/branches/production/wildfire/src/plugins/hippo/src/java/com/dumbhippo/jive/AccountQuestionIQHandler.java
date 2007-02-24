package com.dumbhippo.jive;


import javax.ejb.EJB;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.blocks.AccountQuestionBlockHandler;
import com.dumbhippo.server.blocks.AccountQuestionBlockHandler.BadResponseCodeException;
import com.dumbhippo.server.views.UserViewpoint;

@IQHandler(namespace=AccountQuestionIQHandler.ACCOUNT_QUESTION_NAMESPACE)
public class AccountQuestionIQHandler extends AnnotatedIQHandler {
	static final String ACCOUNT_QUESTION_NAMESPACE = "http://dumbhippo.com/protocol/accountQuestion"; 
	
	@EJB
	private AccountQuestionBlockHandler accountQuestionBlockHandler;

	public AccountQuestionIQHandler() {
		super("Hippo account question IQ Handler");
	}
	
	@IQMethod(name="response", type=IQ.Type.set)
	public void setResponse(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
        String blockId = child.attributeValue("blockId");
        if (blockId == null)
        	throw IQException.createBadRequest("missing blockId attribute");
        Guid blockGuid;
        try {
        	blockGuid = new Guid(blockId);
        } catch (ParseException e) {
        	throw IQException.createBadRequest("invalid blockId attribute");
        }
        
        String response = child.attributeValue("response");
        if (response == null)
        	throw IQException.createBadRequest("missing response attribute");
        
        try {
			accountQuestionBlockHandler.handleResponse(viewpoint, blockGuid, response);
		} catch (NotFoundException e) {
			throw IQException.createBadRequest("blockId attribute doesn't point to a question for this user");
		} catch (BadResponseCodeException e) {
			throw IQException.createBadRequest("response attribute had an expected value");
		}
	}
}

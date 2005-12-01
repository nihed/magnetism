package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.XMPPMethods;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

@Stateless
public class XMPPMethodsBean implements XMPPMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Log logger = GlobalSetup.getLog(XMPPMethodsBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	public void postClicked(Guid clickerId, String postId) throws GuidNotFoundException, ParseException {
		logger.debug("postClicked invoked: " + clickerId + " " + postId);
		User clicker = identitySpider.lookupGuid(User.class, clickerId);
		postingBoard.postViewedBy(postId, clicker);
	}
}

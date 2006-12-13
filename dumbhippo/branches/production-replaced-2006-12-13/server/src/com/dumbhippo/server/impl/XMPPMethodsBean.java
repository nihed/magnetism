package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.XMPPMethods;

/**
 * FIXME not in use anymore?
 *
 */
@Stateless
public class XMPPMethodsBean implements XMPPMethods, Serializable {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(XMPPMethodsBean.class);
	
	private static final long serialVersionUID = 0L;
	
	@SuppressWarnings("unused")
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	public void postClicked(Guid clickerId, String postId) throws NotFoundException, ParseException {
		logger.debug("postClicked invoked over xmpp, clicker {} post {}", clickerId, postId);
		User clicker = identitySpider.lookupGuid(User.class, clickerId);
		postingBoard.postViewedBy(postId, clicker);
	}
}

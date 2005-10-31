package com.dumbhippo.web;

import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonInfo;
import com.dumbhippo.server.PostInfo;
import com.dumbhippo.server.PostingBoard;

/**
 * @author otaylor
 *
 * Displays information for the logged in user, such as links recently
 * shared with him.
 */
public class WelcomePage {
	static private final Log logger = GlobalSetup.getLog(WelcomePage.class);

	@Signin
	private SigninBean signin;
	
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private PostingBoard postBoard;
	private PersonInfo personInfo;
	private GroupSystem groupSystem;
	private InvitationSystem invitationSystem;
	
	public WelcomePage() throws NamingException {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		groupSystem = WebEJBUtil.defaultLookup(GroupSystem.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), signin.getUser());
		
		return personInfo;
	}
	
	public List<PostInfo> getReceivedPostInfos() {
		logger.debug("Getting received posts for " + signin.getUser().getId());
		return postBoard.getReceivedPostInfos(signin.getUser(), 0);
	}
	
	public List<Group> getGroups() {
		return Group.sortedList(groupSystem.findGroups(signin.getUser()));
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public List<PersonInfo> getInviters() {
		return PersonInfo.sortedList(invitationSystem.findInviters(signin.getUser()));
	}
}

package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * @author otaylor
 *
 * Backing bean for the /download page.
 */
public class DownloadPage extends AbstractSigninOptionalPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadPage.class);
	
	private Configuration configuration;
	
	@Browser
	private BrowserBean browser;

	private InvitationSystem invitationSystem;
	
	private InvitationToken invitation;
	private PersonView inviter;

	public DownloadPage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);		
	}
	
	public boolean getHaveDownload() {
		return getDownloadUrl() != null;
	}
	
	public String getDownloadUrl() {
		if (browser.isFedora5Requested()) {
			return getDownloadUrlFedora5();
		} else if (browser.isWindowsRequested()) {
			return getDownloadUrlWindows();
		} else {
			return null;
		}
	}
	
	public String getDownloadFor() {
		if (browser.isFedora5Requested()) {
			return "Fedora Core 5";
		} else if (browser.isWindowsRequested()) {
			return "Windows";
		} else {
			return null;
		}
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	public String getDownloadUrlFedora5() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX);
	}
	
	// deprecated
	public String getDownloadUrlLinux() {
		logger.warn("Some page is still referring to downloadUrlLinux instead of distribution-specific urls");
		return getDownloadUrlFedora5();
	}
	
	public String getDownloadUrlLinuxTar() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX_TAR);
	}

	public String getDownloadUrlLinuxSrpm() {
		return configuration.getPropertyFatalIfUnset(HippoProperty.DOWNLOADURL_LINUX_SRPM);
	}
	
	public InvitationToken getInvitation() {
		return invitation;
	}
	
	public PersonView getInviter() {
		return inviter;
	}

	public boolean getReceivedTutorialShare() {
		if (!getSignin().isValid())
			return false;
		return getUserSignin().getUser().getAccount().getWasSentShareLinkTutorial();
	}
	
	public void setInviterId(String inviterId) {
		if (inviterId == null || inviterId.length() == 0)
			return;
		try {
			User inviterUser = identitySpider.lookupGuidString(User.class, inviterId);
			inviter = personViewer.getPersonView(getSignin().getViewpoint(), inviterUser);
		} catch (ParseException e) {
			logger.debug("Failed to parse inviter ID", e);			
			return;
		} catch (NotFoundException e) {
			logger.debug("Trying to set inviter ID to unknown user", e);
			return;
		} 
	}
	
	public void setInvitationId(String invitationIdStr) {
		if (invitationIdStr == null || invitationIdStr.length() == 0)
			return;
		if (!getSignin().isValid())
			return;
		long invitationId = Long.parseLong(invitationIdStr);
		UserViewpoint userView = getUserSignin().getViewpoint();
		invitation = invitationSystem.lookupInvitation(userView, invitationId);
	}	
}

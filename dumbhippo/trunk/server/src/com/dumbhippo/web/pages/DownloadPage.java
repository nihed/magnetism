package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.Download;
import com.dumbhippo.web.DownloadBean;

/**
 * @author otaylor
 *
 * Backing bean for the /download page.
 */
public class DownloadPage extends AbstractSigninOptionalPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(DownloadPage.class);
	
	@Download
	private DownloadBean download;
	
	private InvitationToken invitation;
	private PersonView inviter;

	public DownloadPage() {
	}
	
	public boolean getHaveDownload() {
		return getDownloadUrl() != null;
	}
	
	public String getDownloadUrl() {
		return download.getDownloadUrl();
	}
	
	// if linuxRequested && haveDownload then this should always return non-null
	public String getDownloadUrlSrpm() {
		return download.getDownloadUrlSrpm();
	}
	
	public String getDownloadFor() {
		return download.getDownloadFor();
	}
	
	public String getDownloadUrlWindows() {
		return download.getDownloadUrlWindows();
	}
	
	public String getDownloadUrlFedora5() {
		return download.getDownloadUrlFedora5();
	}
	
	public String getDownloadUrlFedora6() {
		return download.getDownloadUrlFedora6();
	}
	
	public String getDownloadUrlFedora5Srpm() {
		return download.getDownloadUrlFedora5Srpm();
	}
	
	public String getDownloadUrlFedora6Srpm() {
		return download.getDownloadUrlFedora6Srpm();
	}
	
	public String getDownloadUrlLinuxTar() {
		return download.getDownloadUrlLinuxTar();
	}
	
	// deprecated
	public String getDownloadUrlLinux() {
		return download.getDownloadUrlLinux();
	}
	
	// deprecated
	public String getDownloadUrlLinuxSrpm() {
		return download.getDownloadUrlLinuxSrpm();
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

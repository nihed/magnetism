package com.dumbhippo.web;

import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationView;
import com.dumbhippo.server.Viewpoint;

/**
 * InvitesPage corresponds to invites.jsp
 * 
 * @author marinaz
 * 
 */
public class InvitesPage extends AbstractInvitePage {
	
    private String invitationToDelete;
    private InvitationView invitationToDeleteView;

    private String invitationToRestore;
    
	public InvitesPage() {
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public String getInvitationToDelete() {
		return invitationToDelete;
	}

	public void setInvitationToDelete(String invitationToDelete) {
		this.invitationToDelete = invitationToDelete;
        if (invitationToDelete != null) { //&& (invitationToDeleteView == null)
        	invitationToDeleteView = 
		    	invitationSystem.deleteInvitation(new Viewpoint(signin.getUser()), 
		    			                          invitationToDelete);        	
	    } else {
	    	invitationToDeleteView = null;
	    }
	}
	
	public InvitationView getDeletedInvitation()
	{
		return invitationToDeleteView;
	}
	
	public String getInvitationToRestore() {
		return invitationToRestore;
	}

	public void setInvitationToRestore(String invitationToRestore) {
		this.invitationToRestore = invitationToRestore;
		invitationSystem.restoreInvitation(new Viewpoint(signin.getUser()), 
				                           invitationToRestore);
	}
	
}
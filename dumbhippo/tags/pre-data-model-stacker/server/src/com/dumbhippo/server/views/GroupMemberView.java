package com.dumbhippo.server.views;

public class GroupMemberView extends PersonView {

	private boolean viewerCanRemoveInvitation;
	// we don't currently need a GroupView here, but can add it in the future
	
	public GroupMemberView() {
		super();
	}
	
    public void setViewerCanRemoveInvitation(boolean viewerCanRemoveInvitation) {
        this.viewerCanRemoveInvitation = viewerCanRemoveInvitation;
	}

	public boolean getViewerCanRemoveInvitation() {
	    return viewerCanRemoveInvitation;
	}
}
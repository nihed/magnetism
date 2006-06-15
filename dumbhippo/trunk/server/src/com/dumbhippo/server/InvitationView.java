package com.dumbhippo.server;

import java.util.Set;

import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.InviterData;
import com.dumbhippo.persistence.Group;

/**
 * This is a class encapsulating information about an invitation as viewed by
 * a particular user that can be returned out of the session tier and used by 
 * web pages. 
 * 
 * @author marinaz
 */ 
public class InvitationView {
	InvitationToken invite;
	InviterData inviterData;
	Set<Group> suggestedGroups;
	
	/**
	 * Construct a new InvitationView object representing a view of a particular
	 * invitation by a particular person. The inviterData should be in the list
	 * of the inviters for the invite, but we do not want to be fishing it out
	 * in the jsp layer.
	 * 
	 * @param invite the invitation being viewd
	 * @param inviterData inviter data for the person viewing the invitation
	 */
	public InvitationView(InvitationToken invite, InviterData inviterData, 
			              Set<Group> suggestedGroups) {
		this.invite = invite;
		this.inviterData = inviterData;
		this.suggestedGroups = suggestedGroups;
	}
	
	public InvitationToken getInvite() {
		return invite;
	}
	
	public InviterData getInviterData() {
		return inviterData;
	}	
	
	public Set<Group> getSuggestedGroups() {
	    return suggestedGroups;	
	}
	
    public String getCommaSeparatedSuggestedGroupIds() {
    	StringBuilder groupIdsBuilder = new StringBuilder();
    	int i = 0;
    	
	    for (Group group : suggestedGroups) {
			if (i > 0) {
				groupIdsBuilder.append(",");
			}
			groupIdsBuilder.append(group.getId());
			i++;
	    }	
	    
		return groupIdsBuilder.toString();
    }

    public int getSuggestedGroupsCount() {
    	return suggestedGroups.size();
    }
}

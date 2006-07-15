package com.dumbhippo.hungry.performance;

import java.util.List;

import com.dumbhippo.hungry.util.SignedInPageTestCase;
import com.dumbhippo.hungry.util.SkipTest;
import com.dumbhippo.hungry.util.WebServices;

/**
 * "Test page" for adding contacts, groups, share to a user to use for performance 
 * data. It derives from the test page infrastructure mainly to reuse some code for
 * authenticating and signin in; it doesn't actually correspond to
 * a single page.
 * 
 * @author otaylor
 */
@SkipTest
public class ModifyUser extends SignedInPageTestCase {
	
	public ModifyUser(String userId) {
		super(userId);
	}
	
	public void addContact(String contactId) {
		WebServices ws = new WebServices(t);
		ws.doPOST("/addcontactperson", "contactId", contactId);
		System.out.println("Added " + contactId + " as contact to " + getUserId());
	}
	
	public void createGroup(String groupName, List<String> otherUsers) {
		WebServices ws = new WebServices(t);
		StringBuffer membersString = new StringBuffer();
		if (otherUsers != null) {
			for (String otherUser : otherUsers) {
				if (membersString.length() > 0)
					membersString.append(",");
				membersString.append(otherUser);
			}
		}
		ws.getXmlPOST("/creategroup", 
				      "name", groupName,
				      "members", membersString.toString(),
				      "secret", "false");
		System.out.println("Created group " + groupName);
	}
	
	public void joinGroup(String groupId) {
		WebServices ws = new WebServices(t);
		ws.doPOST("/joingroup", "groupId", groupId);
		System.out.println("Added " + getUserId() + " to group " + groupId);
	}
	
	public void shareLink(String url, String title, List<String> personRecipients, List<String> groupRecipients) {
		WebServices ws = new WebServices(t);
		
		StringBuffer commaRecipients = new StringBuffer();
		if (personRecipients != null) {
			for (String id : personRecipients) {
				if (commaRecipients.length() > 0)
					commaRecipients.append(",");
				commaRecipients.append(id);
			}
		}
		if (groupRecipients != null) {
			for (String id : groupRecipients) {
				if (commaRecipients.length() > 0)
					commaRecipients.append(",");
				commaRecipients.append(id);
			}
		}
		
		String description = "This is a share from " + getUserId() + " to " + commaRecipients + " with title " + title;
		
		ws.doPOST("/sharelink",
				  "url", url,
				  "title", title, 
				  "description", description,
				  "recipients", commaRecipients.toString(),
				  "secret", "false");		
		
		System.out.println("Shared: " + description);
	}

	@Override
	public void testPage() {
		//  Nothing
	}

	public void validatePage() {
		// Nothing
	}
}

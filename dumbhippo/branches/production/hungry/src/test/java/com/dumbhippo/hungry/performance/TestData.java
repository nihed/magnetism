package com.dumbhippo.hungry.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dumbhippo.hungry.util.CheatSheet;

/**
 * A utility class to create a standard set of data to use for measuring
 * performance. It creates a number of users, then adds contacts, 
 * groups and shares between them. The contacts and group memberships
 * are done using java.util.Random, but with a fixed seed so that the
 * "random" data comes up the same every time. (Changing any of the
 * constants below or adding other code that uses the random number
 * generator will, of course, result in everything being shuffled.)
 * 
 * @author otaylor
 */
public class TestData {
	static final int N_USERS = 20;            // Total number of users
	static final int CONTACTS_PER_USER = 8;   // Number of contacts to add per user
	static final int N_GROUPS = 8;            // Number of groups to create
	static final int GROUPS_PER_USER = 3;     // Number of groups to add each user to

	// Number of shares to make from each user to a group they are a member of 
	static final int GROUP_SHARES_PER_USER = 1;
	
    // Number of shares to make from each user to a set of their contacts
	static final int CONTACT_SHARES_PER_USER = 2;
	// Number of contacts for each of those shares
	static final int CONTACTS_PER_SHARE = 2;

	// Number of shares to make that go both to a group and to CONTACTS_PER_SHARE contacts
	static final int COMBINED_SHARES_PER_USER = 1;
	
	// Information about one of our users
	private static class User {
		private String name;
		private String email;
		private String userId;
		private int[] Groups;
		private int[] Contacts;
		
		String getEmail() {
			return email;
		}
		void setEmail(String email) {
			this.email = email;
		}
		String getName() {
			return name;
		}
		void setName(String name) {
			this.name = name;
		}
		String getUserId() {
			return userId;
		}
		void setUserId(String userId) {
			this.userId = userId;
		}
		int[] getGroups() {
			return Groups;
		}
		void setGroups(int[] groups) {
			Groups = groups;
		}
		int[] getContacts() {
			return Contacts;
		}
		void setContacts(int[] contacts) {
			Contacts = contacts;
		}		
	}
	
	// Information about one of our groups
	private static class Group {
		private String name;
		private String groupId;

		String getGroupId() {
			return groupId;
		}
		void setGroupId(String groupId) {
			this.groupId = groupId;
		}
		String getName() {
			return name;
		}
		void setName(String name) {
			this.name = name;
		}
	}
	
	CheatSheet cs;
	Random random;
	User[] users;
	Group[] groups;
	int totalShares;
	
	private TestData() {
		 cs = CheatSheet.getReadWrite();
		 random = new Random(0); // Fixed seed

		 users = new User[N_USERS];
		 groups = new Group[N_GROUPS];
	}
	
	/**
	 * Pick 'count' distinct elements out of the first n integers
	 * with equal probability to each.
	 * 
	 * @param n total number of elements to choose from
	 * @param count number of chosen elemen
	 * @return a new array with the returned elements
	 */
	private int[] shuffledInts(int n, int count) {
		int[] tmp = new int[n];
		int[] result = new int[count];
		
		for (int i = 0; i < n; i++) {
			tmp[i] = i;
		}
		
		for (int i = 0; i < count; i++) {
			int j = i + random.nextInt(n - i);
			result[i] = tmp[j];
			tmp[j] = tmp[i];
		}
		
		return result;
	}
	
	private void eraseData() {
    	System.out.println("Erasing database contents...");
    	cs.nukeDatabase();
    	System.out.println("Done.");		
	}
	
	private void createUsers() {
		for (int i = 0; i < N_USERS; i++) {
			User user = users[i] = new User();
			
			user.setEmail("hippo" + (i + 1) + "@example.com");
			user.setName("Hippo " + (i + 1));
			
			MakeUser makeUser = new MakeUser(user.getEmail(), user.getName());
			makeUser.setUp();
			makeUser.testPage();
			
			user.setUserId(makeUser.getUserId());			

			// Compute a list of groups for the user; we need
			// this data in advance so that we can invite all
			// of the users in a group when we create the group
			int[] groupIndices = shuffledInts(N_GROUPS, GROUPS_PER_USER);
			user.setGroups(groupIndices);
		}		
	}
	
	private void setupUser(int userIndex) {
		User user = users[userIndex];
		ModifyUser mu = new ModifyUser(user.getUserId());
		mu.setUp();
		
		// Choose CONTACTS_PER_USER other users and add them as contacts
		int[] userIndices = shuffledInts(N_USERS - 1, CONTACTS_PER_USER);
		for (int i = 0; i < CONTACTS_PER_USER; i++) {
			if (userIndices[i] >= userIndex)
				userIndices[i]++;
			mu.addContact(users[userIndices[i]].getUserId());
		}		
		user.setContacts(userIndices);
		
		// Add Groups to the user
		int[] groupIndices = user.getGroups();
		for (int i = 0; i < GROUPS_PER_USER; i++) {
			int index = groupIndices[i];
			
			if (groups[index] == null) {
				// First reference to a group, we need to create it
				
				Group group = groups[index] = new Group();
				group.setName("Hippo Group " + (index + 1));
				
				List<String> otherUsers = new ArrayList<String>();
				// Find other users that are part of the same group so
				// that we can initially invite them
				for (int j = userIndex + 1; j < N_USERS; j++) {
					User otherUser = users[j];
					int[] otherUserGroups = otherUser.getGroups();
					for (int k = 0; k < GROUPS_PER_USER; k++) {
						if (otherUserGroups[k] == index) {
							otherUsers.add(otherUser.getUserId());
						}
					}
				}
				
				mu.createGroup(group.getName(), otherUsers);
				
				// It would be a pain to parse the returned XML from the
				// HTTP method, so we just dig the group ID out of the database
				group.setGroupId(cs.getGroupId(group.getName()));
				
			} else {
				// This join accepts the invitation; accepting an 
				// invitation adds the inviter as a contact, so 
				// later users get more contacts than earlier users
				mu.joinGroup(groups[index].getGroupId());
			}
		}		
	}
	
	private void setupUsers() {
		for (int i = 0; i < N_USERS; i++)
			setupUser(i);
	}
	
	private void doShare(User user, ModifyUser mu, int nContacts, int nGroups) {
		totalShares++;
		String url = "http://devel.dumbhippo.com/wiki/SampleShare" + totalShares;
		String title = "Sample Share " + totalShares;
		List<String> personRecipients = new ArrayList<String>();
		List<String> groupRecipients = new ArrayList<String>();
		
		int contactIndices[] = user.getContacts();
		int contactShuffle[] = shuffledInts(CONTACTS_PER_USER, nContacts);
		for (int i = 0; i < nContacts; i++) {
			int contactIndex = contactIndices[contactShuffle[i]];
			personRecipients.add(users[contactIndex].getUserId()); 
		}
		
		int groupIndices[] = user.getGroups();
		int groupShuffle[] = shuffledInts(GROUPS_PER_USER, nGroups);
		for (int i = 0; i < nGroups; i++) {
			int groupIndex = groupIndices[groupShuffle[i]];
			groupRecipients.add(groups[groupIndex].getGroupId()); 
		}

		mu.shareLink(url, title, personRecipients, groupRecipients);
	}
	
	private void doUserShares(int userIndex) {
		User user = users[userIndex];
		
		ModifyUser mu = new ModifyUser(user.getUserId());
		mu.setUp();
		
		for (int i = 0; i < CONTACT_SHARES_PER_USER; i++)
			doShare(user, mu, CONTACTS_PER_SHARE, 0);
		
		for (int i = 0; i < GROUP_SHARES_PER_USER; i++)
			doShare(user, mu, 0, 1);
		
		for (int i = 0; i < COMBINED_SHARES_PER_USER; i++)
			doShare(user, mu, CONTACTS_PER_SHARE, 1);
	}
	
	private void doShares() {
		for (int i = 0; i < N_USERS; i++)
			doUserShares(i);		
	}

	public static void main(String[] args) {
    	TestData td = new TestData();
    	
    	td.eraseData();
    	td.createUsers();
    	td.setupUsers();
    	td.doShares();
	}
}

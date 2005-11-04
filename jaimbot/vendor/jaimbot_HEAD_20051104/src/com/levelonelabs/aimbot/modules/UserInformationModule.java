/*------------------------------------------------------------------------------
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is levelonelabs.com code.
 * The Initial Developer of the Original Code is Level One Labs. Portions
 * created by the Initial Developer are Copyright (C) 2001 the Initial
 * Developer. All Rights Reserved.
 *
 *         Contributor(s):
 *             Scott Oster      (ostersc@alum.rpi.edu)
 *             Steve Zingelwicz (sez@po.cwru.edu)
 *             William Gorman   (willgorman@hotmail.com)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this
 * file only under the terms of either the GPL or the LGPL, and not to allow
 * others to use your version of this file under the terms of the NPL, indicate
 * your decision by deleting the provisions above and replace them with the
 * notice and other provisions required by the GPL or the LGPL. If you do not
 * delete the provisions above, a recipient may use your version of this file
 * under the terms of any one of the NPL, the GPL or the LGPL.
 *----------------------------------------------------------------------------*/

package com.levelonelabs.aimbot.modules;

import java.util.*;

import com.levelonelabs.aim.AIMAdapter;
import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests for user queries
 *
 * @author Scott Oster
 *
 * @created September 6, 2002
 *
 * @todo instead of string, store HistoryObject (signon/off, date), then can easily compute on/off
 *       line durations
 * @todo attempt to remove signons caused by bot being offline and coming back
 * @todo when get to HistObjs attempt to maintain sanity of off/on/off/on cycle
 */
public class UserInformationModule extends BotModule {
	private static ArrayList services;
	private static final int SIZE = 6;

	/**
	 * Initialize the service commands.
	 */
	static {
		services = new ArrayList();
		services.add("history");
		services.add("users");
		services.add("enemies");
	}

	private Hashtable userHash = new Hashtable();


	/**
	 * Constructor for AdminModule.
	 * 
	 * @param bot
	 */
	public UserInformationModule(AIMBot bot) {
		super(bot);
		//register to here about signon events
		super.addAIMListener(new AIMAdapter() {
			public void handleBuddySignOn(AIMBuddy buddy, String info) {
				handleBuddyEvent(buddy, "Signed on: ");
			}


			public void handleBuddySignOff(AIMBuddy buddy, String info) {
				handleBuddyEvent(buddy, "Signed off: ");
			}
		});
	}


	void handleBuddyEvent(AIMBuddy buddy, String type) {
		ArrayList arr = (ArrayList) userHash.get(buddy.getName().toLowerCase());
		if (arr == null) {
			arr = new ArrayList();
		}
		arr.add(type + " " + new Date());
		while (arr.size() > SIZE) {
			arr.remove(0);
		}
		userHash.put(buddy.getName().toLowerCase(), arr);
	}


	/**
	 * @see com.levelonelabs.aimbot.BotModule#getName()
	 */
	public String getName() {
		return "User Information Module";
	}


	/**
	 * @see com.levelonelabs.aimbot.BotModule#getServices()
	 */
	public ArrayList getServices() {
		return services;
	}


	/**
	 * @see com.levelonelabs.aimbot.BotModule#help()
	 */
	public String help() {
		StringBuffer sb = new StringBuffer();
		sb.append("<B>history <i>USER</i></B> (displays user's recent sign on and off history)\n");
		sb.append("<B>users</B> (displays status of all users)\n");
		sb.append("A=" + AIMBot.ROLE_ADMINISTRATOR + ", M = messages pending, * = new user, E = Enemy, - = banned\n");
		sb.append("<B>enemies</B> (displays status of all enemies)\n");
		return sb.toString();
	}


	/**
	 * @see com.levelonelabs.aimbot.BotModule#performService(AIMBuddy, String)
	 */
	public void performService(AIMBuddy buddy, String query) {
		if (query.toLowerCase().startsWith("history")) {
			StringTokenizer st = new StringTokenizer(query.trim(), " ");

			//check for right number of arguments
			if (st.countTokens() < 2) {
				sendMessage(buddy, "ERROR:\n" + help());
				return;
			}

			//grab the command and target
			String imcommand = st.nextToken();
			if (!imcommand.toLowerCase().equals("history")) {
				sendMessage(buddy, "ERROR:\n" + help());
				return;
			}
			String imcommandTo = st.nextToken();
			AIMBuddy to = getBuddy(imcommandTo);

			//verify they are a user of the bot
			if (to == null) {
				sendMessage(buddy, "User " + imcommandTo
					+ " does not exist in the system.\nUse the ADDUSER command to add them.");
				return;
			}
			ArrayList hist = (ArrayList) userHash.get(to.getName().toLowerCase());
			if (hist == null) {
				sendMessage(buddy, "Sorry, I have no history for user: " + to.getName());
				return;
			} else {
				String result = "";
				for (Iterator iter = hist.iterator(); iter.hasNext();) {
					String element = (String) iter.next();
					result += (element + "\n");
				}
				sendMessage(buddy, result);
				return;
			}
		} else if (query.toLowerCase().trim().equals("users")) {
			String result = "";
			int num = 0;

			for (Iterator iter = getBuddyNames(); iter.hasNext();) {
				String name = (String) iter.next();
				AIMBuddy bud = getBuddy(name);
				if (bud == null) {
					continue;
				}
				num++;
				String markup = "";
				if (userHash.get(name.toLowerCase()) == null) {
					markup += "*";
				}
				if (bud.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
					markup += "A";
				}
				if (bud.hasRole(AIMBot.ROLE_ENEMY)) {
					markup += "E";
				}
				if (bud.isBanned()) {
					markup += "-";
				}
				if (bud.hasMessages()) {
					markup += "M";
				}
				result += (bud.getName() + "[" + (bud.isOnline() ? "<B>ON</B>" : "<I>OFF</I>") + "]" + markup + "    ");
			}
			if (result.trim().equals("")) {
				sendMessage(buddy, "Sorry, I have no information about active users.");
				return;
			} else {
				sendMessage(buddy, "Current (<b>" + num + "</b>) Users are:\n" + result);
				return;
			}
		} else if (query.toLowerCase().trim().equals("enemies")) {
			String result = "";
			int num = 0;

			for (Iterator iter = getBuddyNames(); iter.hasNext();) {
				String name = (String) iter.next();
				AIMBuddy bud = getBuddy(name);
				if (bud == null) {
					continue;
				}
				if (bud.hasRole(AIMBot.ROLE_ENEMY)) {
					result += (bud.getName() + "[" + (bud.isOnline() ? "<B>ON</B>" : "<I>OFF</I>") + "]    ");
					num++;
				}
			}
			if (result.trim().equals("")) {
				sendMessage(buddy, "I have no enemies.");
				return;
			} else {
				sendMessage(buddy, "Current (<b>" + num + "</b>) Enemies are:\n" + result);
				return;
			}
		}
	}
}
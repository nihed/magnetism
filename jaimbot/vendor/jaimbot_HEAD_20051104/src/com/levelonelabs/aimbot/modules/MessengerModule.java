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

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMAdapter;
import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * A class to handle on-offline messaging
 * 
 * @author Will Gorman, Scott Oster
 * 
 * @created January 28, 2002
 */
public class MessengerModule extends BotModule {
	private static ArrayList services;
	private static Logger logger = Logger.getLogger(MessengerModule.class.getName());

	/**
	 * Initialize the service commands.
	 */
	static {
		services = new ArrayList();
		services.add("tell");
		services.add("offline");
		services.add("clear");
		services.add("show");
	}


	/**
	 * Constructor for the MessengerModule object
	 * 
	 * @param bot
	 */
	public MessengerModule(AIMBot bot) {
		super(bot);
		//register to here about signon events
		super.addAIMListener(new AIMAdapter() {
			public void handleBuddySignOn(AIMBuddy buddy, String info) {
				retrieveMessages(buddy);
			}
		});
	}


	/**
	 * Gets the name attribute of the MessengerModule object
	 * 
	 * @return The name value
	 */
	public String getName() {
		return "Messenger Module";
	}


	/**
	 * Gets the services attribute of the MessengerModule object
	 * 
	 * @return The services value
	 */
	public ArrayList getServices() {
		return services;
	}


	/**
	 * Describes the usage of the module
	 * 
	 * @return the usage of the module
	 */
	public String help() {
		StringBuffer sb = new StringBuffer();
		sb.append("<B>tell <i>BUDDY MESSAGE</i></B> (sends a message to a person in ");
		sb.append("the system, if they are offline they will recieve the message ");
		sb.append("next time they log in)\n");
		sb.append("<B>offline <i>BUDDY MESSAGE</i></B> (will notify the person the next time they log in)\n");
		sb.append("<B>clear messages</B> (will erase messages in system for user)\n");
		sb.append("<B>show messages</B> (will display all the messages left for a user)\n");
		sb.append("* If the preference \"savemessages\" is set to true, need to manually \"clear messages\".\n");

		return sb.toString();
	}


	/**
	 * Called when someone leaves a message for someone else to add the message.
	 * 
	 * @param to
	 *            target buddy
	 * @param from
	 *            sending buddy
	 * @param message
	 *            message sent
	 */
	public void addMessage(AIMBuddy to, AIMBuddy from, String message) {
		String fromName = "Someone";

		if (to == null) {
			return;
		}

		if (from != null) {
			fromName = from.getName();
		}

		to.addMessage(fromName + " said \"" + message + "\" at " + new Date());
	}


	/**
	 * Grabs and sends any stored messages for a buddy when they sign on
	 * 
	 * @param buddy
	 * @return whether any messages where found
	 */
	public boolean retrieveMessages(AIMBuddy buddy) {
		//check for messages
		if (buddy.hasMessages()) {
			ArrayList messages = buddy.getMessages();
			String message = "";

			//collect the messages
			for (int i = 0; i < messages.size(); i++) {
				message += (messages.get(i) + "<BR>");
			}

			//send the list
			super.sendMessage(buddy, message);
			//check if they should be saved
			String savePref = buddy.getPreference("savemessages");
			if ((savePref == null) || savePref.equals("false")) {
				buddy.clearMessages();
			}
			return true;
		}
		return false;
	}


	/**
	 * Handle a messaging query
	 * 
	 * @param buddy
	 * @param query
	 */
	public void performService(AIMBuddy buddy, String query) {
		if (query.toLowerCase().startsWith("tell")) {
			handleTell(buddy, query);
		} else if (query.toLowerCase().startsWith("offline")) {
			handleOffline(buddy, query);
		} else if (query.toLowerCase().startsWith("clear messages")) {
			logger.info("Clearing Messages for user:" + buddy + ".");
			buddy.clearMessages();
			super.sendMessage(buddy, "Messages Cleared");
		} else if (query.toLowerCase().startsWith("show messages")) {
			if (!retrieveMessages(buddy)) {
				super.sendMessage(buddy, "No Messages");
			}
		}
	}


	/**
	 * Tell funcionality
	 * 
	 * @param buddy
	 * @param query
	 */
	private void handleTell(AIMBuddy buddy, String query) {
		String name = "Someone";

		//sendMessage(getBuddy("osterCRD"), "YEP, POOP");
		if (query.toLowerCase().startsWith("tell")) {
			StringTokenizer st = new StringTokenizer(query, " ");

			//check for right number of arguments
			if (st.countTokens() < 3) {
				sendMessage(buddy, "ERROR:\n" + help());
				return;
			}

			//grab the command and target
			String imcommand = st.nextToken();
			if (!imcommand.toLowerCase().equals("tell")) {
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

			//grab the rest of the message and send it to the target
			String imcommandText = "";
			while (st.hasMoreTokens()) {
				imcommandText = imcommandText + " " + st.nextToken();
			}

			//only send a message if there is somethign there.
			if (!imcommandText.equals("")) {
				//if the target if not online we need to store the message for
				// when they sign on
				if (to.isOnline()) {
					if (buddy != null) {
						name = buddy.getName();
					}

					sendMessage(to, name + " said: " + imcommandText);
				} else {
					sendMessage(buddy, imcommandTo + " is offline and will be told when they signon.");
					addMessage(to, buddy, imcommandText);
				}
			}
		}
	}


	/**
	 * Offline functionality
	 * 
	 * @param buddy
	 * @param query
	 */
	private void handleOffline(AIMBuddy buddy, String query) {
		//handle offline request
		if (query.toLowerCase().startsWith("offline")) {
			StringTokenizer st = new StringTokenizer(query, " ");

			//check for proper params
			if (st.countTokens() < 3) {
				super.sendMessage(buddy, "ERROR:\n" + help());
				return;
			}

			//grab the target
			String imcommand = st.nextToken();
			String imcommandTo = st.nextToken();
			AIMBuddy to = super.getBuddy(imcommandTo);
			if (to == null) {
				super.sendMessage(buddy, "User " + imcommandTo
					+ " does not exist in the system.  Use the ADDUSER command to add them.");
				return;
			}

			//grab the message
			String imcommandText = "";
			while (st.hasMoreTokens()) {
				imcommandText = imcommandText + " " + st.nextToken();
			}
			if (!imcommandText.equals("")) {
				sendMessage(buddy, imcommandTo + " will be told next time they signon.");
				addMessage(to, buddy, imcommandText);
			}
		}
	}
}
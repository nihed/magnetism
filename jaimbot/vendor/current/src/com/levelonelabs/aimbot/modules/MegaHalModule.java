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

import megahal.MegaDictionary;
import megahal.MegaHalInterface;
import megahal.MegaModel;

import java.io.File;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Creates Babel poetry from user input
 *
 * @author Will Gorman
 *
 * @created February 3, 2002
 */
public class MegaHalModule extends BotModule implements MegaHalInterface {
	private static ArrayList services;

	/**
	 * Initialize the service commands.
	 */
	static {
		//init the services
		services = new ArrayList();
		services.add("megahaladmin");
	}

	/** tells MegaModel which order (how many contexts) it should be. */
	protected int order = 5;
	/** used for specifing which brain directory to use */
	protected String directory = System.getProperty("user.dir") + File.separator + "conf" + File.separator + "megahal";
	/** remembers previous brain directory */
	protected String last = null;
	MegaModel model = null;


	/**
	 * Constructor for the BabelModule object
	 * 
	 * @param bot
	 */
	public MegaHalModule(AIMBot bot) {
		super(bot);
		model = new MegaModel(this); // the initialize function really is the
		// constructor...
		//set default to 3 seconds instead of 5
		MegaModel.timeout = 3;
		model.load_personality(order, directory, "");
		// INIT MEGAHAL
	}


	/**
	 * warn is used for errors that occur.
	 * 
	 * @param title
	 *            error title.
	 * @param error_msg
	 *            error message.
	 * @return boolean whether printing of error was successful.
	 */
	public boolean warn(String title, String error_msg) {
		return true;
	}


	/**
	 * allow for progress tracking of certain activities in MegaModel.
	 * 
	 * @param message
	 *            message to display
	 * @param done
	 *            amount completed.
	 * @param total
	 *            total amount to complete.
	 * @return whether printing of progress was complete or not.
	 */
	public boolean progress(String message, int done, int total) {
		return true;
	}


	/**
	 * allows for printing of debug information.
	 * 
	 * @param str
	 *            debug string
	 */
	public void debug(String str) {
	}


	/**
	 * Gets the services attribute of the BabelModule object
	 * 
	 * @return The services value
	 */
	public ArrayList getServices() {
		return services;
	}


	/**
	 * Gets the name attribute of the BabelModule object
	 * 
	 * @return The name value
	 */
	public String getName() {
		return "MegaHal Module";
	}


	/**
	 * Describes the usage of the module
	 * 
	 * @return the usage of the module
	 */
	public String help() {
		StringBuffer sb = new StringBuffer();
		sb.append("<B>MEGAHAL</B> commands:\n");
		sb.append("<b>megahaladmin save</b> (saves the megahal brain *ADMIN ONLY*)\n");
		sb.append("<b>megahaladmin think <i>N</i></b>  (sets the think time to <N> seconds *ADMIN ONLY*)\n");
		return sb.toString();
	}


	/**
	 * Create poetry
	 * 
	 * @param buddy
	 * @param query
	 */
	public void performService(AIMBuddy buddy, String query) {
		if (query.toLowerCase().startsWith("megahaladmin")) {
			if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
				super.sendMessage(buddy, "Sorry, you are not an " + AIMBot.ROLE_ADMINISTRATOR);
				return;
			}
			StringTokenizer st = new StringTokenizer(query, " ");

			//check for enough args
			if (st.countTokens() < 2) {
				super.sendMessage(buddy, "ERROR:\n" + help());
				return;
			}
			st.nextToken();
			String imcommand = st.nextToken();
			if (imcommand.toLowerCase().equals("save")) {
				synchronized (model) {
					model.save_model(directory + File.separator + MegaModel.MEGA_BRAIN);
				}
				super.sendMessage(buddy, "Saved Model");
				return;
			} else if (imcommand.toLowerCase().equals("think") && (st.countTokens() == 1)) {
				int delay = MegaModel.timeout;
				String newDelay = st.nextToken();
				try {
					delay = Integer.parseInt(newDelay);
				} catch (Exception e) {
					super.sendMessage(buddy, "Error! " + newDelay + " did not parse.");
				}
				synchronized (model) {
					MegaModel.timeout = delay;
				}
				super.sendMessage(buddy, "Changing thinking time to " + delay + " seconds.");
				return;
			} else {
				super.sendMessage(buddy, "I don't know about " + imcommand);
			}

		} else {
			if (query.toLowerCase().startsWith("you should talk to")) {
				AIMBuddy to = getBuddy(query.substring(18).trim());
				if (to != null) {
					if (to.getName().toLowerCase().equals(super.bot.getUsername().toLowerCase())) {
						super.sendMessage(buddy, "Sorry, I can't talk to myself");
					} else {
						if (!to.isOnline()) {
							to.addMessage(buddy.getName() + " said I should talk to you!");
							super.sendMessage(buddy, "I'll talk to them once they're online.");
						} else {
							super.sendMessage(to, buddy.getName() + " said I should talk to you!");
							super.sendMessage(buddy, "I've started a conversation with " + to.getName());
						}
					}
				} else {
					super.sendMessage(buddy, "I don't know a " + query.substring(18).trim());
				}
				return;
			}
			MegaDictionary words = new MegaDictionary();
			words.make_words(query.toUpperCase());
			String output = "";
			synchronized (model) {
				model.learn(words);
				output = MegaModel.capitalize(model.generate_reply(words));
			}

			super.sendMessage(buddy, output);
		}
	}
}
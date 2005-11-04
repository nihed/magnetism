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

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Properties;

import java.util.logging.Logger;

import com.levelonelabs.aim.AIMBuddy;

import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests to control X10 devices using Heyu and CM11A. Heyu runs on
 * Linux/Unix and is written by Daniel B. Suthers. Heyu is available for free
 * at http://heyu.tanj.com. Daniel requests that NO mirrors of his Heyu source
 * code be created.
 * 
 * HeyuModule officially supports Heyu version 1.35. HeyuModule REQUIRES a
 * WORKING INSTALLATION of Heyu on the same physical machine (i.e. you must be
 * running the jaimbot on Linux/Unix with a CM11A attached and Heyu installed,
 * running and working).
 * 
 * Please see the Heyu FAQ at http://heyu.tanj.com for details about X10 and
 * the CM11A. The CM11A is available from http://www.x10.com and other sources
 * for under $50. Bundled deals that include X10 modules for the CM11A to
 * control can often be purchased at that price (a substantial discount).
 * Please note: The author is NOT trying to generate sales for X10, just simply
 * stating how you can get started using this module.
 * 
 * CM11A and X10 may be Registered Trademarks of X10 Corp. (http://www.x10.com)
 * 
 * The author of this software is not affiliated with X10 Corp.
 * 
 * @author David Nelson (david@david-nelson.com)
 * 
 * @created November 22, 2003
 */
public class HeyuModule extends BotModule {
	private static ArrayList services;
	private static Logger logger = Logger.getLogger(HeyuModule.class.getName());
	private static String requiredRole = AIMBot.ROLE_ADMINISTRATOR;
	private static String heyuPath = "/usr/local/bin/heyu";
	private static String successMessage = "";
	private String modulesText = "";
	private boolean allowHelp = false;

	/**
	 * Initialize the service commands.
	 */
	static {
		services = new ArrayList();
		services.add("turn");
		services.add("heyu");
	}


	public ArrayList getServices() {
		return services;
	}


	public String help() {
		String helpText = "";
		StringBuffer sb = new StringBuffer();

		if (allowHelp) {
			sb.append("<B>turn</B> (executes Heyu commands)\n");
			sb.append("\n<b>EXAMPLE: turn office on</b>\n");
			sb.append("<B>heyu</B> (lists admin supplied module text)\n");
			helpText = sb.toString();
		}
		return helpText;
	}


	public String getModules() {
		return modulesText;
	}


	public void performService(AIMBuddy buddy, String query) {
		if (buddy.hasRole(requiredRole)) {
			if (query.toLowerCase().startsWith("turn")) {
				try {
					Runtime.getRuntime().exec(heyuPath + " " + query.toLowerCase());
					logger.finest(buddy.getName() + " performed: " + query.toLowerCase());
					if (successMessage != null && successMessage.trim().length() > 0) {
						super.sendMessage(buddy, successMessage);
					}
				} catch (IOException ioe) {
					super.sendMessage(buddy, "Error processing command: " + ioe.getMessage());
					logger.severe("Error processing command: " + ioe.getMessage());
				}
			} else if (query.toLowerCase().startsWith("heyu")) {
				super.sendMessage(buddy, getModules());
			}
		}
	}


	public String getName() {
		return "Heyu Module";
	}


	public HeyuModule(AIMBot bot) {
		super(bot);

		Properties props = new Properties();
		try {
			InputStream is = ClassLoader.getSystemResourceAsStream("HeyuModule.properties");
			props.load(is);
			requiredRole = props.getProperty("requiredRole", requiredRole);
			heyuPath = props.getProperty("heyuPath", heyuPath);
			successMessage = props.getProperty("successMessage", successMessage);
			String helpAllowed = props.getProperty("allowHelp");
			if (helpAllowed != null) {
				helpAllowed = helpAllowed.trim().toLowerCase();
			} else {
				helpAllowed = "false";
			}
			allowHelp = (helpAllowed.equals("true"));
			int i = 0;
			String module = props.getProperty("modules" + i);
			while (module != null) {
				modulesText = modulesText + module + "\n";
				i++;
				module = props.getProperty("modules" + i);
			}
		} catch (Exception e) {
			logger.info("Error Reading HeyuModule.properties: " + e.getMessage());
		}
	}
}
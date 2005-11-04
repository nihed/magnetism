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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Creates a telnet port to communicate with JaimBot. 
 * 
 * By default, opens port 1234. Allows telnet clients connect to it and send 
 * Jaimbot commands to it. Operations like tell and post will work normally.
 * However, the Jaimbot command chain does not currently return a module's 
 * output, therefore no output will be produced to the telnet session.  
 *
 * @author Steve Zingelwicz
 *
 * @created May 28, 2002
 */
public class TelnetModule extends BotModule {
	//its unfortunate if somebody actually has this name, but I'm not too
	// concerned about it
	private static final String TELNET_BUDDY_NAME = "TelnetMod";
	private static ArrayList services;
	static Logger logger = Logger.getLogger(TelnetModule.class.getName());

	/**
	 * Initialize the service commands.
	 */
	static {
		services = new ArrayList();
	}

	private TelnetServer telnetSvr;


	/**
	 * Constructor for the ListModule object
	 * 
	 * @param bot
	 */
	public TelnetModule(AIMBot bot) {
		super(bot);
		//logger.fine("Starting TelnetModule");
		telnetSvr = new TelnetServer(bot);
		telnetSvr.start();
		//logger.fine("Telnet Server started");
	}


	/**
	 * Gets the services attribute of the ListModule object
	 * 
	 * @return The services value
	 */
	public ArrayList getServices() {
		return services;
	}


	/**
	 * Gets the name attribute of the ListModule object
	 * 
	 * @return The name value
	 */
	public String getName() {
		return "Telnet Module";
	}


	/**
	 * This will be called when the AIMBot gets a string starting with a keyword
	 * that this module advertised as a service.
	 * 
	 * @param buddy
	 * @param query
	 */
	public void performService(AIMBuddy buddy, String query) {
	}


	/**
	 * Describes the usage of the module
	 * 
	 * @return the usage of the module
	 */
	public String help() {
		StringBuffer sb = new StringBuffer();
		sb.append("The Telnet module does not support commands through this interface.\n");
		return sb.toString();
	}


	/**
	 * Returns or creates and returns the mods buddy
	 * 
	 * @return the mods buddy
	 */
	private AIMBuddy getModBuddy() {
		AIMBuddy modBuddy = super.getBuddy(TELNET_BUDDY_NAME);
		if (modBuddy == null) {
			modBuddy = new AIMBuddy(TELNET_BUDDY_NAME);
			super.addBuddy(modBuddy);
		}
		return modBuddy;
	}


	public class TelnetServer extends Thread {
		public ServerSocket svrSock;
		public AIMBot parentBot;
		public boolean bRunSvr = false;


		public TelnetServer(AIMBot bot) {
			try {
				svrSock = new ServerSocket(1234);
				//logger.info("Created server socket on 1234");
				parentBot = bot;
				bRunSvr = true;
			} catch (Exception e) {
				logger.severe("Could not create server socket in Telent Module");
				bRunSvr = false;
			}
		}


		public void run() {
			Socket sock;
			TelnetHandler th;

			//logger.fine("Executing server thread");
			try {
				while (bRunSvr == true) {
					sock = svrSock.accept();
					logger.info("Accepted connection ");
					th = new TelnetHandler(sock);
					th.start();
					logger.fine("Spun off handler thread");
				}
			} catch (Exception e) {
				logger.severe("Error accepting connection in Telnet Module");
			}
		}


		public class TelnetHandler extends Thread {
			Socket sock;
			InputStream is;
			OutputStream os;
			InputStreamReader isr;
			OutputStreamWriter osw;
			boolean bRunState = false;


			public TelnetHandler(Socket newSock) {
				sock = newSock;
				//logger.fine("Constructing Telnet Handler");
				try {
					is = sock.getInputStream();
					isr = new InputStreamReader(is);

					os = sock.getOutputStream();
					osw = new OutputStreamWriter(os);

					bRunState = true;
					//logger.fine("Completed constructing Telnet Handler");
				} catch (Exception e) {
					logger.severe("Unable to start Telnet Handler");
				}
			}


			public void run() {
				StringBuffer cmdBuf = new StringBuffer();
				String cmd;
				int ch;

				if (bRunState == false) {
					return;
				}

				//logger.fine("Running Telnet Handler");
				try {
					osw.write("Welcome to Jaimbot's Telnet Module.  Type quit to quit.\n");
					osw.flush();
					do {
						while ((ch = isr.read()) != '\n') {
							cmdBuf.append((char) ch); // append the string
							osw.write((char) ch);
							// echo the character to the telnet client
							osw.flush();
						}
						osw.write('\n');
						osw.flush();

						cmd = cmdBuf.toString();
						cmdBuf = new StringBuffer();
						cmd = cmd.trim();

						logger.fine("Received command: ");
						logger.fine("-" + cmd + "-");

						if (!cmd.equals("quit")) {
							logger.fine("Executing bot command...");
							parentBot.handleMessage(getModBuddy(), cmd);
							logger.fine("Ok");
						}
					} while (!cmd.equals("quit"));

					// Attempt to clean up after ourselves
					isr.close();
					osw.close();
					sock.close();
				} catch (Exception e) {
					e.printStackTrace();
					logger.severe("Trouble in TelnetHandler");
				}
			}
		}
	}
}
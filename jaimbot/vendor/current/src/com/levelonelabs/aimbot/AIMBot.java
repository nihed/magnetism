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

package com.levelonelabs.aimbot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.levelonelabs.aim.AIMAdapter;
import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMClient;
import com.levelonelabs.aim.AIMGroup;
import com.levelonelabs.aim.AIMSender;
import com.levelonelabs.aim.XMLizable;


/**
 * The main Aimbot that uses the aim package to communicate with AIM and
 * provides services via AIM through various bot modules that perform action
 * when clients make specific queries to the screename registered by this bot.
 * 
 * @author Scott Oster (ostersc@alumn.rpi.edu)
 * 
 * @created January 10, 2002
 */
public class AIMBot extends AIMAdapter {
	public static final String ROLE_USER = "User";
	public static final String ROLE_ENEMY = "Enemy";
	public static final String ROLE_ADMINISTRATOR = "Administrator";

	private static final String PERSISTANCE_FILENAME = "persistance.xml";
	private static Logger logger = Logger.getLogger(AIMBot.class.getName());
	/** A handle to the outgoing AIM connection */
	protected AIMSender aim;
	private String username;
	private String password;
	private boolean autoAdd = false;
	private Hashtable services;
	private Hashtable groups;
	private List modules;
	private boolean enforceUser;
	private String nonUserResponse;


	/**
	 * Constructor for the AIMBot object
	 */
	public AIMBot() {
		services = new Hashtable();
		groups = new Hashtable();
		modules = new ArrayList();
	}


	/**
	 * Add the specified username as an admin of the bot, if it is not already
	 * 
	 * @param admin
	 */
	private void verifyAdmin(String admin) {
		if (admin.equals("")) {
			return;
		}
		AIMBuddy adminBuddy = aim.getBuddy(admin);
		if (adminBuddy == null) {
			adminBuddy = new AIMBuddy(admin);
		}
		if (!adminBuddy.hasRole(AIMBot.ROLE_USER)) {
			adminBuddy.addRole(AIMBot.ROLE_USER);
			logger.info("Adding " + AIMBot.ROLE_USER + " role to " + admin);
		}
		if (!adminBuddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
			adminBuddy.addRole(AIMBot.ROLE_ADMINISTRATOR);
			logger.info("Adding " + AIMBot.ROLE_ADMINISTRATOR + " role to " + admin);
		}
		if (aim.getBuddy(adminBuddy.getName()) == null) {
			aim.addBuddy(adminBuddy);
			logger.info("Adding " + admin + " as an admin.");
		}
	}


	/**
	 * Start up the bot.
	 * 
	 * @param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		String propsFile = System.getProperty("config.file", "bot.properties");

		AIMBot bot = new AIMBot();
		bot.init(propsFile);
	}


	/**
	 * Read in the properties file and configure the bot from it.
	 * 
	 * @param propertiesFileName
	 *            the name of the properties file to look for with the
	 *            classloader.
	 */
	public void init(String propertiesFileName) {
		/*
		 * We use a properties file to define the username,password, and
		 * optionally the log level
		 */
		Properties props = null;
		try {
			InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFileName);
			props = new Properties();
			props.load(is);
		} catch (Exception e) {
			logger.severe("Failed to load props file (" + propertiesFileName + ")!");
			logger
				.severe("You must configure bot.properties, or add -Dconfig.file=<CONFIGFILE> to use a different one(must be on classpath).");
			e.printStackTrace();
			System.exit(-1);
		}

		//Setup the aim options
		String userP = props.getProperty("aim.username");

		//remove whitespace from username, as AOL ignores it
		userP = userP.replaceAll(" ", "");
		String passP = props.getProperty("aim.password");
		String autoaddP = props.getProperty("bot.autoadd", "false").trim();
		String enforceUserP = props.getProperty("bot.enforceUser", "true").trim();
		String profileP = props.getProperty("bot.profile", "I'm running code from:\nhttp://jaimbot.sourceforge.net/")
			.trim();
		String nonUserResponseP = props.getProperty("bot.nonuser.response",
			"Sorry, you must be a user of this system to send requests.").trim();

		setupAIM(userP, passP, profileP, nonUserResponseP, autoaddP, enforceUserP);

		//Setup the Logger
		String loglevel = props.getProperty("logger.level");
		String logFile = props.getProperty("logger.file", username + "_aimbot.log.xml");
		setupLogger(loglevel, logFile);

		String autoPersist = props.getProperty("bot.autopersist", "true").trim();
		//sets autoPersist
		if (autoPersist.equalsIgnoreCase("true")) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					persist();
				}
			});
		}

		//load in the mod names
		List modNames = new ArrayList();
		for (int i = 0; i < props.size(); i++) {
			if (props.containsKey("mod." + i)) {
				String modName = props.getProperty("mod." + i);
				modNames.add(modName);
			}
		}
		loadModules(modNames);

		depersist();
		String admin = props.getProperty("bot.admin", "").trim();

		//remove whitespace from username, as AOL ignores it
		admin = admin.replaceAll(" ", "");
		verifyAdmin(admin);
		aim.signOn();
	}


	private void setupAIM(String user, String pass, String profile, String nonUserResponse, String autoAddUsers,
		String enforceUser) {
		this.nonUserResponse = nonUserResponse;
		if ((user == null) || (pass == null) || user.trim().equals("") || pass.trim().equals("")) {
			logger.severe("ERROR: invalid username or password.");
			System.exit(-1);
		} else {
			this.username = user;
			this.password = pass;
		}

		//check for autoadd
		this.autoAdd = false;
		if (autoAddUsers.equalsIgnoreCase("true")) {
			this.autoAdd = true;
		}

		//check for enforceUser
		this.enforceUser = true;
		if (enforceUser.equalsIgnoreCase("false")) {
			this.enforceUser = false;
		}

		aim = new AIMClient(username, password, profile, nonUserResponse, this.autoAdd);
		aim.addAIMListener(this);
	}


	private void setupLogger(String logLevel, String logFilename) {
		Level level = null;
		try {
			level = Level.parse(logLevel);
		} catch (Exception e) {
			System.err.println("ERROR parsing log level, defaulting to INFO");
			level = Level.INFO;
		}

		try {
			Handler fh = new FileHandler(logFilename);
			Logger.getLogger("").addHandler(fh);

			Logger.getLogger("").setLevel(level);
			Handler[] handlers = Logger.getLogger("").getHandlers();
			for (int i = 0; i < handlers.length; i++) {
				handlers[i].setLevel(level);
			}
		} catch (Exception e) {
			logger.severe("ERROR: unable to attach FileHandler to logger!");
			e.printStackTrace();
		}
	}


	/**
	 * Returns the AIM username of the bot.
	 * 
	 * @return the AIM username of the bot.
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * Gets the specified group
	 * 
	 * @param groupName
	 * 
	 * @return The group value
	 */
	public AIMGroup getGroup(String groupName) {
		return (AIMGroup) this.groups.get(groupName);
	}


	/**
	 * Gets an enum of the groups of the AIMBot
	 * 
	 * @return The groups enumeration
	 */
	public Enumeration getGroups() {
		return this.groups.elements();
	}


	/**
	 * Will call the appropriate bot module to service the request.
	 * 
	 * @param buddy
	 *            the buddy making the request
	 * @param request
	 *            the text of the request
	 */
	public void handleMessage(AIMBuddy buddy, String request) {
		if (buddy != null) {
			logger.info(buddy.getName() + " said: " + request);
		} else {
			logger.info("Ignoring request:" + request + " from null buddy.");
			return;
		}

		if (buddy.hasRole(ROLE_ENEMY)) {
			logger.info("Ignoring request:" + request + " from Enemy:" + buddy.getName() + " and attempting to warn");
			retaliate(buddy);
			return;
		}

		if (this.enforceUser && !buddy.hasRole(ROLE_USER)) {
			//if we are enforcing the user list and the buddy isnt a user,
			// ignore the request and send the non-user response
			logger.info("Ignoring request:" + request + " from buddy that isn't a user, sending non user response.");
			if (this.nonUserResponse != null && !this.nonUserResponse.trim().equals("")) {
				aim.sendMessage(buddy, this.nonUserResponse);
			}
			return;
		}

		StringTokenizer stok = new StringTokenizer(request, " ");
		String keyword = "";
		if (stok.hasMoreTokens()) {
			keyword = stok.nextToken().toLowerCase().trim();
		}

		//handle bot functionality first
		if (keyword.equals("help")) {
			StringBuffer sb = new StringBuffer();
			if (stok.hasMoreTokens()) {
				try {
					int ind = Integer.parseInt(stok.nextToken());
					BotModule bm = (BotModule) modules.get(ind);
					sb.append("Help for " + bm.getName() + ":\n" + bm.help());
					aim.sendMessage(buddy, sb.toString());
					return;
				} catch (Exception e) {
				}
			}
			sb.append("For information on a specific service, type <I>help</I>");
			sb.append(" followed by the service number listed below.\n");
			sb.append("<B>Current Services:</B>\n");
			for (int i = 0; i < modules.size(); i++) {
				sb.append(i + ") " + ((BotModule) modules.get(i)).getName() + "    ");
			}
			sb.append("\n<b>EXAMPLE: help 1</b>");

			aim.sendMessage(buddy, sb.toString());
			return;
		} else if (keyword.equals("persist")) {
			if (buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
				boolean success = this.persist();
				if (success) {
					aim.sendMessage(buddy, "Persistance Succeeded.");
				} else {
					aim.sendMessage(buddy, "Persistance Failed!");
				}
			}

			return;
		}

		BotModule mod = (BotModule) services.get(keyword);
		if (mod != null) {
			logger.info("Request(" + keyword + ") being serviced by: " + mod.getName());
			mod.performService(buddy, request);
		} else {
			mod = (BotModule) modules.get(0);
			if (mod != null) {
				logger.info("Default Request (" + keyword + ") being serviced by: " + mod.getName());
				mod.performService(buddy, request);
			} else {
				logger.severe("Couldn't find mod to service request(" + keyword + ").");
			}
		}
	}


	/**
	 * Called when AIM encounters an error
	 * 
	 * @param error
	 *            the error code
	 * @param message
	 *            decsriptive text describing the error
	 */
	public void handleError(String error, String message) {
		logger.severe("ERROR(" + error + "): " + message);
	}


	/**
	 * Handle being warned from others. Mark them with the Enemy role and warn
	 * them back after a witty response.
	 * 
	 * @param enemy
	 *            the buddy that warned us
	 * @param amount
	 *            the current warning level
	 */
	public void handleWarning(AIMBuddy enemy, int amount) {
		if ((enemy == null) || (enemy.getName().equals("anonymous"))) {
			logger.info("AIMBot UNDER ATTACK!: anonymous raised warning to " + amount + " ***");
			return;
		}
		logger.info("AIMBot UNDER ATTACK!: " + enemy.getName() + " raised warning to " + amount + " ***");

		retaliate(enemy);
	}


	/**
	 * Warn a user, and mark them as an Enemy. If they were already an Enemy,
	 * ban them.
	 * 
	 * @param enemy
	 */
	private void retaliate(AIMBuddy enemy) {
		if (!enemy.hasRole(ROLE_ENEMY)) {
			enemy.addRole(ROLE_ENEMY);
			aim.sendMessage(enemy,
				"In the End, we will remember not the words of our enemies, but the silence of our friends");
		} else {
			aim.banBuddy(enemy);
		}
		aim.sendWarning(enemy);
	}


	/**
	 * Adds a Group
	 * 
	 * @param group
	 *            The feature to be added to the Group attribute
	 */
	public void addGroup(AIMGroup group) {
		this.groups.put(group.getName(), group);
	}


	/**
	 * Removes the specified group
	 * 
	 * @param group
	 */
	public void removeGroup(AIMGroup group) {
		this.groups.remove(group.getName());
	}


	/**
	 * All bot modules will call this and pass a reference to themselves and an
	 * ArrayList containing the keywords they want to listen for
	 * 
	 * @param mod
	 *            the module
	 */
	protected void registerModule(BotModule mod) {
		this.modules.add(mod);
		ArrayList servicesList = mod.getServices();
		if (servicesList != null) {
			for (int i = 0; i < servicesList.size(); i++) {
				registerService((String) servicesList.get(i), mod);
			}
		}
	}


	/**
	 * Load BotModules and register them
	 * 
	 * @param modNames
	 *            List of names to load
	 */
	private void loadModules(List modNames) {
		//wipe the slate clean
		services = new Hashtable();
		modules = new ArrayList();

		//iterate the modules
		for (int i = 0; i < modNames.size(); i++) {
			try {
				//grab a mod class
				String modName = (String) modNames.get(i);
				Class modclass = Class.forName(modName);
				java.lang.reflect.Constructor constructor;
				Class[] carr = {this.getClass()};
				Object[] oarr = {this};

				//make a constructor
				constructor = modclass.getConstructor(carr);
				//get an instance
				BotModule mod = (BotModule) constructor.newInstance(oarr);

				//register the mod
				registerModule(mod);
				logger.info("Loading mod (" + mod.getName() + ")");
			} catch (Exception e) {
				e.printStackTrace();
				logger.severe("Unable to load mod=" + modNames.get(i));
			}
		}
	}


	/**
	 * Method to persist all state to XML. Saves username, password, all
	 * buddies, all groups. Modules and services will be reloaded on
	 * depersistance so we dont need to save them; just give them a chance to
	 * persist and then depersist them next time loadmoduels is called.
	 * 
	 * @return returns true iff the persistance succeeded.
	 */
	private boolean persist() {
		Document doc = createDomDocument();
		Element root = doc.createElement("AIMBot_Persistance");
		root.setAttribute("username", this.username);
		//root.setAttribute("password", this.password);
		doc.appendChild(root);

		//add buddies
		Element buddiesElem = doc.createElement("buddies");
		for (Iterator iter = aim.getBuddyNames(); iter.hasNext();) {
			String name = (String) iter.next();
			AIMBuddy bud = aim.getBuddy(name);
			if (bud == null) {
				continue;
			}
			Element buddyElem = doc.createElement("buddy");
			bud.writeState(buddyElem);
			buddiesElem.appendChild(buddyElem);
		}
		root.appendChild(buddiesElem);

		//add groups
		Element groupsElem = doc.createElement("groups");
		for (Iterator iter = groups.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			AIMGroup grp = (AIMGroup) groups.get(name);
			if (grp == null) {
				continue;
			}
			Element groupElem = doc.createElement("group");
			grp.writeState(groupElem);
			groupsElem.appendChild(groupElem);
		}
		root.appendChild(groupsElem);

		//add modules
		Element modsElem = doc.createElement("modules");
		for (int i = 0; i < modules.size(); i++) {
			BotModule mod = (BotModule) modules.get(i);
			if (mod instanceof XMLizable) {
				//persist the mod
				Element modElem = doc.createElement("module");
				modElem.setAttribute("name", mod.getName());
				try {
					((XMLizable) mod).writeState(modElem);
					modsElem.appendChild(modElem);
				} catch (Throwable t) {
					logger.severe("Problem persisting mod +[" + mod.getName() + "], ignoring it.");
				}
			}
		}
		root.appendChild(modsElem);

		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			// Prepare the output file
			File file = new File(this.username + "_" + PERSISTANCE_FILENAME);
			Result result = new StreamResult(file);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);

		} catch (Exception e) {
			logger.severe("ERROR: failed to persist!");
			e.printStackTrace();
			return false;
		}

		logger.info("Successfully persisted AIMBot to file:" + this.username + "_" + PERSISTANCE_FILENAME);

		return true;
	}


	/**
	 * Method depersist.
	 * 
	 * @return returns true iff the depersistance succeeded.
	 */
	private boolean depersist() {
		// Create a DOM builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// Parse the XML String to create a document
		Document d;
		try {
			d = factory.newDocumentBuilder().parse(new File(this.username + "_" + PERSISTANCE_FILENAME));
		} catch (IOException e) {
			logger.info("Couldn't locate persistance file; starting fresh.");
			return false;
		} catch (Exception e) {
			logger.severe("Error reading persistance file; aborting depersitance.");
			e.printStackTrace();
			return false;
		}

		// Get document root
		Element root = d.getDocumentElement();

		if ((root == null) || !root.getTagName().equalsIgnoreCase("AIMBot_Persistance")) {
			logger.severe("Error parsing persistance file; aborting depersitance.");
			return false;
		}

		//parse buddies
		Element buddiesTag = (Element) root.getElementsByTagName("buddies").item(0);
		NodeList list = buddiesTag.getElementsByTagName("buddy");
		for (int i = 0; i < list.getLength(); i++) {
			Element buddyElem = (Element) list.item(i);
			String name = buddyElem.getAttribute("name");

			AIMBuddy buddy = new AIMBuddy(name);
			buddy.readState(buddyElem);
			aim.addBuddy(buddy);
		}

		//parse groups
		Element groupsTag = (Element) root.getElementsByTagName("groups").item(0);
		list = groupsTag.getElementsByTagName("group");
		for (int i = 0; i < list.getLength(); i++) {
			Element groupElem = (Element) list.item(i);
			String name = groupElem.getAttribute("name");

			AIMGroup group = new AIMGroup(name);
			group.readState(groupElem);
			addGroup(group);
		}

		//parse modules
		Element modsTag = (Element) root.getElementsByTagName("modules").item(0);
		list = modsTag.getElementsByTagName("module");
		for (int i = 0; i < list.getLength(); i++) {
			Element modElem = (Element) list.item(i);
			String name = modElem.getAttribute("name");
			for (int j = 0; j < modules.size(); j++) {
				BotModule mod = (BotModule) modules.get(j);
				if ((mod.getName().equals(name)) && (mod instanceof XMLizable)) {
					((XMLizable) mod).readState(modElem);
				}
			}
		}

		logger.info("Successfully depersisted AIMBot from file:" + this.username + "_" + PERSISTANCE_FILENAME);

		return true;
	}


	/**
	 * createDomDocument - creates an empty document
	 * 
	 * @return Document - returns an empty document
	 */
	protected static Document createDomDocument() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			return doc;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return null;
	}


	/**
	 * Registers a specific module to handle requests that start with a specific
	 * string
	 * 
	 * @param keyword
	 *            a word to look for in client text that identifies a bot module
	 *            to be called
	 * @param mod
	 *            the bot module that will handle this request
	 */
	private void registerService(String keyword, BotModule mod) {
		services.put(keyword, mod);
	}
}
package com.dumbhippo.aimbot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.aim.ScreenName;

class Config {

	private static Logger logger = GlobalSetup.getLogger(Config.class);
	
	private static final String CONFIG_FILE = "conf/imbot.xml";
	
	private static Config defaultConfig;
	
	private List<BotConfig> botConfigs;
	
	static Config getDefault() {
		if (defaultConfig == null)
			defaultConfig = new Config();
		return defaultConfig;
	}
	
	private Config() {
		botConfigs = new ArrayList<BotConfig>();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc;
		try {
			doc = factory.newDocumentBuilder().parse(new File(CONFIG_FILE));
		} catch (Exception e) {
			throw new RuntimeException("Failed to load config file", e);
		}
		Element rootNode = doc.getDocumentElement();
		if (rootNode == null || !rootNode.getTagName().equals("imbot"))
			throw new RuntimeException("Config file root node should be <imbot>");
		Node child = rootNode.getFirstChild();
		while (child != null) {
			if (child instanceof Element) {
				Element element = (Element) child;
				if (element.getTagName().equals("bot")) {
					String name = element.getAttribute("name");
					String pass = element.getAttribute("password");
					if (name == null || pass == null)
						throw new RuntimeException("Element <bot> needs name and password attributes");
					BotConfig bot = new BotConfig(new ScreenName(name), pass);
					botConfigs.add(bot);
					logger.info("Loaded bot " + bot.getName());
				} else {
					throw new RuntimeException("Element " + element.getTagName() + " in config file, don't know that one");
				}
			}
			child = child.getNextSibling();
		}
		
		if (botConfigs.isEmpty()) {
			throw new RuntimeException("imbot config file lists no <bot> elements, need at least one");
		} else {
			logger.info("Loaded " + botConfigs.size() + " bots for the bot pool");
		}
	}
	
	List<BotConfig> getBotConfigs() {
		return Collections.unmodifiableList(botConfigs);
	}
}

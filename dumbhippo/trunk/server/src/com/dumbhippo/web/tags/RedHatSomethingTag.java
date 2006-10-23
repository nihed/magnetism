package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.web.WebEJBUtil;

public class RedHatSomethingTag extends SimpleTagSupport {

	// if you put HTML chars in here you have to add xml escaping on output below
	private static final String[] SOMETHINGS = {
		"Island",
		"Garden Path",
		"Remix",
		"Glovebox",
		"Adventure",
		"Peninsula",
		"Rose Garden",
		"Flying Trapeze",
		"Science Project",
		"Forbidden Fruit",
		"Flying Car",
		"Spaceship",
		"Medicine Cabinet",
		"Shoeshine",
		"Improvisation",
		"Stealthy Gill-Equipped Aqua Warrior",
		"Butter Knife",
		"Crayon Sharpener",
		"Chocolate Orange",
		"Linen Closet",
		"Igloo",
		"Fireplug",
		"Electric Fence",
		"Water Pistol",
		"Something-or-other",
		"Bag of Chips",
		"Mechanical Duck",
		"Racket",
		"Conspiracy",
		"Riot",
		"Joint",
		"Evidence",
		"Ribbit",
		"Swarmy",
		"Juke",
		"Circus",
		"Channel",
		"Tizzy",
		"Fuzebox",
		"Fling",
		"Popfly",
		"Broadside",
		"Bustle",
		"Freebase",
		"Pandemic",
		"Swarm",
		"Swoop",
		"Sweep",
		"Tangle",
		"Whisper",
		"Cluster",
		"Ruffle",
		"On-Line Cyber Cafe"
	};
	
	/* default to true for safety */
	private static boolean stealthMode = true;
	private static boolean loadedStealthMode = false;
	
	private String pickSomething() {
		// use seconds not milliseconds, milliseconds might always be multiples of 10 or something 
		int i = (int) ((System.currentTimeMillis() / 1000) % SOMETHINGS.length);
		return SOMETHINGS[i];
	}
	
	@Override
	public void doTag() throws IOException {

		if (!loadedStealthMode) {
	        Configuration configuration = WebEJBUtil.defaultLookup(Configuration.class);
	        
			String stealthModeString = configuration.getProperty(HippoProperty.STEALTH_MODE);
			stealthMode = Boolean.parseBoolean(stealthModeString);
			loadedStealthMode = true;
		}
		
		JspWriter writer = getJspContext().getOut();
		if (stealthMode)
			writer.print("Today's Word Is: " + pickSomething() + "");
		else
			writer.print("<a href=\"http://redhat.com\">A Red Hat " + pickSomething() + "</a>");
	}
}

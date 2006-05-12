package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

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
		"Ruffle"
	};
	
	private String pickSomething() {
		// use seconds not milliseconds, milliseconds might always be multiples of 10 or something 
		int i = (int) ((System.currentTimeMillis() / 1000) % SOMETHINGS.length);
		return SOMETHINGS[i];
	}
	
	@Override
	public void doTag() throws IOException {
		
		JspWriter writer = getJspContext().getOut();
		writer.print("<a href=\"http://redhat.com\">A Red Hat " + pickSomething() + "</a>");
	}
}

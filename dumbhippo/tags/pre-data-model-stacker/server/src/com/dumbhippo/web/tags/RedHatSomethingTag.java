package com.dumbhippo.web.tags;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.StringUtils;
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
		"On-Line Cyber Cafe",
		"Action Item",
		"Air Freshener",
		"Ant Farm",
		"Barn Raising",
		"Bed of Nails",
		"Blasting Zone",
		"Blind Date",
		"Box of Matches",
		"Bubble Bath",
		"Butter Churn",
		"Candy Necklace",
		"Cereal Box Prize",
		"Chocolate Rabbit",
		"Crowbar",
		"Cupcake Factory",
		"Dance Contest",
		"Dart Board",
		"Dessert Tray",
		"Door Prize",
		"Double Whammy",
		"Dream Boat",
		"Drum Solo",
		"Energy Drink",
		"Extra Inning",
		"Flying Carpet",
		"Garden Gnome",
		"Glee Club",
		"Haunted House",
		"Honeymoon",
		"Honey Pot",
		"Hot Spot",
		"Hypno-Ring",
		"Ice Cube Tray",
		"Jamboree",
		"Jelly Bean Jar",
		"Jug Band",
		"Kissing Booth",
		"Kite String",
		"Laundry Basket",
		"Lemonade Stand",
		"Magic Garden",
		"Merry-Go-Round",
		"Mirror Maze",
		"Moon Base",
		"Mystery Spot",
		"Pancake House",
		"Paper Airplane",
		"Precious Moment",
		"Prom Queen",
		"Puppet Show",
		"Request Line",
		"Robot Army",
		"Rodeo",
		"Root Cellar",
		"Rubber Band Ball",
		"Rumble Seat",
		"Secret Staircase",
		"Ski Lodge",
		"Squeaky Toy",
		"Stamp Collection",
		"Stir-Fry",
		"Super Freak",
		"Sweat Lodge",
		"Swizzle Stick",
		"Theme Park",
		"Toaster Oven",
		"Treasure Chest",
		"Vacation Home",
		"Water Slide",
		"Wishing Well",
		"Wood Pile"		
	};
	
	/* default to true for safety */
	private static boolean stealthMode = true;
	private static boolean loadedStealthMode = false;
	
	private String pickSomething() {
		return StringUtils.getRandomString(SOMETHINGS);		
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

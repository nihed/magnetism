package com.dumbhippo.aimbot;

import java.util.Random;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMClient;
import com.levelonelabs.aim.AIMListener;

class Bot implements Runnable {

	private AIMClient aim;
	private Random random;

	class Listener implements AIMListener {
		public void handleConnected() {
			System.out.println("connected");
		}
		
		public void handleDisconnected() {
			System.out.println("disconnected");
		}
		
		public void handleMessage(AIMBuddy buddy, String request) {
			System.out.println("message from " + buddy.getName() + ": " + request);
			saySomethingRandom(buddy);
		}
		
		public void handleWarning(AIMBuddy buddy, int amount) {
			System.out.println("warning from " + buddy.getName());
		}
		
		public void handleBuddySignOn(AIMBuddy buddy, String info) {
			System.out.println("Buddy sign on " + buddy.getName());
		}
		
		public void handleBuddySignOff(AIMBuddy buddy, String info) {
			System.out.println("Buddy sign off " + buddy.getName());
		}
		
		public void handleError(String error, String message) {
			System.out.println("error: " + error + " message: " + message);
		}
		
		public void handleBuddyUnavailable(AIMBuddy buddy, String message) {
			System.out.println("buddy unavailable: " + buddy.getName() + " message: " + message);
		}
		
		public void handleBuddyAvailable(AIMBuddy buddy, String message) {
			System.out.println("buddy available: " + buddy.getName() + " message: " + message);
			if (buddy.getName().equals("bryanwclark")) {
				saySomethingRandom(buddy);
			} else if (buddy.getName().equals("hp40000")) {
				saySomethingRandom(buddy);
			} else if (buddy.getName().equals("dfxfischer")) {
				saySomethingRandom(buddy);
			}
		}
	}
	
	Bot() {
		random = new Random();
		
		aim = new AIMClient("DumbHippoBot", "s3kr3tcode", "My Profile!",
				"You aren't a buddy!", true /*auto-add everyone as buddy*/);
		aim.addAIMListener(new Listener());
	}

	void saySomethingRandom(AIMBuddy buddy) {
		switch (random.nextInt(5)) {
		case 0:
			aim.sendMessage(buddy, "You suck");
			break;
		case 1:
			aim.sendMessage(buddy, "Mortle frobbles the tib tom");
			break;
		case 2:
			aim.sendMessage(buddy, "Hippo Hippo Hooray");
			break;
		case 3:
			aim.sendMessage(buddy, "Do I repeat myself often?");
			break;
		case 4:
			aim.sendMessage(buddy, "I may be dumb, but I'm not stupid");
			break;
		}
	}

	public void run() {
		aim.signOn();
	}
}

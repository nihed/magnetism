package com.dumbhippo.aimbot;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.dumbhippo.aim.Buddy;
import com.dumbhippo.aim.Client;
import com.dumbhippo.aim.Listener;

class Bot implements Runnable {

    static private final String PING = "PING";
	static private Timer timer;
	
	private Client aim;
	private Random random;
	private SelfPinger pinger;
	
    /** FIXME this is a bit broken, because Client isn't threadsafe to 
     * speak of, and the timer is running in a 
     * separate thread... I synchronized the "send a frame" method so 
     * at least we won't send completely corrupt garbage, but didn't 
     * comprehensively make the class threadsafe -hp
     */
	class SelfPinger extends TimerTask {
	    // check connection ever "TIME_DELAY" milliseconds (5 mins)
	    private static final long TIME_DELAY = 5 * 60 * 1000;
	    
	    SelfPinger() {
	    	timer.schedule(this, TIME_DELAY, TIME_DELAY);
	    }
	    
		@Override
		public void run() {
			if (aim != null && aim.getOnline()) {
				// clock setting back just works since we do a 
				// ping then which fixes last message timestamp
				
				long now = System.currentTimeMillis();
				long last = aim.getLastMessageTimestamp();

				// only ping if we've been idle
				if ((now - last) > TIME_DELAY) {
					
					System.out.println("Self-pinging at " + new Date(now));
					
					aim.sendMessageRaw(aim.getName(), PING);
					
					// set up a one-shot to verify results
					timer.schedule(new Ponger(), TIME_DELAY);
				}
            } else {
            	cancel();
            }
		}
		
		class Ponger extends TimerTask {

			@Override
			public void run() {
				// if we've now been idle for a while, sign us off, 
				// the connection is hosed
				if (aim != null && aim.getOnline()) {
					long now = System.currentTimeMillis();
					long last = aim.getLastMessageTimestamp();

					System.out.println("Ping check, last message at " + new Date(last));
					
					if ((now - last) > TIME_DELAY*2) {
						System.out.println("Last message too old, signing off");
						aim.signOff();
					}
				}
			}
		}
	}
	
	class BotListener implements Listener {
		public void handleConnected() {
			System.out.println("connected");
		}
		
		public void handleDisconnected() {
			System.out.println("disconnected");
		}
		
		public void handleMessage(Buddy buddy, String request) {
			System.out.println("message from " + buddy.getName() + ": " + request);
			saySomethingRandom(buddy);
		}
		
		public void handleWarning(Buddy buddy, int amount) {
			System.out.println("warning from " + buddy.getName());
		}
		
		public void handleBuddySignOn(Buddy buddy, String info) {
			System.out.println("Buddy sign on " + buddy.getName());
		}
		
		public void handleBuddySignOff(Buddy buddy, String info) {
			System.out.println("Buddy sign off " + buddy.getName());
		}
		
		public void handleError(String error, String message) {
			System.out.println("error: " + error + " message: " + message);
		}
		
		public void handleBuddyUnavailable(Buddy buddy, String message) {
			System.out.println("buddy unavailable: " + buddy.getName() + " message: " + message);
		}
		
		public void handleBuddyAvailable(Buddy buddy, String message) {
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
		// don't put things in constructor that need to be recreated 
		// for each run()
		
		synchronized(Bot.class) {
			if (timer == null)
				timer = new Timer(true); // daemon thread
		}
		
		random = new Random();
	}

	void saySomethingRandom(Buddy buddy) {
		System.out.println("saying something random to " + buddy.getName());
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
		
		aim = new Client("DumbHippoBot", "s3kr3tcode", "My Profile!",
				"You aren't a buddy!", true /*auto-add everyone as buddy*/);
		aim.addListener(new BotListener());
		
		pinger = new SelfPinger();
		
		System.out.println("Signing on...");
		aim.run();
		System.out.println("Signed off.");
		aim = null;
		
		pinger.cancel();
		pinger = null;
	}
	
    /**
     * Strip out HTML from a string
     * 
     * @param line * *
     * @return the string without HTML
     */
    private static String stripHTML(String line) {
        StringBuilder sb = new StringBuilder(line);
        String out = "";

        for (int i = 0; i < (sb.length() - 1); i++) {
            if (sb.charAt(i) == '<') {
                // Most tags
                if ((sb.charAt(i + 1) == '/') || ((sb.charAt(i + 1) >= 'a') && (sb.charAt(i + 1) <= 'z'))
                    || ((sb.charAt(i + 1) >= 'A') && (sb.charAt(i + 1) <= 'Z'))) {
                    for (int j = i + 1; j < sb.length(); j++) {
                        if (sb.charAt(j) == '>') {
                            sb = sb.replace(i, j + 1, "");
                            i--;
                            break;
                        }
                    }
                } else if (sb.charAt(i + 1) == '!') {
                    // Comments
                    for (int j = i + 1; j < sb.length(); j++) {
                        if ((sb.charAt(j) == '>') && (sb.charAt(j - 1) == '-') && (sb.charAt(j - 2) == '-')) {
                            sb = sb.replace(i, j + 1, "");
                            i--;
                            break;
                        }
                    }
                }
            }
        }

        out = sb.toString();
        return out;
    }
}

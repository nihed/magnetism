package com.dumbhippo.aimbot;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.aim.Buddy;
import com.dumbhippo.aim.Client;
import com.dumbhippo.aim.FilterException;
import com.dumbhippo.aim.Listener;
import com.dumbhippo.aim.RawListenerAdapter;
import com.dumbhippo.aim.ScreenName;
import com.dumbhippo.aim.TocError;

class Bot implements Runnable {
	private static Log logger = GlobalSetup.getLog(Bot.class);

	static private Timer timer;
	
	private ScreenName name;
	private String pass;
	
	private Client aim;
	private Random random;
	private SelfPinger pinger;
	
	class SelfPinger extends TimerTask {
	    // check connection every "TIME_DELAY" milliseconds (5 mins)
	    private static final long TIME_DELAY = 5 * 60 * 1000;
	    private static final String PING = "PING";
	    
	    SelfPinger() {
	    	timer.schedule(this, TIME_DELAY, TIME_DELAY);
	    	
	    	aim.addRawListener(new RawListenerAdapter() {
	    		public void handleMessage(ScreenName buddy, String htmlMessage)
				throws FilterException {
	    			if (buddy.equals(aim.getName()) && htmlMessage.equals(PING)) {
	    				logger.debug("filtering out " + PING);
	    				throw new FilterException();
	    			}
	    		}
	    	});
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
					
					logger.debug("Self-pinging at " + new Date(now));
					
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

					logger.debug("Ping check, last message at " + new Date(last));
					
					if ((now - last) > TIME_DELAY*2) {
						logger.debug("Last message too old, signing off");
						aim.signOff();
					}
				}
			}
		}
	}
	
	class BotListener implements Listener {
		public void handleConnected() {
			logger.debug("connected");
		}
		
		public void handleDisconnected() {
			logger.debug("disconnected");
		}
		
		public void handleMessage(Buddy buddy, String request) {
			logger.debug("message from " + buddy.getName() + ": " + request);
			saySomethingRandom(buddy);
		}
		
		public void handleWarning(Buddy buddy, int amount) {
			logger.debug("warning from " + buddy.getName());
		}
		
		public void handleBuddySignOn(Buddy buddy, String info) {
			logger.debug("Buddy sign on " + buddy.getName());
		}
		
		public void handleBuddySignOff(Buddy buddy, String info) {
			logger.debug("Buddy sign off " + buddy.getName());
		}
		
		public void handleError(TocError error, String message) {
			logger.debug("error: " + message);
		}
		
		public void handleBuddyUnavailable(Buddy buddy, String message) {
			logger.debug("buddy unavailable: " + buddy.getName() + " message: " + message);
		}
		
		public void handleBuddyAvailable(Buddy buddy, String message) {
			logger.debug("buddy available: " + buddy.getName() + " message: " + message);
			if (buddy.getName().equals("bryanwclark")) {
				saySomethingRandom(buddy);
			} else if (buddy.getName().equals("hp40000")) {
				saySomethingRandom(buddy);
			} else if (buddy.getName().equals("dfxfischer")) {
				saySomethingRandom(buddy);
			}
		}
	}
		
	Bot(ScreenName name, String pass) {
		// don't put things in constructor that need to be recreated 
		// for each run()
		
		synchronized(Bot.class) {
			if (timer == null)
				timer = new Timer(true); // daemon thread
		}
		
		random = new Random();
		
		this.name = name;
		this.pass = pass;
	}

	void saySomethingRandom(Buddy buddy) {
		logger.debug("saying something random to " + buddy.getName());
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

	public void signOn() {
		if (aim != null)
			throw new IllegalStateException("can't sign on when you're already running");

		logger.debug("Bot signing on...");
		
		Client client = new Client(name, pass, "I am DUMB HIPPO BOT",
				"Hmm, who are you?", true /*auto-add everyone as buddy*/);
		client.addListener(new BotListener());
		
		client.signOn();
		
		if (client.getOnline()) {
			aim = client;
		} else {
			logger.error("Bot failed to sign on");
		}
	}
	
	public void run() {
		if (aim == null)
			signOn();
		
		if (aim != null) {
			pinger = new SelfPinger();
			
			logger.debug("Bot thread waiting for events...");
			aim.read();
			logger.debug("Bot thread exiting");
			aim = null;
			
			pinger.cancel();
			pinger = null;
		}
	}
	
	public void signOff() {
		if (aim != null) {
			aim.signOff();
			// this should cause read() to return so in run() we set aim = null
		}
	}
	
	public boolean getOnline() {
		return aim != null && aim.getOnline();
	}
	
	// Normally called from a separate thread
	public void doInvite(BotTaskInvite invite) throws BotTaskFailedException {
		
		logger.debug("Bot " + name + " got invite task from " + invite.getFromAimName() + " for " + invite.getInviteeAimName());
		
		// we save a reference in case it gets set to null by the main thread
		Client client = aim;
		
		if (client == null) {
			throw new BotTaskFailedException("bot is not running");
		}
		
		ScreenName recipientName = new ScreenName(invite.getInviteeAimName());
		Buddy recipient = client.addBuddy(recipientName);
		
		XmlBuilder builder = new XmlBuilder();
		builder.append("Invitation from ");
		builder.appendEscaped(invite.getFromAimName());
		builder.appendTextNode("a", "click here to join", "href", invite.getInviteUrl());
		
		client.sendMessage(recipient, builder.toString());
		
		if (!client.getOnline()) {
			// most likely this means we failed (there's really no way to know reliably)
			throw new BotTaskFailedException("offline right when we sent the message");
		}
	}
}

package com.dumbhippo.aimbot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.aim.ScreenName;

public class BotPool {
	
	private static Log logger = GlobalSetup.getLog(BotPool.class);
	private Map<ScreenName,BotQueue> bots;
	
	static private class BotQueue {
		private ScreenName name;
		private Thread botThread;
		private Bot bot;		
		private LinkedBlockingQueue<BotTask> taskQueue;
		private Thread taskThread;
		
		class TaskDispatcher implements Runnable {
			private void doTask(BotTask task) {
				try {
					if (task instanceof BotTaskNoop) {
						// nothing
					} else if (task instanceof BotTaskInvite) {
						bot.doInvite((BotTaskInvite) task);
					} else {
						throw new RuntimeException("unknown bot task " + task.getClass().getCanonicalName());
					}
				} catch (BotTaskFailedException e) {
					// FIXME - I guess we could put it back in the queue or something.
					// there's really no way to make this AIM thing 100% reliable though.
					logger.error("DROPPED A TASK", e);
				}
			}
			
			public void run() {
				while (true) {
					if (!isActive()) {
						return; // end thread
					}
					
					BotTask task;
					try {
						task = taskQueue.take();
					} catch (InterruptedException e) {
						task = null;
					}
					
					if (task != null)
						doTask(task);
				}
			}
		}
		
		BotQueue(String name, String pass) {
			this.name = new ScreenName(name);
			this.bot = new Bot(this.name, pass);
			taskQueue = new LinkedBlockingQueue<BotTask>();
		}
				
		ScreenName getName() {
			return name;
		}
		
		void put(BotTask task) {
			if (bot == null)
				throw new IllegalStateException("have to activate the bot");

			while (true) {
				try {
					taskQueue.put(task);
					break;
				} catch (InterruptedException e) {
				}
			}
		}
		
		boolean isActive() {
			return botThread != null;
		}
		
		void activate() {
			if (botThread != null)
				throw new IllegalStateException("already active");
			
			bot.signOn();
			
			if (bot.getOnline()) {
				botThread = new Thread(bot);
				botThread.setDaemon(true);
				botThread.start();
				
				taskThread = new Thread(new TaskDispatcher());
				taskThread.setDaemon(true);
				taskThread.start();
			} else {
				// Humph. FIXME
			}
		}
		
		void passivate() {
			bot.signOff();

			// shut down bot thread
			while (true) {
				try {
					if (botThread.isAlive())
						botThread.join();
					botThread = null;
					break;
				} catch (InterruptedException e) {
				}
			}
			
			// make sure the task thread wakes up
			while (true) {
				try {
					taskQueue.put(new BotTaskNoop());
					break;
				} catch (InterruptedException e) {
				}
			}
			// shut down task thread
			while (true) {
				try {
					taskThread.join();
					taskThread = null;
					break;
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	BotPool() {
		
		bots = new HashMap<ScreenName,BotQueue>();
		
		// FIXME read this from a config file
		BotQueue queue;
		
		queue = new BotQueue("DumbHippoBot", "s3kr3tcode");
		bots.put(queue.getName(), queue);
	}
	
	// this is called from multiple threads
	public synchronized void put(BotTask task) {
		// for now we always just use one bot
		
		BotQueue queue;
		
		// pick the one bot at random
		queue = bots.values().iterator().next();
		
		if (!queue.isActive())
			queue.activate();
		
		queue.put(task);
	}
}

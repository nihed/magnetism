package com.dumbhippo.aimbot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.aim.ScreenName;

public class BotPool {
	
	private static Log logger = GlobalSetup.getLog(BotPool.class);
	private List<BotQueue> bots;
	
	static private class BotQueue {
		private ScreenName name;
		private Thread botThread;
		private Bot bot;		
		private LinkedBlockingQueue<BotTask> taskQueue;
		private Thread taskThread;
		private TaskDispatcher taskDispatcher;
		
		class TaskDispatcher implements Runnable {
			
			private boolean quit;
			
			private void doTask(BotTask task) {
				// This is kind of crazy, but not real way to make it 100% reliable I don't think; AIM doesn't 
				// give us a "successfully received" reply to sending anything...
				
				for (int retries = 5; retries > 0; --retries) {
					
					if (quit) {
						logger.error("DROPPED A TASK: dispatcher thread quit: " + task);
						return;
					}
					
					if (!bot.getOnline()) {
						logger.debug("Bot offline, dispatcher waiting for change in it");
						// this also returns on interrupt
						bot.waitForOnlineMaybeChanged();
					}
					
					if (quit) {
						logger.debug("DROPPED A TASK: dispatcher thread quit: " + task);
						return;
					}
					
					try {
						if (task instanceof BotTaskNoop) {
							// nothing
						} else if (task instanceof BotTaskInvite) {
							bot.doInvite((BotTaskInvite) task);
						} else {
							throw new RuntimeException("unknown bot task " + task.getClass().getCanonicalName());
						}
					} catch (BotTaskFailedException e) {
						logger.debug("Task failed; " + retries + " retries remain");
						continue;
					}
					
					return; // Success!
				}
				logger.error("DROPPED A TASK: " + task);
			}
			
			public void run() {
				quit = false;
				while (!quit) {	
					BotTask task;
					try {
						task = taskQueue.take();
					} catch (InterruptedException e) {
						task = null;
					}
					
					if (task != null)
						doTask(task);
				}
				logger.debug(" ...task thread exiting");
			}
			
			public void quit() {
				quit = true;
			}
		}
		
		BotQueue(ScreenName name, String pass) {
			this.name = name;
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
			
			logger.info("Activating queue for bot " + name);
			
			botThread = new Thread(bot);
			botThread.setDaemon(true);
			botThread.setName("Bot-" + name);
				
			taskDispatcher = new TaskDispatcher();
				
			taskThread = new Thread(taskDispatcher);
			taskThread.setDaemon(true);
			taskThread.setName("Tasks-" + name);
			
			botThread.start();
			taskThread.start();
		}
		
		void passivate() {
			
			// sets a flag and may wake up bot thread if needed
			bot.quit();
			// just sets a flag, we kick it below
			taskDispatcher.quit();

			// shut down bot thread
			while (true) {
				try {
					botThread.join();
					botThread = null;
					break;
				} catch (InterruptedException e) {
				}
			}
			
			// may be blocking on either the bot thread or the task queue, kick it
			taskThread.interrupt();
			
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
		
		bots = new ArrayList<BotQueue>();
		
		Config config = Config.getDefault();
		for (BotConfig b : config.getBotConfigs()) {
			BotQueue queue = new BotQueue(b.getName(), b.getPass());
			bots.add(queue);
		}
	}
	
	public void start() {
		logger.info("Starting up all the bots");
		for (BotQueue q : bots) {
			q.activate();
		}
		logger.info("All bots launched");
	}
	
	// this is called from multiple threads, a cheesy synchronized on the 
	// method works fine for now since this is the only place we mess 
	// with the bots
	public synchronized void put(BotTask task) {
		// for now we always just use the first bot
		
		BotQueue queue;
		
		// pick the first bot from the config file
		queue = bots.iterator().next();
		
		queue.put(task);
	}
}

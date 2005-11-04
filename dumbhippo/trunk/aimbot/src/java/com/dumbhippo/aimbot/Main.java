package com.dumbhippo.aimbot;

public class Main {

	public static void main(String[] args) {
		Bot bot = new Bot();
		bot.signOn();

		// the Bot starts a daemon thread; here we 
		// just want to wait forever until killed by 
		// the OS. This means when we're killed by the 
		// OS the JVM will exit.
		while (true) {
			try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
			}
		}
	}
	
}

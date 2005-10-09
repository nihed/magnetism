/**
 * 
 */
package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.client.EjbLink;

/**
 * App that hammers a server with remote session bean calls.
 * Creates test data, so should be run on an empty/trash database.
 * 
 * @author hp
 *
 */
public class TestClient {

	private EjbLink ejb;
	private TestGlueRemote test;
	
	public TestClient() {
		XMPPConnection.DEBUG_ENABLED = true;
		
		try {
			ejb = new EjbLink(true);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new Error("Could not connect to server", e);
		}

		test = ejb.getTestGlue();
		
		if (test == null)
			System.err.println("TestGlueRemote is null");
	}
	
	public void loadData() {
		test.loadTestData();	
	}
	
	public List<Thread> spawnAccountTesters() {
		HippoAccount account = test.getAnAccount();
		System.out.println("Got single account " + account.getOwner().getId());

		Set<HippoAccount> accounts = test.getActiveAccounts();

		List<Thread> threads = new ArrayList<Thread>();
		
		Iterator<HippoAccount> i = accounts.iterator();
		while (i.hasNext()) {
			HippoAccount first = i.next();
			if (!i.hasNext())
				break; // just ignore the odd one
			HippoAccount second = i.next();
			
			Runnable tester = new AccountTester(first, second);
			Thread thread = new Thread(tester);
			threads.add(thread);
			thread.start();
			tester = new AccountTester(second, first);
			thread = new Thread(tester);
			threads.add(thread);
			thread.start();
			
			if (XMPPConnection.DEBUG_ENABLED)
				break;
		}
		
		return threads;
	}
	
	public class AccountTester implements Runnable, PacketListener {
		
		private HippoAccount account;
		private HippoAccount friend;
		private XMPPConnection connection;
		
		public AccountTester(HippoAccount account, HippoAccount friend) {
			this.account = account;
			this.friend = friend;
			
			System.out.println("Spawning tester for account " +
					account.getOwner().getId() + " and friend "
					+ friend.getOwner().getId());
		}
		
		public void run() {
			connection = loginToJabber();
			
			Message message = new Message();
			
			message.setTo(friend.getOwner().getId() + "@dumbhippo.com");
			message.setBody("This is the message body");
			
			connection.sendPacket(message);
			
			while (connection.isConnected()) {
				try {
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {
					// ignore
				}
			}
			
			connection = null;
			
			System.out.println("Disconnected, ending tester");
		}
		
		private XMPPConnection loginToJabber() {
			
			System.out.println("Smack login timeout " + SmackConfiguration.getPacketReplyTimeout());
			
			XMPPConnection connection;
			try {
				connection = new XMPPConnection("127.0.0.1", 21020);
				
				connection.addPacketListener(this, null);
				
				connection.login(account.getOwner().getId(), "foo");
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new Error("Could not login as " + account.getOwner().getName().getFullName(), e);
			}
		
			System.out.println("Successfully logged in as " + account.getOwner().getName().getFullName());
			return connection;
		}

		public void processPacket(Packet packet) {
			System.out.println("Got a packet " + packet.toXML());
			if (connection != null && connection.isAuthenticated())
				connection.close();
			synchronized (this) {
				notifyAll();
			}
		}
	}
	
	public static void main(String[] args) {
		
		TestClient app = new TestClient();
	
		app.loadData();
		
		List<Thread> threads = app.spawnAccountTesters();
		
		System.out.println("main() waiting for threads to exit...");
		while (!threads.isEmpty()) {
			Thread t = threads.get(0);
			threads.remove(0);
			while (t.isAlive()) {
				try {
					t.join();
				} catch (InterruptedException e) {
					// nothing
				}
			}
		}
		
		System.out.println("main() exiting");
	}
}

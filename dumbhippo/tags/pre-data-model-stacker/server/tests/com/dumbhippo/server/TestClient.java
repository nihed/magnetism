/**
 * 
 */
package com.dumbhippo.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.dumbhippo.Digest;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.server.util.EJBUtil;

/**
 * App that hammers a server with remote session bean calls.
 * Creates test data, so should be run on an empty/trash database.
 * 
 * @author hp
 *
 */
public class TestClient {

	private TestGlueRemote test;
	
	public TestClient() {
		XMPPConnection.DEBUG_ENABLED = true;

		test = EJBUtil.defaultLookupRemote(TestGlueRemote.class);
		
		if (test == null)
			System.err.println("TestGlueRemote is null");
	}
	
	public void loadData() {
		test.loadTestData();
	}
	
	public List<Thread> spawnAccountTesters() {
		Set<Account> accounts = test.getActiveAccounts();

		System.out.print("Account IDs: ");
		for (Account a : accounts) {
			System.out.print(a.getId() + " ");
		}
		System.out.print("\n");
		
		List<Thread> threads = new ArrayList<Thread>();
		
		Iterator<Account> i = accounts.iterator();
		while (i.hasNext()) {
			Account first = i.next();
			if (!i.hasNext())
				break; // just ignore the odd one
			Account second = i.next();
			
			Runnable tester = new AccountTester(first, second);
			Thread thread = new Thread(tester);
			threads.add(thread);
			thread.start();
			tester = new AccountTester(second, first);
			thread = new Thread(tester);
			threads.add(thread);
			thread.start();
			
			// when we're running the GUI, let's not launch a ton 
			// of connections
			if (XMPPConnection.DEBUG_ENABLED)
				break;
			
			// keep it sane
			if (threads.size() > 10)
				break;
		}
		
		return threads;
	}
	
	public class AccountTester implements Runnable, PacketListener {
		
		private Account account;
		private Account friend;
		private String authCookie;
		private XMPPConnection connection;
		private boolean authorized = false;
		
		public AccountTester(Account account, Account friend) {
			this.account = account;
			this.friend = friend;
			
			System.out.println("Spawning tester for account " +
					account.getOwner().getId() + " and friend "
					+ friend.getOwner().getId());
			
			authCookie = test.authorizeNewClient(account.getId(), "TestClient");
			
			System.out.println("   got authCookie = " + authCookie);
		}
		
		public void run() {
			connection = loginToJabber();
			
			Message message = new Message();
			
			message.setTo(friend.getOwner().getId() + "@mugshot.org");
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
				
				// FIXME change this whole mess to use PacketCollector
				
				connection.addPacketListener(this, null);
			
				// Ask what is supported, though we don't care, 
				// the server seems to like it
		        Authentication askAuth = new Authentication();
		        askAuth.setType(IQ.Type.GET);
		        askAuth.setUsername(account.getOwner().getId());
				
				connection.sendPacket(askAuth);
				
				// FIXME wait for reply here...
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
				
				// Hardcode use of digest auth; we only work with our own server
				
				Authentication auth = new Authentication();
				auth.setUsername(account.getOwner().getId());
				
				auth.setDigest(Digest.computeDigest(connection.getConnectionID(), authCookie));
				
				auth.setResource("Smack"); // not sure if we need to match the rest of smack on this
				
				connection.sendPacket(auth);
		
				while (!authorized && connection.isConnected()) {
					try {
						synchronized (this) {
							wait();
						}
					} catch (InterruptedException e) {
						// ignore
					}
				}				
				
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new Error("Could not login as " + account.getOwner().getNickname(), e);
			}
		
			System.out.println("Successfully sent login as " + account.getOwner().getNickname());
			return connection;
		}

		public void processPacket(Packet packet) {
			System.out.println("Got a packet " + packet.toXML());
			
			if (packet instanceof IQ) {
				IQ iq = (IQ) packet;
				if (iq.getType() == IQ.Type.ERROR)
					System.err.println(iq.getError());
				
				if (iq instanceof Authentication &&
						iq.getType() == IQ.Type.RESULT) {
					System.out.println("We're probably authorized now");
					authorized = true;
				}
			}
			
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

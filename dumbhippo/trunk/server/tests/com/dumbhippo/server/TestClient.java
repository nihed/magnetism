/**
 * 
 */
package com.dumbhippo.server;

import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

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
	
	public void spawnAccountTesters() {
		HippoAccount account = test.getAnAccount();
		System.out.println("Got single account " + account.getOwner().getId());

		Set<HippoAccount> accounts = test.getActiveAccounts();

		Iterator<HippoAccount> i = accounts.iterator();
		while (i.hasNext()) {
			HippoAccount first = i.next();
			if (!i.hasNext())
				break; // just ignore the odd one
			HippoAccount second = i.next();


			Runnable tester = new AccountTester(first, second);
			Thread thread = new Thread(tester);
			thread.run();
			tester = new AccountTester(second, first);
			thread = new Thread(tester);
			thread.run();
		}
	}
	
	public class AccountTester implements Runnable {
		
		private HippoAccount account;
		private HippoAccount friend;
		
		public AccountTester(HippoAccount account, HippoAccount friend) {
			this.account = account;
			this.friend = friend;
			
			System.out.println("Spawning tester for account " +
					account.getOwner().getId() + " and friend "
					+ friend.getOwner().getId());
		}
		
		public void run() {
			XMPPConnection connection = loginToJabber();
			
			Message message = new Message();
			
			message.setTo(friend.getOwner().getId() + "@dumbhippo.com");
			message.setBody("This is the message body");
			
			connection.sendPacket(message);
		}
		
		private XMPPConnection loginToJabber() {
			XMPPConnection connection;
			try {
				connection = new XMPPConnection("127.0.0.1", 21022);
				connection.login(account.getOwner().getId(), "");
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new Error("Could not login as " + account.getOwner().getName().getFullName(), e);
			}
		
			System.out.println("Successfully logged in as " + account.getOwner().getName().getFullName());
			return connection;
		}
	}
	
	public static void main(String[] args) {
		
		TestClient app = new TestClient();
	
		app.loadData();
		
		app.spawnAccountTesters();
	}
}

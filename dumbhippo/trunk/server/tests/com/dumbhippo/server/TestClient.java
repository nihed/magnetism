/**
 * 
 */
package com.dumbhippo.server;

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
		
		for (HippoAccount a : accounts) {
			System.out.println("Spawning thread for account " + a.getOwner().getId());
			Runnable tester = new AccountTester(a);
			Thread thread = new Thread(tester);
			thread.run();
		}
	}
	
	public class AccountTester implements Runnable {
		
		private HippoAccount account;
		
		public AccountTester(HippoAccount account) {
			this.account = account;
		}
		
		public void run() {
			XMPPConnection connection = loginToJabber();
			
			connection.sendPacket(new Message());
		}
		
		private XMPPConnection loginToJabber() {
			XMPPConnection connection;
			try {
				connection = new XMPPConnection("127.0.0.1:21022");
				connection.login(account.getOwner().getId(), "");
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new Error("Could not login as " + account.getOwner().getName().getFullName(), e);
			}
			
			return connection;
		}
		
	}
	
	public static void main(String[] args) {
		
		TestClient app = new TestClient();
	
		app.loadData();
		
		app.spawnAccountTesters();
	}
}

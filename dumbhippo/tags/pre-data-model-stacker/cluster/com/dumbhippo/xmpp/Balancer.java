package com.dumbhippo.xmpp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.XmlBuilder;

public class Balancer {
	
	private static String messageHost;
	
	private static class WriterThread extends Thread {
		private Socket socket;
		private String host;
		
		public WriterThread(Socket sock, String host) {
			this.socket = sock;
			this.host = host;
		}
		
		public void run() {
			try {
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				XmlBuilder builder = new XmlBuilder();
				builder.openElement("stream:stream", "from", messageHost, 
						            "xmlns", "jabber:client", "xmlns:stream", "http://etherx.jabber.org/streams", "version", "1.0");
				builder.openElement("stream:error");
				builder.appendTextNode("see-other-host", host, "xmlns", "urn:ietf:params:xml:ns:xmpp-streams");
				out.write(builder.toString());
				out.flush();
				socket.close();
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			}
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		List<String> hosts = new ArrayList<String>();
		System.out.print("Hosts: ");
		for (int i = 2; i < args.length; i++) {
			hosts.add(args[i]);
			System.out.print(args[i] + " ");
		}
		System.out.println();
		
		messageHost = args[0];
		
		ServerSocket socket = new ServerSocket(Integer.parseInt(args[1]));
		int currentHost = 0;
		while (true) {
			WriterThread wt = new WriterThread(socket.accept(), hosts.get(currentHost));
			wt.start();
			currentHost++;
			if (currentHost >= hosts.size())
				currentHost = 0;
		}
	}
}
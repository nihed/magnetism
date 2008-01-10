package com.dumbhippo.xmpp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;

public class Balancer {
	
	private static String messageHost;
	private static InetAddress bindInterface;
	
	private static class WriterThread extends Thread {
		private Socket socket;
		private String host;
		private Guid id;
		
		public WriterThread(Socket sock, String host, Guid id) {
			this.socket = sock;
			this.host = host;
			this.id = id;
		}
		
		// Waits until we see the client's stream:stream to emit our response
		private static class SimpleStreamHandler extends DefaultHandler {
			private Runnable callback;
			SimpleStreamHandler(Runnable cb) {
				this.callback = cb;
			}
			
			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
			}

			@Override
			public void startDocument() throws SAXException {
				System.out.println("Start document");
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				System.out.println("startElement: uri: '" + uri + "' localName: '" + localName + "' qname: '" + qName + "'");
				if (qName.equals("stream:stream"))
					callback.run();
			}

			@Override
			public void error(SAXParseException e) throws SAXException {
			}
			
			@Override
			public void warning(SAXParseException e) throws SAXException {
			}				

			@Override
			public void fatalError(SAXParseException e) throws SAXException {
			}		
		}

		@Override
		public void run() {
			SAXParser parser;
			try {
				parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(new InputSource(socket.getInputStream()), new SimpleStreamHandler(new Runnable () {
					public void run() {
						try {
							PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
							XmlBuilder builder = new XmlBuilder();
							builder.openElement("stream:stream", "from", messageHost, "id", id.toString(), 
									"xmlns", "jabber:client", "xmlns:stream", "http://etherx.jabber.org/streams", "version", "1.0");
							builder.openElement("stream:error", "from", messageHost);
							builder.appendTextNode("see-other-host", host, "xmlns", "urn:ietf:params:xml:ns:xmpp-streams");
							out.write(builder.toString());
							out.flush();
							/* FIXME - this is needed since the loudmouth client library appears to
							 * not process any pending data when it receives a connection shutdown
							 * immediately following.  Thus it won't see our redirect.
							 */
							Thread.sleep(4000);
							socket.close();
						} catch (IOException e) {
							System.out.println("IOException: " + e.getMessage());
						} catch (InterruptedException e) {
						}					
					}
				}));
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		List<String> hosts = new ArrayList<String>();
		System.out.print("Hosts: ");
		for (int i = 3; i < args.length; i++) {
			hosts.add(args[i]);
			System.out.print(args[i] + " ");
		}
		System.out.println();
		
		bindInterface = args[0].length() > 0 ? InetAddress.getByName(args[0]) : null;
		messageHost = args[1];
			
		ServerSocket socket = new ServerSocket();
		socket.bind(new InetSocketAddress(bindInterface, Integer.parseInt(args[2])));
		int currentHost = 0;
		while (true) {
			WriterThread wt = new WriterThread(socket.accept(), hosts.get(currentHost), Guid.createNew());
			wt.start();
			currentHost++;
			if (currentHost >= hosts.size())
				currentHost = 0;
		}
	}
}
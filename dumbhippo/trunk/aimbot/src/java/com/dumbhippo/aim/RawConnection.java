/*------------------------------------------------------------------------------
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is levelonelabs.com code.
 * The Initial Developer of the Original Code is Level One Labs. Portions
 * created by the Initial Developer are Copyright (C) 2001 the Initial
 * Developer. All Rights Reserved.
 *
 *         Contributor(s):
 *             Scott Oster      (ostersc@alum.rpi.edu)
 *             Steve Zingelwicz (sez@po.cwru.edu)
 *             William Gorman   (willgorman@hotmail.com)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this
 * file only under the terms of either the GPL or the LGPL, and not to allow
 * others to use your version of this file under the terms of the NPL, indicate
 * your decision by deleting the provisions above and replace them with the
 * notice and other provisions required by the GPL or the LGPL. If you do not
 * delete the provisions above, a recipient may use your version of this file
 * under the terms of any one of the NPL, the GPL or the LGPL.
 *----------------------------------------------------------------------------*/

package com.dumbhippo.aim;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;

public class RawConnection {
	private static Log logger = GlobalSetup.getLog(RawConnection.class);
    
    // for TOC3 (using toc2_login)
    // private static final String REVISION = "\"TIC:\\Revision: 1.61 \" 160 US
    // \"\" \"\" 3 0 30303 -kentucky -utf8 94791632";
    private static final String REVISION = "\"TIC:TOC2\" 160";

    private String loginServer = "toc.oscar.aol.com";

    private int loginPort = 5190;

    private String authorizerServer = "login.oscar.aol.com";

    private int authorizerPort = 29999;
    
    // private final String ROAST = "Tic/Toc";

    private IOThreads io;
    
    private long lastMessageTimestamp;

    private ScreenName name;

    private String pass;

    private String info;
    
    private RawListener listener;
    
    private PermitDenyMode permitMode;
    
    private int warnAmount;
    
    public RawConnection(ScreenName name, String pass, String info, RawListener listener) {

    	setWarnAmount(0);
    	permitMode = PermitDenyMode.PERMIT_ALL;
    	
        this.name = name;
        this.pass = pass;
        this.info = info;
        this.listener = listener;
    }
    
    public void signOn() {
        // AOL likes to have a bunch of bogus IPs for some reason, so lets try
        // them all until one works
        InetAddress[] loginIPs = null;
        try {
            loginIPs = InetAddress.getAllByName(loginServer);
        } catch (UnknownHostException e) {
            signOff("unknown host");
            generateError("Signon err", e.getMessage());
            return;
        }

        for (int i = 0; i < loginIPs.length; i++) {
            try {
                logger.debug("Attempting to logon using IP:" + loginIPs[i]);
                // * Client connects to TOC
                io = new IOThreads(loginIPs[i], loginPort);
                logger.debug("Successfully connected using IP:" + loginIPs[i]);
                break;
            } catch (IOException e) {
                // try the next one
            }
        }

        if (io == null) {
            signOff("can't establish connection");
            generateError("Signon err", "Unable to establish connection to logon server.");
            return;
        }
        
        // From here on we're guaranteed to get an end frame from "io" when 
        // it closes.

        io.signOn(name, pass, info, authorizerServer, authorizerPort);
        if (io.getOnceSignedOn())
        	generateConnected();
    }
    
    /**
     * sign off
     * 
     * @param place (for debugging)
     */
    public void signOff(String place) {
    	if (io == null)
    		return;
    	
    	logger.debug("Trying to close IM (" + place + ").....");
    	io.close();
    	
    	logger.debug("*** AIM CLIENT SIGNED OFF.");
    }

    public void read() {
    	while (io != null) {
    		Frame frame = io.takeFrame();
    		fromAIM(frame);
    	}
    }
    
    public void addBuddy(ScreenName buddy, String group) {
        if (getOnline()) {
            String toBeSent = "toc2_new_buddies {g:" + group + "\nb:" + buddy + "\n}";
            frameSend(toBeSent + "\0");
        }
    }
    
    public void removeBuddy(ScreenName buddy, String group) {
        String toBeSent = "toc2_remove_buddy";
        frameSend(toBeSent + " " + buddy + " " + group + "\0");
    }

    public void sendWarning(ScreenName buddy) {
        logger.debug("Attempting to warn: " + buddy + ".");

        String work = "toc_evil ";
        work = work.concat(buddy.getNormalized());
        work = work.concat(" norm \0");
        frameSend(work);
    }
    
    private void sendPermitOrDeny(String buddyOrEmpty, boolean permit) {
    	String which = permit ? "permit": "deny";
        if (buddyOrEmpty.length() == 0) {
        	// this assumes we are in the "opposite mode"
            logger.debug("Attempting to " + which + " all.");
        } else {
            logger.debug("Attempting to deny: " + buddyOrEmpty + ".");
        }

        String toBeSent = "toc2_add_" + which;
        frameSend(toBeSent + " " + buddyOrEmpty + "\0");    	
    }
    
    public void sendDeny(ScreenName buddy) {       
        sendPermitOrDeny(buddy.getNormalized(), false);
    }
    
    /*
     * To do this we need to track whether we're in "permit mode" or 
     * "deny mode"
    public void sendPermitAll() {
    	sendPermitOrDeny("", false);
    }
    public void sendDenyAll() {
    	sendPermitOrDeny("", true);
    }
    */

    public void sendPermit(ScreenName buddy) {
        sendPermitOrDeny(buddy.getNormalized(), true);
    }

    public void sendMessage(ScreenName to, String html) {
        if (html.length() >= 1024) {
            html = html.substring(0, 1024);
        }
        logger.debug("Sending Message " + to + " > " + html);

        String work = "toc2_send_im ";
        work = work.concat(to.getNormalized());
        work = work.concat(" \"");
        for (int i = 0; i < html.length(); i++) {
            switch (html.charAt(i)) {
                case '$' :
                case '{' :
                case '}' :
                case '[' :
                case ']' :
                case '(' :
                case ')' :
                case '\"' :
                case '\\' :
                    work = work.concat("\\" + html.charAt(i));
                    break;
                default :
                    work = work.concat("" + html.charAt(i));
                    break;
            }
        }

        work = work.concat("\"\0");
        frameSend(work);
    }
    
    public void sendGetStatus(ScreenName buddy) {
    	frameSend("toc_get_status " + buddy + "\0");
    }
    
    private void sendSetAway(String reason) {
        String work = "toc_set_away \"" + reason + "\"\0";

        frameSend(work);
    }

    public void sendUnvailable(String reason) {
    	if (reason == null || reason.length() == 0) {
    		reason = "Away";
    	}
    	sendSetAway(reason);
    }
    
    public void sendAvailable() {
    	sendSetAway("");
    }    

    public void sendSetPermitMode(PermitDenyMode mode) {
    	logger.info("Setting permit mode to:" + mode);
    	permitMode = mode;
    	frameSend("toc2_set_pdmode " + permitMode.getProtocolValue() + "\0");
    }
    
	public ScreenName getName() {
		return name;
	}
	
	public long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}
    
    /** 
     * @returns true if we are authenticated and connected
     */
    public boolean getOnline() {
    	return io != null && io.isOpen();
    }
    
    /**
     * Gets the permit mode that is set on the server.
     * 
     * @return current mode
     */
    public PermitDenyMode getPermitMode() {
    	return permitMode;
    }
    
    private void setWarnAmount(int warnAmount) {
    	this.warnAmount = warnAmount;
    	if (io != null)
    		io.setWarnAmount(this.warnAmount);
    }
    
    private void frameSend(String toBeSent) {
    	if (io != null) {
    		io.putFrame(new Frame(toBeSent));
    	}
    }
    
    private void fromAIM(Frame frame) {
    	if (frame.isEndFrame()) {
    		boolean haveGeneratedConnected = false;
    		if (io.getOnceSignedOn())
    			haveGeneratedConnected = true;
    		
    		io = null;
    		
    		if (haveGeneratedConnected)
    			generateDisconnected();
    		return;
    	}
        try {
            String inString = frame.getAsString();

            logger.debug("*** AIM: " + inString + " ***");
            StringTokenizer inToken = new StringTokenizer(inString, ":");
            String command = inToken.nextToken();
            
            if (command.equals("IM_IN2")) {
            	command_IM_IN2(inToken);
            } else if (command.equals("CONFIG2")) {
                command_CONFIG2(inToken);
            } else if (command.equals("EVILED")) {
                command_EVILED(inToken);
            } else if (command.equals("UPDATE_BUDDY2")) {
            	command_UPDATE_BUDDY2(inToken);
            } else if (command.equals("ERROR")) {
            	command_ERROR(inToken);
            }
        } catch (Exception e) {
            logger.error("ERROR: failed to handle aim protocol properly");
            e.printStackTrace();
        }
    }

    private void command_IM_IN2(StringTokenizer inToken) {
        ScreenName from = new ScreenName(inToken.nextToken());
        // whats this?
        inToken.nextToken();
        // whats this?
        inToken.nextToken();
        String mesg = inToken.nextToken();
        while (inToken.hasMoreTokens()) {
            mesg = mesg + ":" + inToken.nextToken();
        }

        lastMessageTimestamp = System.currentTimeMillis();
        
        logger.debug("*** AIM MESSAGE: " + from + " > " + mesg + " ***");

        generateMessage(from, mesg);    	
    }
    
    private void command_CONFIG2(StringTokenizer inToken) {
        if (inToken.hasMoreElements()) {
            String config = inToken.nextToken();
            while (inToken.hasMoreTokens()) {
                config = config + ":" + inToken.nextToken();
            }
            processConfig(config);
            logger.debug("*** AIM CONFIG RECEIVED ***");
        } else {
            permitMode = PermitDenyMode.PERMIT_ALL;
            logger.debug("*** AIM NO CONFIG RECEIVED ***");
        }
    }
    
    private void command_EVILED(StringTokenizer inToken) {
        int amount = Integer.parseInt(inToken.nextToken());
        ScreenName from;
        if (inToken.hasMoreElements()) {
            from = new ScreenName(inToken.nextToken());
        } else {
        	from = new ScreenName("anonymous");
        }
        generateSetEvilAmount(from, amount);
        setWarnAmount(amount);
    }
    
    private void command_UPDATE_BUDDY2(StringTokenizer inToken) {
        ScreenName buddy = new ScreenName(inToken.nextToken());
        String stat = inToken.nextToken();
        if (stat.equals("T")) {
            generateBuddySignOn(buddy, "INFO");
            // logger.debug("Buddy:" + name + " just signed on.");
        } else if (stat.equals("F")) {
            generateBuddySignOff(buddy, "INFO");
            // logger.debug("Buddy:" + name + " just signed off.");
        }
        
        int evilAmount = Integer.parseInt(inToken.nextToken());
        generateSetEvilAmount(new ScreenName("anonymous"), evilAmount);
        setWarnAmount(evilAmount);
        
        if (stat.equals("T")) { // See whether user is available.
            @SuppressWarnings("unused") String signOnTime = inToken.nextToken();

            // TODO: what is the format of this?
            // System.err.println(bname+" signon="+signOnTime);
            @SuppressWarnings("unused") String idleTime = inToken.nextToken();
            // System.err.println(bname+"
            // idle="+Integer.valueOf(idleTime).intValue()+" mins");
            if (-1 != inToken.nextToken().indexOf('U')) {
                generateBuddyUnavailable(buddy, "INFO");
            } else {
                generateBuddyAvailable(buddy, "INFO");
            }
        }
    }
    
    private void command_ERROR(StringTokenizer inToken) {
        String error = inToken.nextToken();
        logger.error("*** AIM ERROR: " + error + " ***");
        if (error.equals("901")) {
            generateError(error, "Not currently available");
            // logger.debug("Not currently available");
            return;
        }

        if (error.equals("902")) {
            generateError(error, "Warning not currently available");
            // logger.debug("Warning not currently available");
            return;
        }

        if (error.equals("903")) {
            generateError(error, "Message dropped, sending too fast");
            // logger.debug("Message dropped, sending too fast");
            return;
        }

        if (error.equals("960")) {
            String person = inToken.nextToken();
            generateError(error, "Sending messages too fast to " + person);
            // logger.debug("Sending messages too fast to " + person);
            return;
        }

        if (error.equals("961")) {
            String person = inToken.nextToken();
            generateError(error, person + " sent you too big a message");
            // logger.debug(person + " sent you too big a message");
            return;
        }

        if (error.equals("962")) {
            String person = inToken.nextToken();
            generateError(error, person + " sent you a message too fast");
            // logger.debug(person + " sent you a message too fast");
            return;
        }

        if (error.equals("983")) {
        	generateError(error, "You have been connecting and "
+ "disconnecting too frequently.  Wait 10 minutes and try again. "
+ "If you continue to try, you will need to wait even longer.");
        	return;
        }
        
        if (error.equals("Signon err")) {
            String text = inToken.nextToken();
            generateError(error, "AIM Signon failure: " + text);

            // logger.debug("AIM Signon failure: " + text);
            signOff("Signon err");
        }
    }
        
    private void processConfig(String config) {
        PermitDenyMode newPermitMode = PermitDenyMode.PERMIT_ALL;
        BufferedReader br = new BufferedReader(new StringReader(config));
        try {
            String current_group = Buddy.DEFAULT_GROUP;
            String line;
            while (null != (line = br.readLine())) {
                if (line.equals("done:")) {
                    break;
                }
                char type = line.charAt(0);
                String arg = line.substring(2);
                switch (type) {
                    case 'g' :
                        current_group = arg;
                        break;
                    case 'b' :
                        //trim out the alias if there is one
                        int ind = arg.indexOf(":");
                        if (ind > -1) {
                            arg = arg.substring(0,ind);
                        }
                        
                        ScreenName name = new ScreenName(arg);
                        generateUpdateBuddy(name, current_group);
                        break;
                    case 'p' :
                    	generateAddPermitted(new ScreenName(arg));
                        break;
                    case 'd' :
                    	generateAddDenied(new ScreenName(arg));
                        break;
                    case 'm' :
                        newPermitMode = PermitDenyMode.parseInt(arg);
                        break;
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading configuration.");
            signOff("IOException in processing config");
            return;
        }

        permitMode = newPermitMode;
    }
    
    private void generateAddDenied(ScreenName name) {
    	if (listener != null)
    		listener.handleAddDenied(name);
	}

	private void generateAddPermitted(ScreenName name) {
		if (listener != null)
			listener.handleAddPermitted(name);
	}

	private void generateUpdateBuddy(ScreenName name, String group) {
		if (listener != null)
			listener.handleUpdateBuddy(name, group);
	}

	private void generateError(String error, String message) {
    	if (listener != null)
    		listener.handleError(error, message);
    }
    
    private void generateConnected() {
    	if (listener != null)
    		listener.handleConnected();
    }
    
    private void generateDisconnected() {
    	if (listener != null)
    		listener.handleDisconnected();
    }
    
    private void generateMessage(ScreenName buddy, String htmlMessage) {
    	if (listener != null)
    		listener.handleMessage(buddy, htmlMessage);
    }
    
    private void generateSetEvilAmount(ScreenName whoEviledUs, int amount) {
    	if (listener != null)
    		listener.handleSetEvilAmount(whoEviledUs, amount);
    }
    
    private void generateBuddySignOn(ScreenName buddy, String htmlInfo) {
    	if (listener != null)
    		listener.handleBuddySignOn(buddy, htmlInfo);
    }
    
    private void generateBuddySignOff(ScreenName buddy, String htmlInfo) {
    	if (listener != null)
    		listener.handleBuddySignOff(buddy, htmlInfo);
    }
    
    private void generateBuddyAvailable(ScreenName buddy, String htmlMessage) {
    	if (listener != null)
    		listener.handleBuddyAvailable(buddy, htmlMessage);
    }
    
    private void generateBuddyUnavailable(ScreenName buddy, String htmlMessage) {
    	if (listener != null)
    		listener.handleBuddyUnavailable(buddy, htmlMessage);
    } 
    
    /**
     * Protocol method
     * 
     * @para * *
     * @return roasted string
     */
    private static String imRoast(String pass) {
        String roast = "Tic/Toc";
        String out = "";
        String in = pass;
        String out2 = "0x";
        for (int i = 0; i < in.length(); i++) {
            out = java.lang.Long.toHexString(in.charAt(i) ^ roast.charAt(i % 7));
            if (out.length() < 2) {
                out2 = out2 + "0";
            }

            out2 = out2 + out;
        }

        return out2;
    }
    
    private static int toc2MagicNumber(ScreenName buddy, String password) {
    	String username = buddy.getNormalized();
        int sn = username.charAt(0) - 96;
        int pw = password.charAt(0) - 96;

        int a = sn * 7696 + 738816;
        int b = sn * 746512;
        int c = pw * a;

        return c - a + b + 71665152;
    }
    
    private static class Frame {
    	private String str;
    	
    	Frame() {
    		str = null;
    	}
    	
    	Frame(byte[] bytes) {
    		str = new String(bytes);
    	}
    	
    	Frame(String str) {
    		this.str = str; 
    	}
    	
    	boolean isEndFrame() {
    		return str == null;
    	}
    	
    	byte[] getBytes() {
    		return str.getBytes();
    	}
    	
    	String getAsString() {
    		return str;
    	}
    }
    
    private static class IOThreads {
        
        // rate limiting
        private static final int MAX_POINTS = 10;

        private static final int RECOVER_RATE = 2200;    	
    	
        private Socket connection;

        private DataInputStream in;

        private DataOutputStream out;

        private int seqNo;

        private int sendLimit;
        
        private long lastFrameSendTime;
        
        private int warnAmount;
        
        // did we ever authenticate?
        private boolean onceSignedOn; 
        
        private LinkedBlockingQueue<Frame> inQueue;
        
        private LinkedBlockingQueue<Frame> outQueue;
        
        private Thread inThread;
        
        private Thread outThread;
        
        IOThreads(InetAddress address, int port) throws IOException {
            connection = new Socket(address, port);
            connection.setSoTimeout(10000);
            in = new DataInputStream(connection.getInputStream());
            out = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
            
            sendLimit = MAX_POINTS;
        	lastFrameSendTime = System.currentTimeMillis();
            seqNo = (int) Math.floor(Math.random() * 65535.0);
            
            inQueue = new LinkedBlockingQueue<Frame>();
            outQueue = new LinkedBlockingQueue<Frame>();
        }

        void setWarnAmount(int warnAmount) {
        	this.warnAmount = warnAmount;
        }
        
        boolean isOpen() {
        	// this connection looks open until the queues are empty
        	// and nothing more could be read into them
        	return !(connection.isClosed() && inQueue.isEmpty());
        }
        
        boolean getOnceSignedOn() {
        	return onceSignedOn;
        }
        
        Frame takeFrame() {
        	Frame frame = null;
        	while (frame == null && isOpen()) {
        		try {
        			frame = inQueue.take();
        		} catch (InterruptedException e) {
        		}
        	}
			if (frame != null && frame.isEndFrame())
				close();
        	return frame;
        }
        
        void putFrame(Frame frame) {
        	if (frame == null)
        		throw new IllegalArgumentException("null frame");
        	queuePut(outQueue, frame);
        }
        
        /**
         * There are three ways this can be called. In scenario 1, we get an IO error
         * in the input thread in frameRead(), which adds an end frame to the 
         * incoming queue. We then call close() from takeFrame().
         * In scenario 2, we close the connection on our side by calling this 
         * directly.  In scenario 3, we call this during signOn().
         */
        void close() {
        	if (Thread.currentThread() == inThread)
        		throw new IllegalStateException("close() called from input thread");
        	if (Thread.currentThread() == outThread)
        		throw new IllegalStateException("close() called from output thread");

        	try {
        		// don't set these to null, it creates a big synchronization
        		// problem since the child threads use them
        		out.close();
        		in.close();
        		connection.close();
        	} catch (IOException e) {
        		// not much we can do...
        	}

        	// note, threads may not be created yet if still signing on
        	
        	while (inThread != null) {            	
        		// frameRead() called from the inThread 
        		// will return an end frame to the incoming queue

        		try {
        			inThread.join();
        			inThread = null;
        		} catch (InterruptedException e) {
        		}
        	}

        	if (outThread != null)
            	// send outgoing end frame, to exit outThread
        		putFrame(new Frame());

        	while (outThread != null) {
        		try {
        			outThread.join();
        			outThread = null;
        		} catch (InterruptedException e) {
        		}
        	}
    	}
        
        private static void queuePut(LinkedBlockingQueue<Frame> queue, Frame frame) {
        	while (true) {
        		try {
        			queue.put(frame);
        			break;
        		} catch (InterruptedException e) {
        			
        		}
        	}
        }
        
        private void writeFrame(String toBeSent) throws IOException {
        	out.writeByte(42); // *
        	out.writeByte(2); // DATA
        	out.writeShort(seqNo); // SEQ NO
        	seqNo = (seqNo + 1) & 65535;
        	out.writeShort(toBeSent.length()); // DATA SIZE
        	out.writeBytes(toBeSent); // DATA
        	out.flush();
        }
        
        private void frameSend(Frame frame) {
        	String toBeSent = frame.getAsString();
            if (sendLimit < MAX_POINTS) {
                sendLimit += ((System.currentTimeMillis() - lastFrameSendTime) / RECOVER_RATE);
                // never let the limit exceed the max, else this code won't work
                // right
                sendLimit = Math.min(MAX_POINTS, sendLimit);
                if (sendLimit < MAX_POINTS) {
                    // sendLimit could be less than 0, this still works properly
                    logger.debug("Current send limit=" + sendLimit + " out of " + MAX_POINTS);
                    try {
                        // this will wait for every point below the max
                        int waitAmount = MAX_POINTS - sendLimit;
                        logger.debug("Delaying send " + waitAmount + " units");
                        Thread.sleep(RECOVER_RATE * waitAmount);
                        sendLimit += waitAmount;
                    } catch (InterruptedException ie) {
                    }
                }
            }
            try {
            	writeFrame(toBeSent);
            } catch (IOException e) {
            	// main thread should notice and close()
            }

            // sending is more expensive the higher our warning level
            // this should decrement between 1 and 10 points (exponentially)
            sendLimit -= (1 + Math.pow((3 * warnAmount) / 100, 2));
            lastFrameSendTime = System.currentTimeMillis();
        }
        
        private Frame frameRead() {
        	try {
            	int length;
            	byte[] data;

        		synchronized (in) {
        			in.skip(4);
        			length = in.readShort();
        			data = new byte[length];
        			in.readFully(data);
        		}
        		Frame f = new Frame(data);
        		return f;
        	} catch (InterruptedIOException e) {
        		// This is normal; read times out when we dont read anything.
        		// logger.debug("*** AIM ERROR: " + e + " ***");
        		return null;
        	} catch (IOException e) {
        		return new Frame(); // end frame
        	}
        }

        private void createThreads() {
        	if (inThread != null || outThread != null)
        		throw new IllegalStateException("threads already exist");
        	
        	inThread = new Thread(new Runnable() {

				public void run() {
					// stuff input queue until we get the end frame
					while (true) {
						Frame frame = frameRead();
						if (frame != null) {
							queuePut(inQueue, frame);
							if (frame.isEndFrame())
								return;
						}
					}
				}
        		
        	});
        	
        	outThread = new Thread(new Runnable() {

				public void run() {
					// write output queue until we get the end frame
					while (true) {
						try {
							Frame frame = outQueue.take();
							if (frame != null) {
								if (frame.isEndFrame())
									return;
								// if connection is closed, this no-ops
								frameSend(frame);
							}
						} catch (InterruptedException e) {
						}
					}
				}
        		
        	});

        	inThread.setDaemon(true);
        	outThread.setDaemon(true);
        }
        
        public void signOn(ScreenName name, String pass, String info,
        		String authorizerServer, int authorizerPort) {
            logger.debug("*** Starting AIM CLIENT (SEQNO:" + seqNo + ") ***");
            try {
                int length;
            	
                // * Client sends "FLAPON\r\n\r\n"
                out.writeBytes("FLAPON\r\n\r\n");
                out.flush();
                // 6 byte header, plus 4 FLAP version (1)
                byte[] signon = new byte[10];
                // * TOC sends Client FLAP SIGNON
                in.readFully(signon);
                // * Client sends TOC FLAP SIGNON
                out.writeByte(42);// *
                out.writeByte(1); // SIGNON TYPE
                out.writeShort(seqNo); // SEQ NO
                seqNo = (seqNo + 1) & 65535;
                String normalName = name.getNormalized();
                out.writeShort(normalName.length() + 8); // data length = username length
                // + SIGNON DATA
                out.writeInt(1); // FLAP VERSION
                out.writeShort(1); // TLF TAG
                out.writeShort(normalName.length()); // username length
                out.writeBytes(normalName); // usename
                out.flush();

                // * Client sends TOC "toc_signon" message
                String signonCommand = "toc2_signon " + authorizerServer + " " + authorizerPort + " " + name + " " + imRoast(pass)
                    + " English " + REVISION + " " + toc2MagicNumber(name, pass) + "\0";
                writeFrame(signonCommand);
                
                // * if login fails TOC drops client's connection
                // else TOC sends client SIGN_ON reply
                in.skip(4); // seq num
                length = in.readShort(); // data length
                signon = new byte[length];
                in.readFully(signon); // data
                if (new String(signon).startsWith("ERROR")) {
                	queuePut(inQueue, new Frame(signon));
                    logger.error("Signon error");
                    queuePut(inQueue, new Frame()); // end frame
                    return;
                }

                in.skip(4); // seq num
                length = in.readShort(); // data length
                signon = new byte[length];
                in.readFully(signon); // data
                // * Client sends TOC toc_init_done message
                writeFrame("toc_init_done\0");
                onceSignedOn = true;
                writeFrame("toc_set_info \"" + info + "\"\0");
                logger.debug("Done with AIM logon");
                connection.setSoTimeout(3000);

                createThreads();
                
            } catch (IOException e) {
            	queuePut(inQueue, new Frame()); // end frame
            }
        }
    }
}

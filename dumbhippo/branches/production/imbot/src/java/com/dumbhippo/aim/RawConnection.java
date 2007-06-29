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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

public class RawConnection {
	private static Logger logger = GlobalSetup.getLogger(RawConnection.class);
    
    // for TOC3 (using toc2_login)
    // private static final String REVISION = "\"TIC:\\Revision: 1.61 \" 160 US
    // \"\" \"\" 3 0 30303 -kentucky -utf8 94791632";
    private static final String REVISION = "\"TIC:TOC2\" 160";
    
    // Online docs of TOC protocol says "currently exchange should always be 4,
    // however this may change in the future".
    // Rumor has it that 4 is "private chats" and 5 is "public chats".
    // Needs to match exchange used in aim launch URL in web UI
    private static final int AIM_CHAT_ROOM_EXCHANGE = 5;
    
    // Number of milliseconds that a bot will wait around for someone else
    // to join an empty chatroom before it leaves itself.
    private static final long CHAT_ROOM_LONELY_BOT_TIMEOUT = 60000;

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
    
    /**
     * Map of chat room ID to instances of static inner class ChatRoomInfo that contains
     * chat room name, roster of screen names, and room idle times.
     */
    private Map<String,ChatRoomInfo> chatRoomInfoById;
   
    public RawConnection(ScreenName name, String pass, String info, RawListener listener) {

    	setWarnAmount(0);
    	permitMode = PermitDenyMode.PERMIT_ALL;
    	
        this.name = name;
        this.pass = pass;
        this.info = info;
        this.listener = listener;
        
        this.chatRoomInfoById = new HashMap<String,ChatRoomInfo>();
    }
    
    public void signOn() {
        // AOL likes to have a bunch of bogus IPs for some reason, so lets try
        // them all until one works
        InetAddress[] loginIPs = null;
        try {
            loginIPs = InetAddress.getAllByName(loginServer);
        } catch (UnknownHostException e) {
            signOff("unknown host");
            generateError(TocError.SIGNON_ERROR, e.getMessage());
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
            generateError(TocError.SIGNON_ERROR, "Unable to establish connection to logon server.");
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
    	
    	logger.debug("Trying to close at (" + place + ")");
    	io.close();
    	
    	logger.debug("SIGNED OFF");
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
    
    /*
     * toc_chat_join <Exchange> <Chat Room Name>
     */
    public void chatJoin(String chatRoomName) {
    	frameSend("toc_chat_join " + AIM_CHAT_ROOM_EXCHANGE + " " + chatRoomName + "\0");
    }
    
    /*
     * toc_chat_leave <Chat Room ID>
     */
    public void chatLeave(String chatRoomId) {
    	frameSend("toc_chat_leave " + chatRoomId +  "\0");
    }
    
    /*
     * toc_chat_accept <Chat Room ID>
     * accept a CHAT_INVITE.  server will respond with a CHAT_JOIN.
     */
    public void chatAccept(String chatRoomId) {
    	frameSend("toc_chat_accept " + chatRoomId + "\0");
    }

    /*
     * toc_chat_send <Chat Room ID> <Message>
     */
    
    public void chatSend(String chatRoomId, String html) {
    	if (html.length() >= 1024) {
    		html = html.substring(0, 1024);
    	}
    	logger.debug("Sending Chat Message " + chatRoomId + " > " + html);
    	
    	String work = "toc_chat_send ";
    	work = work.concat(chatRoomId);
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
    
    private String toDebugAscii(String str) {
    	StringBuilder sb = new StringBuilder(str.length());
    	for (int i = 0; i < str.length(); ++i) {
    		char c = str.charAt(i);
    		if (c > 31 && c < 127) {
    			sb.append(c);
    		} else if (c == '\n') {
    			sb.append("\\n");
    		} else if (c == '\t') {
    			sb.append("\\t");
    		} else if (c == '\r') {
    			sb.append("\\r");
    		} else if (c == '\0') {
    			sb.append("\\0");
    		} else {
    			sb.append("{0x" + Integer.toString(c, 16) + "}");
    		}
    	}
    	return sb.toString();
    }
    
    private void fromAIM(Frame frame) {
    	if (frame.isEndFrame()) {
    		logger.debug("fromAIM(): end frame");
    		
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

            logger.debug("Incoming frame is: '" + toDebugAscii(inString) + "'");
            StringTokenizer inToken = new StringTokenizer(inString, ":");
            String command = inToken.nextToken();
            
            logger.debug("fromAIM(): command = " + toDebugAscii(command));
            
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
            } else if (command.equals("NICK")) {
            	command_NICK(inToken);
            } else if (command.equals("CHAT_INVITE")) {
            	command_CHAT_INVITE(inToken);
            } else if (command.equals("CHAT_JOIN")) {
            	command_CHAT_JOIN(inToken);
            } else if (command.equals("CHAT_IN")) {
            	command_CHAT_IN(inToken);
            } else if (command.equals("CHAT_UPDATE_BUDDY")) {
            	command_CHAT_UPDATE_BUDDY(inToken);
            } else if (command.equals("CHAT_LEFT")) {
            	command_CHAT_LEFT(inToken);
            } else {
            	// log this but don't crash, probably it's harmless
            	logger.error("unknown AIM command '" + toDebugAscii(command) + "'");
            }
            
            // check if there are any bots alone in chat rooms for longer than
            //  the configured timeout
            checkForLonelyBots();
            
        } catch (Exception e) {
            logger.error("exception handling incoming command: ", e);
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
        
        logger.debug("IM: " + from + ": " + toDebugAscii(mesg));

        generateMessage(from, mesg);
    }
    
    private void command_CONFIG2(StringTokenizer inToken) {
        if (inToken.hasMoreElements()) {
            String config = inToken.nextToken();
            while (inToken.hasMoreTokens()) {
                config = config + ":" + inToken.nextToken();
            }
            processConfig(config);
            logger.debug("processed AIM config");
        } else {
            permitMode = PermitDenyMode.PERMIT_ALL;
            logger.debug("config command had no config");
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
            generateBuddySignOn(buddy, "");
            // logger.debug("Buddy:" + name + " just signed on.");
        } else if (stat.equals("F")) {
            generateBuddySignOff(buddy, "");
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
                generateBuddyUnavailable(buddy, "");
            } else {
                generateBuddyAvailable(buddy, "");
            }
        }
    }
    
    private void command_ERROR(StringTokenizer inToken) {
        String error = inToken.nextToken();
        String args = "";
        if (inToken.hasMoreTokens())
        	args = inToken.nextToken();
        
        logger.debug("AIM error: '" + error + "' args '" + args + "'");
        
        TocError tocError = TocError.parse(error);
        
        logger.debug(tocError.format(args));
        
        generateError(tocError, tocError.format(args));
        
        if (tocError == TocError.SIGNON_ERROR) {
            signOff("Signon err");
        }
    }
        
    private void command_NICK(StringTokenizer inToken) {
    	String nick = inToken.nextToken();
    	logger.debug("Got our nick = '" + nick + "'");
    	
    	ScreenName newName = new ScreenName(nick);
    	
    	// equals is defined using the normal form only
    	if (newName.equals(name)) {
    		name = newName;
    	} else {
    		logger.error("Strange, got a new nick with normal form " + newName.getNormalized()
    				+ " while we are " + name.getNormalized());
    	}
    }
    
    /*
     * CHAT_INVITE:<Chat Room Name>:<Chat Room Id>:<Invite Sender>:<Message>
     * Server is inviting us to a chat room.
     */
    // TODO: may want to disable this so that random people can't invite our bot to chat rooms,
    //  it is not needed for joins requested by our EJB server
    private void command_CHAT_INVITE(StringTokenizer inToken) {
    	String chatRoomName = inToken.nextToken();
    	String chatRoomId = inToken.nextToken();
    	String inviteSender = inToken.nextToken();
    	String message = inToken.nextToken();
    	
        logger.debug("CHAT_INVITE room_name=" + chatRoomName + 
        			 " room_id=" + chatRoomId +
        			 " inviteSender=" + inviteSender + 
        			 " message=" + message);
        
        // respond with a toc_chat_accept
        chatAccept(chatRoomId);
        
        // The server will send a CHAT_JOIN in response
	}
    
    /*
     * CHAT_JOIN:<Chat Room Id>:<Chat Room Name>
     * Succesfully joined a chat room, now we get the ID as well.
     */
    private void command_CHAT_JOIN(StringTokenizer inToken) {
    	String chatRoomId = inToken.nextToken();
    	String chatRoomName = inToken.nextToken();
    	
    	// add the mapping id->name to this connection's table of chat rooms
    	ChatRoomInfo chatRoomInfo = chatRoomInfoById.get(chatRoomId);
    	if (chatRoomInfo == null) {
    		chatRoomInfo = new ChatRoomInfo();
    	}
    	chatRoomInfo.setChatRoomName(chatRoomName);
    	chatRoomInfoById.put(chatRoomId, chatRoomInfo);
    	
        logger.debug("CHAT_JOIN room_id=" + chatRoomId + " room_name=" + chatRoomName);
    }
    
    /*
	 * CHAT_IN:<Chat Room Id>:<Source User>:<Whisper? T/F>:<Message>
	 * Someone sent a chat message in a room (possibly us).
	 */
    private void command_CHAT_IN(StringTokenizer inToken) {
    	String chatRoomId = inToken.nextToken();
    	ScreenName from = new ScreenName(inToken.nextToken());
    	String whisper = inToken.nextToken();
    	String mesg = inToken.nextToken();
    	while (inToken.hasMoreTokens()) {
             mesg = mesg + ":" + inToken.nextToken();
        }
   
        lastMessageTimestamp = System.currentTimeMillis();
        
        logger.debug("CHAT_IN room_id=" + chatRoomId +
        		 " from=" + from +
    			 " whisper=" + whisper + 
    			 " message=" + mesg);
        
        // get chat room name for this id
        ChatRoomInfo chatRoomInfo = chatRoomInfoById.get(chatRoomId);
        if (chatRoomInfo != null) {
        	String chatRoomName = chatRoomInfo.getChatRoomName();
        	generateChatMessage(from, chatRoomName, chatRoomId, mesg);
        } else {
        	logger.warn("Unknown chat room id received in CHAT_IN");
        }
    }
    
    /*
     * CHAT_UPDATE_BUDDY:<Chat Room Id>:<Inside? T/F>:<User 1>:<User 2>...
     * Someone arrived/departed a room, or initial room roster list.
     */
    private void command_CHAT_UPDATE_BUDDY(StringTokenizer inToken) {
    	String chatRoomId = inToken.nextToken();
    	String inside = inToken.nextToken();
    	
    	// get the current roster for the room from hashmap, or create a new one
    	
    	ChatRoomInfo chatRoomInfo = chatRoomInfoById.get(chatRoomId);
    	if (chatRoomInfo == null) {
    		chatRoomInfo = new ChatRoomInfo();
    		chatRoomInfoById.put(chatRoomId, chatRoomInfo);
    	}
    	List<String> roster = chatRoomInfo.getChatRoomRoster();
    	
    	if (roster == null) {
    		roster = new ArrayList<String>();
    		chatRoomInfo.setChatRoomRoster(roster);
    	}
    	
    	String userListStr = "";
    	while (inToken.hasMoreTokens()) {
    		String s = inToken.nextToken();
    		ScreenName sn = new ScreenName(s);
    		
    		userListStr = userListStr + s + ":";
    		// leave this bot off of roster actions
    		if (!sn.equals(name)) {
    			if (inside.equals("T")) {
    				if (!roster.contains(s)) {
    					roster.add(s);
    					logger.debug("Added " + s + " to roster for " + chatRoomId);
    				} else {
    					logger.debug("Keeping " + s + " on roster for " + chatRoomId);
    				}
    			} else { 
    				roster.remove(s);
    				logger.debug("Removed " + s + " from roster for " + chatRoomId);
    			}
    		}
        }
    	
        logger.debug("CHAT_UPDATE_BUDDY room_id=" + chatRoomId +
        		 " inside=" + inside +
    			 " user_list=" + userListStr);
        
        logger.debug("Current roster for room_id=" + chatRoomId + " is " + roster);
    	
        lastMessageTimestamp = System.currentTimeMillis();
        
        // relay room members (sans this bot) to dumbhippo server
        generateChatRoomRosterChange(chatRoomInfo.getChatRoomName(), chatRoomId, roster);
        
        // if this bot is the only user left in the room, and we've allowed
    	//  enough time for others to join, then depart the room
        if (roster.size() == 0) {
        	
        	Long roomJoinTimeObj = chatRoomInfo.getLonelyBotTime();
        	if (roomJoinTimeObj == null) {
        		// bot alone counter wasn't started, so start it now
        		chatRoomInfo.setLonelyBotTime(System.currentTimeMillis());
        	} else {
        		// otherwise we don't need to do anything, checkForLonelyBots will test next time
        		//  it is invoked
        	}
        } else {
        	// bot isn't alone in room currently, so remove from lonely bot list if it was on there
        	chatRoomInfo.setLonelyBotTime(null);
        }
    }

    /**
     * Iterate through the Map of bots alone in chat rooms, and tell the bots
     * to leave if they've been alone for longer than the set timeout.
     * 
     * Invoked in fromAIM() whenever we receive a message from the AIM service.
     */
    private void checkForLonelyBots() {
     	for (String chatRoomId: chatRoomInfoById.keySet()) {
     		// check each room with a lonely bot, and leave only if the bot has been
     		//  alone for more than the timeout period
     		ChatRoomInfo chatRoomInfo = chatRoomInfoById.get(chatRoomId);
     		
     		Long lonelyPeriodStartObj = chatRoomInfo.getLonelyBotTime();
     		long lonelyPeriodStart = System.currentTimeMillis();
     		if (lonelyPeriodStartObj != null) {
     			lonelyPeriodStart = lonelyPeriodStartObj.longValue();
     		}
     		long timeSinceLonelyPeriodStart = (System.currentTimeMillis() - lonelyPeriodStart);
     		if (timeSinceLonelyPeriodStart > CHAT_ROOM_LONELY_BOT_TIMEOUT) {
     			logger.debug("Room " + chatRoomId + " empty except this bot, time elapsed since got lonely " + timeSinceLonelyPeriodStart + ", so departing...");
     			chatLeave(chatRoomId);
     		} else {
     			logger.debug("Room " + chatRoomId + " empty except this bot, time elapsed since join " + timeSinceLonelyPeriodStart + ", so waiting around...");
     		}	
     	}
    }
    
    /*
     * CHAT_LEFT:<Chat Room Id>
     * We successfully left a chat room.
     */
    private void command_CHAT_LEFT(StringTokenizer inToken) {
      	String chatRoomId = inToken.nextToken();
      	logger.debug("CHAT_LEFT room_id=" + chatRoomId);
      	 
      	// remove the mapping id->name to this connection's table of chat rooms
      	chatRoomInfoById.remove(chatRoomId);
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

	private void generateError(TocError error, String message) {
    	if (listener != null)
    		listener.handleError(error, message);
    }
    
    private void generateConnected() {
    	logger.debug("generateConnected()");
    	if (listener != null)
    		listener.handleConnected();
    }
    
    private void generateDisconnected() {
    	logger.debug("generateDisconnected()");
    	if (listener != null)
    		listener.handleDisconnected();
    }
    
    private void generateMessage(ScreenName buddy, String htmlMessage) {
    	if (listener != null) {
    		try {
    			listener.handleMessage(buddy, htmlMessage);
    		} catch (FilterException e) {
    			
    		}
    	}
    }
    
    private void generateChatMessage(ScreenName buddy, String chatRoomName, String chatRoomId, String htmlMessage) {
    	if (listener != null) {
    		try {
    			listener.handleChatMessage(buddy, chatRoomName, chatRoomId, htmlMessage);
    		} catch (FilterException e) {
    			
    		}
    	}
    }
    
    private void generateChatRoomRosterChange(String chatRoomName, String chatRoomId, List<String> chatRoomRoster) {
    	if (listener != null) {
    		listener.handleChatRoomRosterChange(chatRoomName, chatRoomRoster);
    	}
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
    		// FIXME broken encoding, have to figure out TOC encoding handling
    		try {
				str = new String(bytes, "ISO-8859-1");
			} catch (UnsupportedEncodingException e) {
				logger.error("Unsupported encoding", e);
				throw new RuntimeException(e);
			}
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
            
            logger.debug("Successfully created new " + getClass().getName());
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
         * directly.  This can be from another thread from the bot thread e.g. the timer thread.
         * In scenario 3, we call this during signOn().
         */
        void close() {
        	// both of these could deadlock since we join these threads below 
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
        		logger.error("Failed to close socket: " + e.getMessage());
        	}

        	// only want one thread to do the shutdown of the io threads or there's 
        	// a race for the != null checks
        	logger.debug("trying to get lock to join io threads");
        	synchronized (this) {
            	// note, threads may not be created yet if still signing on.
	        	while (inThread != null) {            	
	        		// frameRead() called from the inThread 
	        		// will return an end frame to the incoming queue
	
	        		try {
	        			logger.debug("waiting to join inThread");
	        			inThread.join();
	        			inThread = null;
	        		} catch (InterruptedException e) {
	        		}
	        	}
	
	        	if (outThread != null) {
	            	logger.debug("sending outgoing end frame, to exit outThread");
	        		putFrame(new Frame());
	        	}
	
	        	while (outThread != null) {
	        		try {
	        			logger.debug("waiting to join outThread");
	        			outThread.join();
	        			outThread = null;
	        		} catch (InterruptedException e) {
	        		}
	        	}
	        	logger.debug("in/out threads successfully joined");
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
        	out.writeByte(2); // DATA frame type code
        	out.writeShort(seqNo); // SEQ NO
        	seqNo = (seqNo + 1) & 65535;
        	out.writeShort(toBeSent.length()); // DATA SIZE
        	out.writeBytes(toBeSent); // DATA
        	out.flush();
        }
        
        private void frameSend(Frame frame) {
        	logger.debug("frameSend() sendLimit = " + sendLimit + " warnAmount = " + warnAmount);
        	
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
            	logger.debug("IOException in frameSend(): " + e.getMessage());
            }

            // sending is more expensive the higher our warning level
            // this should decrement between 1 and 10 points (exponentially)
            sendLimit -= (1 + Math.pow((3 * warnAmount) / 100, 2));
            lastFrameSendTime = System.currentTimeMillis();
            
            logger.debug("frameSend() end new sendLimit = " + sendLimit + " send time " + lastFrameSendTime);
        }
        
        private Frame frameRead() {
        	try {
            	int length;
            	byte[] data = null;

        		synchronized (in) {
        			byte b = in.readByte();
        			if (b != '*') {
        				logger.error("Frame starts with " + b + " not with *?");
        			}
        			b = in.readByte();
        			short seq = in.readShort();
        			logger.debug("frame type = " + b + " seq no = " + seq);
        			
        			length = in.readShort();
        			boolean readData = false;
        			switch (b) {
        			case 1: // SIGNON
        				logger.warn("ignoring SIGNON, we probably got a PAUSE before it...");
        				break;
        			case 2: // DATA
        				readData = true;
        				break;
        			case 5:  // KEEP_ALIVE
        				// we get lots of these
        				break;
        			case 3: // ERROR (supposedly not used by TOC)
        			case 4: // SIGNOFF (supposedly not used by TOC)
        			default:
        				logger.warn("ignoring weird frame type");
        				break;
        			}
        			if (readData) {
            			data = new byte[length];
            			in.readFully(data);
        			} else {
        				in.skip(length);
        			}
        		}
        		if (data != null) {
        			return new Frame(data);
        		} else {
        			return null;
        		}
        	} catch (InterruptedIOException e) {
        		// This happens every connection.setSoTimeout() time period
        		//logger.debug("IO interrupted in frame read: " + e.getMessage());
        		return null;
        	} catch (IOException e) {
        		logger.debug("IOException reading frame: " + e.getMessage());
        		return new Frame(); // end frame
        	}
        }

        private void createThreads() {
        	if (inThread != null || outThread != null)
        		throw new IllegalStateException("threads already exist");
        	
        	inThread = new Thread(new Runnable() {

				public void run() {
					logger.debug("input thread starting up");
					
					// stuff input queue until we get the end frame
					while (true) {
						Frame frame = frameRead();
						if (frame != null) {
							logger.debug("input thread got an incoming frame");
							queuePut(inQueue, frame);
							if (frame.isEndFrame()) {
								logger.debug("input thread got end frame; exiting");
								return;
							}
				        	logger.debug("input thread going back for another frame...");
						}
					}
				}
        		
        	});
        	
        	outThread = new Thread(new Runnable() {

				public void run() {
					logger.debug("output thread starting up");
					
					// write output queue until we get the end frame
					while (true) {
						try {
							Frame frame = outQueue.take();
							if (frame != null) {
								if (frame.isEndFrame()) {
									logger.debug("output thread got end frame; exiting");
									return;
								}
								logger.debug("output thread got a frame to send");
								// if connection is closed, this no-ops
								frameSend(frame);
							}
						} catch (InterruptedException e) {
						}
					}
				}
        		
        	});

        	inThread.setDaemon(true);
        	inThread.setName("Reader-aim");
        	outThread.setDaemon(true);
        	outThread.setName("Writer-aim");
        	
        	inThread.start();
        	outThread.start();
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

                in.skip(4); // * + frametype + seq num
                length = in.readShort(); // data length
                signon = new byte[length];
                in.readFully(signon); // data
                // * Client sends TOC toc_init_done message
                writeFrame("toc_init_done\0");
                onceSignedOn = true;
                writeFrame("toc_set_info \"" + info + "\"\0");
                logger.debug("Done with AIM logon");

                createThreads();
                
            } catch (IOException e) {
            	queuePut(inQueue, new Frame()); // end frame
            }
        }
    }
    /**
     * Inner class ChatRoomInfo holds information about an active chat room.
     */
    private static class ChatRoomInfo {
    	private String chatRoomName;
    	private List<String> chatRoomRoster;
        /**
         * First timestamp when a bot noticed itself alone in a chat room, 
         * or null if the bot was not alone last time it checked.
         */
    	private Long lonelyBotTime;

		public String getChatRoomName() {
			return chatRoomName;
		}

		public void setChatRoomName(String chatRoomName) {
			this.chatRoomName = chatRoomName;
		}

		public List<String> getChatRoomRoster() {
			return chatRoomRoster;
		}

		public void setChatRoomRoster(List<String> chatRoomRoster) {
			this.chatRoomRoster = chatRoomRoster;
		}

		public Long getLonelyBotTime() {
			return lonelyBotTime;
		}

		public void setLonelyBotTime(Long lonelyBotTime) {
			this.lonelyBotTime = lonelyBotTime;
		}
    }    
}

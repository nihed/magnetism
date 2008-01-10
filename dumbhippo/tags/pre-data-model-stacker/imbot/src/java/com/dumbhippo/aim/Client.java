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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


/**
 * Implements the AIM protocol
 * 
 * @author Scott Oster, Will Gorman
 * @created September 4, 2001
 */
public class Client {
	private static Logger logger = GlobalSetup.getLogger(Client.class);
	
    private RawConnection connection;
    
    private List<Listener> aimListeners;
    private List<RawListener> rawListeners;

    private String nonUserResponse;

    private boolean autoAddUsers = false;

    private Map<ScreenName,Buddy> buddyHash; // must synchronize!

    private Set<ScreenName> permitted;

    private Set<ScreenName> denied;
    
    public Client(ScreenName name, String pass, String info, String response, boolean autoAddUsers) {
        this.nonUserResponse = response;
        
        aimListeners = new ArrayList<Listener>();
        rawListeners = new ArrayList<RawListener>();
        buddyHash = new HashMap<ScreenName,Buddy>(); // must synchronize on this to touch it
        permitted = new HashSet<ScreenName>();
        denied = new HashSet<ScreenName>();
        this.autoAddUsers = autoAddUsers;
        
        connection = new RawConnection(name, pass, info, new ClientListener());
        
        this.addBuddy(name);
    }

    public Client(ScreenName name, String pass, String info, boolean autoAddUsers) {
        this(name, pass, info, "Sorry, you must be a user of this system to send requests.", autoAddUsers);
    }

    public Client(ScreenName name, String pass, String info) {
        this(name, pass, info, false);
    }

    public Client(ScreenName name, String pass) {
        this(name, pass, "No info", false);
    }

    /**
     * Retrieve a buddy from the list
     * 
     * @param buddyName
     * @return The buddy
     */
    public Buddy getBuddy(ScreenName buddyName) {
    	synchronized (buddyHash) {
    		return buddyHash.get(buddyName);
    	}
    }
  
    /**
     * Get a list of all the current buddy names
     * 
     * @return list
     */
    public List<ScreenName> getBuddyNames() {
    	ScreenName[] arrayNames;
    	synchronized (buddyHash) {
    		Set<ScreenName> names = buddyHash.keySet();
    		arrayNames = names.toArray(new ScreenName[names.size()]);
    	}
    	                                      
        return Arrays.asList(arrayNames);
    }

    /**
     * Sign off from aim server
     */
    public void signOff() {
    	connection.signOff("User request");
    }

    /** 
     * Block until we're signed on or failed to sign on
     *
     */
    public void signOn() {
    	connection.signOn();
    }
    
    /**
     * Block until we have an incoming event
     */
    public void read() {
    	connection.read();
    }

    public void addListener(Listener listener) {
        aimListeners.add(listener);
    }

    public void addRawListener(RawListener listener) {
        rawListeners.add(listener);
    }
    
    public void sendMessageRaw(ScreenName buddy, String html) {
    	connection.sendMessage(buddy, html);
    }
    
   
    /**
     * This is often called from another thread
     * 
     * @param buddy Buddy to send to
     * @param chatRoomId ID of chat room, or null if IM and not chat room
     * @param html HTML String to send
     */
    public void sendMessage(Buddy buddy, String chatRoomId, String html) {
    	if (buddy == null)
    		throw new IllegalArgumentException("null buddy");
    	if (buddy.isBanned())
    		return;

        if (buddy.isOnline()) {
        	if (chatRoomId != null) {
        		connection.chatSend(chatRoomId, html);
        	} else {
        		connection.sendMessage(buddy.getName(), html);
        	}
        } else {
            // for some reason we are sending a message to an offline buddy,
        	// generate a status update
        	connection.sendGetStatus(buddy.getName());
        }
    }

    public Buddy addBuddy(ScreenName buddyName) {
        if (buddyName == null)
        	throw new IllegalArgumentException("null buddy name");

    	Buddy buddy; 
        synchronized (buddyHash) {
        	buddy = getBuddy(buddyName);
        	if (buddy != null)
        		return buddy;
        
        	buddy = new Buddy(buddyName);
        	buddyHash.put(buddy.getName(), buddy);
        }
        
        connection.addBuddy(buddy.getName(), buddy.getGroup());
        return buddy;
    }

    public void removeBuddy(Buddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        synchronized (buddyHash) {
        	if (getBuddy(buddy.getName()) == null) {
        		return;
        	}
        	// logger.debug("Removed buddy from hash");
        	buddyHash.remove(buddy.getName());
        }
        
    	connection.removeBuddy(buddy.getName(), buddy.getGroup());
    }

    /**
     * Warn a buddy
     * 
     * @param buddy
     */
    public void sendWarning(Buddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        logger.debug("Attempting to warn: " + buddy.getName() + ".");
        connection.sendWarning(buddy.getName());
    }

    public void banBuddy(Buddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        if (getBuddy(buddy.getName()) == null) {
            return;
        }
        
        buddy.setBanned(true);
        
        connection.sendDeny(buddy.getName());
    }

    public void joinRoom(String chatRoomName) {
    	connection.chatJoin(chatRoomName);
    }
    
    private void generateMessage(ScreenName from, String htmlMessage) {
        Buddy aimbud = getBuddy(from);
        if (aimbud == null) {
            if (autoAddUsers) {
                addBuddy(from);
                aimbud = getBuddy(from);
                aimbud.setOnline(true);
            } else {
                logger.info("MESSAGE FROM A NON BUDDY(" + from + ")");
                // only send a response if a non-empty one is configured
                if ((nonUserResponse != null) && !nonUserResponse.equals("")) {
                    connection.sendMessage(from, nonUserResponse);
                }
                return;
            }
        }

        if (aimbud.isBanned()) {
            logger.debug("Ignoring message from banned user (" + from + "):" + htmlMessage);
        } else {
        	for (Listener l : aimListeners) {
                try {
                	l.handleMessage(aimbud, htmlMessage);
                } catch (Exception e) {
                    logger.error("Failure handling message", e);
                }
            }
        }
    }

	private void generateChatRoomRosterChange(String chatRoomName, List<String> chatRoomRoster) {
		for (Listener l : aimListeners) {
            try {
            	l.handleChatRoomRosterChange(chatRoomName, chatRoomRoster);
            } catch (Exception e) {
                logger.error("Failure handling roster change", e);
            }
        }
	}


    private void generateChatMessage(ScreenName from, String chatRoomName, String chatRoomId, String htmlMessage) {
        Buddy aimbud = getBuddy(from);
        if (aimbud == null) {
            if (autoAddUsers) {
                addBuddy(from);
                aimbud.setOnline(true);
            } else {
                logger.info("MESSAGE FROM A NON BUDDY(" + from + ")");
                // only send a response if a non-empty one is configured
                if ((nonUserResponse != null) && !nonUserResponse.equals("")) {
                    connection.sendMessage(from, nonUserResponse);
                }
                return;
            }
        }

        if (aimbud.isBanned()) {
            logger.debug("Ignoring message from banned user (" + from + "):" + htmlMessage);
        } else {
        	for (Listener l : aimListeners) {
                try {
                	l.handleChatMessage(aimbud, chatRoomName, chatRoomId, htmlMessage);
                } catch (Exception e) {
                    logger.error("Failure handling message", e);
                }
            }
        }
    }
    
    private void generateWarning(ScreenName from, int amount) {
        Buddy aimbud = getBuddy(from);
        for (Listener l : aimListeners) {
            try {
            	l.handleWarning(aimbud, amount);
            } catch (Exception e) {
                logger.error("Failure handling warning", e);
            }
        }
    }

    private void generateConnected() {
    	for (Listener l : aimListeners) {
            try {
            	l.handleConnected();
            } catch (Exception e) {
                logger.error("Failure handling connected", e);
            }
        }
    }

    private void generateDisconnected() {
    	for (Listener l : aimListeners) {
            try {
            	l.handleDisconnected();
            } catch (Exception e) {
                logger.error("Failure handling disconnected", e);
            }
        }
    }

    private void generateError(TocError error, String message) {
    	for (Listener l : aimListeners) {
            try {
                l.handleError(error, message);
            } catch (Exception e) {
                logger.error("Failure handling error", e);
            }
        }
    }

    private void generateBuddySignOn(ScreenName buddy, String message) {
        Buddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.error("NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        if (!aimbud.isOnline()) {
            aimbud.setOnline(true);
            for (Listener l : aimListeners) {
                try {
                    l.handleBuddySignOn(aimbud, message);
                } catch (Exception e) {
                    logger.error("Failure handling buddy sign on", e);
                }
            }
        }
    }

    private void generateBuddySignOff(ScreenName buddy, String message) {
        Buddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.error("NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        // logger.debug("XML = \n" + aimbud.toXML());
        aimbud.setOnline(false);
        for (Listener l : aimListeners) {
            try {
            	l.handleBuddySignOff(aimbud, message);
            } catch (Exception e) {
                logger.error("Failure handling buddy sign off", e);
            }
        }
    }

    private void generateBuddyAvailable(ScreenName buddy, String message) {
        Buddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.error("NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }
        for (Listener l : aimListeners) {
            try {
            	l.handleBuddyAvailable(aimbud, message);
            } catch (Exception e) {
                logger.error("Failure handling buddy available", e);
            }
        }
    }

    private void generateBuddyUnavailable(ScreenName buddy, String message) {
        Buddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.error("NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        for (Listener l : aimListeners) {
            try {
            	l.handleBuddyUnavailable(aimbud, message);
            } catch (Exception e) {
                logger.error("Failure handling buddy unavailable", e);
            }
        }
    }

    /** 
     * @returns true if we are authenticated and connected
     */
    public boolean getOnline() {
    	return connection.getOnline();
    }
    
    /**
     * Add a buddy to the denied list
     * 
     * @param buddy
     */
    public void denyBuddy(Buddy buddy) {
        permitted.remove(buddy.getName());
        denied.add(buddy.getName());
        connection.sendDeny(buddy.getName());
    }


    /**
     * Add a buddy to the permitted list
     * 
     * @param buddy
     */
    public void permitBuddy(Buddy buddy) {
        denied.remove(buddy.getName());
        permitted.add(buddy.getName());
        connection.sendPermit(buddy.getName());
    }

    /**
     * Gets the permit mode that is set on the server.
     * 
     * @return current permit mode.
     */
    public PermitDenyMode getPermitMode() {
        return connection.getPermitMode();
    }

    /**
     * Sets the permit mode on the server.
     * 
     * @param mode
     */
    public void setPermitMode(PermitDenyMode mode) {
    	if (mode == PermitDenyMode.DENY_SOME && this.denied.size() == 0) {
            logger.info("Attempting to deny some, and none are denied, ignoring.");
            return;
        } else if (mode == PermitDenyMode.PERMIT_SOME && this.permitted.size() == 0) {
            logger.info("Attempting to permit some, and none are permitted, ignoring.");
            return;
        }

    	connection.sendSetPermitMode(mode);
    }

    /**
     * Clear unvailable message
     */
    public void setAvailable() {
    	connection.sendAvailable();
    }

    /**
     * Set unvailable message
     * 
     * @param reason why not available, if null or empty a default is used
     */
    public void setUnavailable(String reason) {
    	connection.sendUnvailable(reason);
    }

	public ScreenName getName() {
		return connection.getName();
	}
	
	public long getLastMessageTimestamp() {
		return connection.getLastMessageTimestamp();
	}
	
	private class ClientListener implements RawListener {

		/**
		 * Handles on-to-one IMs
		 */
		public void handleMessage(ScreenName buddy, String htmlMessage) {
			boolean filtered = false;
			for (RawListener l : rawListeners) {
				try {
					l.handleMessage(buddy, htmlMessage);
				} catch (FilterException e) {
					filtered = true;
					break;
				} catch (Exception e2) {
					logger.error("Failure handling message", e2);
				}
			}
			
			if (filtered) {
				logger.debug("--message was filtered out");
			} else {
				generateMessage(buddy, htmlMessage);
			}
		}

		/**
		 * Handles messages in chat rooms
		 */
		public void handleChatMessage(ScreenName buddy, String chatRoomName, String chatRoomId, String htmlMessage) {
			boolean filtered = false;
			for (RawListener l : rawListeners) {
				try {
					l.handleChatMessage(buddy, chatRoomName, chatRoomId, htmlMessage);
				} catch (FilterException e) {
					filtered = true;
					break;
				} catch (Exception e2) {
					logger.error("Failure handling chat message", e2);
				}
			}
			
			if (filtered) {
				logger.debug("--message was filtered out");
			} else {
				generateChatMessage(buddy, chatRoomName, chatRoomId, htmlMessage);
			}
		}
		
		public void handleChatRoomRosterChange(String chatRoomName, List<String> chatRoomRoster) {
			generateChatRoomRosterChange(chatRoomName, chatRoomRoster);
		}

		
		public void handleSetEvilAmount(ScreenName whoEviledUs, int amount) {
			for (RawListener l : rawListeners) {
				try {
					l.handleSetEvilAmount(whoEviledUs, amount);
				} catch (Exception e) {
					logger.error("Failure handling set evil", e);
				}
			}
			
            Buddy buddy = getBuddy(connection.getName());
            
            // if what we have is less than what the server just sent, its
            // a warning otherwise it was just a server decrement update
            
            boolean increased = buddy.getWarningAmount() < amount;
            
            buddy.setWarningAmount(amount);
            
			if (increased) {
                generateWarning(whoEviledUs, amount);
            }
		}

		public void handleBuddySignOn(ScreenName buddy, String htmlInfo) {
			for (RawListener l : rawListeners) {
				try {
					l.handleBuddySignOn(buddy, htmlInfo);
				} catch (Exception e) {
					logger.error("Failure handling buddy sign on", e);
				}
			}
			generateBuddySignOn(buddy, htmlInfo);
		}

		public void handleBuddySignOff(ScreenName buddy, String htmlInfo) {
			for (RawListener l : rawListeners) {
				try {
					l.handleBuddySignOff(buddy, htmlInfo);
				} catch (Exception e) {
					logger.error("Failure handling buddy sign off", e);
				}
			}
			generateBuddySignOff(buddy, htmlInfo);			
		}

		public void handleBuddyUnavailable(ScreenName buddy, String htmlMessage) {
			for (RawListener l : rawListeners) {
				try {
					l.handleBuddyUnavailable(buddy, htmlMessage);
				} catch (Exception e) {
					logger.error("Failure handling buddy unavailable", e);
				}
			}
			generateBuddyUnavailable(buddy, htmlMessage);			
			
		}

		public void handleBuddyAvailable(ScreenName buddy, String htmlMessage) {
			for (RawListener l : rawListeners) {
				try {
					l.handleBuddyAvailable(buddy, htmlMessage);
				} catch (Exception e) {
					logger.error("failure handling buddy available", e);
				}
			}
			generateBuddyAvailable(buddy, htmlMessage);			
		}

		public void handleConnected() {
			for (RawListener l : rawListeners) {
				try {
					l.handleConnected();
				} catch (Exception e) {
					logger.error("Failure handling connected", e);
				}
			}
			generateConnected();			
		}

		public void handleDisconnected() {
			for (RawListener l : rawListeners) {
				try {
					l.handleDisconnected();
				} catch (Exception e) {
					logger.error("Failure handling disconnected", e);
				}
			}
			generateDisconnected();
		}

		public void handleError(TocError error, String message) {
			for (RawListener l : rawListeners) {
				try {
					l.handleError(error, message);
				} catch (Exception e) {
					logger.error("Failure handling error", e);
				}
			}
			generateError(error, message);
		}

		public void handleUpdateBuddy(ScreenName name, String group) {
			for (RawListener l : rawListeners) {
				try {
					l.handleUpdateBuddy(name, group);
				} catch (Exception e) {
					logger.error("Failure handling update buddy", e);
				}
			}

			synchronized (buddyHash) {
				Buddy buddy = buddyHash.get(name);
				if (buddy == null) {
					buddy = new Buddy(name, group);
					buddyHash.put(name, buddy);
				} else {
					// they already exist, so just take the server's
					// word for the group they belong in
					buddy.setGroup(group);
				}
			}
		}

		public void handleAddPermitted(ScreenName buddy) {
			for (RawListener l : rawListeners) {
				try {
					l.handleAddPermitted(buddy);
				} catch (Exception e) {
					logger.error("Failure handling add permitted", e);
				}
			}
			permitted.add(buddy);
		}

		public void handleAddDenied(ScreenName buddy) {
			for (RawListener l : rawListeners) {
				try {
					l.handleAddDenied(buddy);
				} catch (Exception e) {
					logger.error("Failure handling add denied", e);
				}
			}
			denied.add(buddy);
		}
	}
}

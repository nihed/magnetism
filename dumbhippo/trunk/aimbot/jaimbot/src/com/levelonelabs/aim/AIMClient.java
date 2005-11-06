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

package com.levelonelabs.aim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * Implements the AIM protocol
 * 
 * @author Scott Oster, Will Gorman
 * @created September 4, 2001
 */
public class AIMClient implements AIMSender {
	
    private static Logger logger = Logger.getLogger(AIMClient.class.getName());

    private AIMRawConnection connection;
    
    private List<AIMListener> aimListeners;
    private List<AIMRawListener> rawListeners;

    private String nonUserResponse;

    private boolean autoAddUsers = false;

    private Map<String,AIMBuddy> buddyHash;

    private Set<String> permitted;

    private Set<String> denied;
    
    public AIMClient(String name, String pass, String info, String response, boolean autoAddUsers) {
        this.nonUserResponse = response;
        
        aimListeners = new ArrayList<AIMListener>();
        rawListeners = new ArrayList<AIMRawListener>();
        buddyHash = new HashMap<String,AIMBuddy>();
        permitted = new HashSet<String>();
        denied = new HashSet<String>();
        this.autoAddUsers = autoAddUsers;
        
        connection = new AIMRawConnection(AIMBuddy.normalize(name), pass, info,
        		new RawListener());
        
        this.addBuddy(new AIMBuddy(name));    	
    }

    public AIMClient(String name, String pass, String info, boolean autoAddUsers) {
        this(name, pass, info, "Sorry, you must be a user of this system to send requests.", autoAddUsers);
    }

    public AIMClient(String name, String pass, String info) {
        this(name, pass, info, false);
    }

    public AIMClient(String name, String pass) {
        this(name, pass, "No info", false);
    }

    /**
     * Retrieve a buddy from the list
     * 
     * @param buddyName
     * @return The buddy
     */
    public AIMBuddy getBuddy(String buddyName) {
        return buddyHash.get(AIMBuddy.normalize(buddyName));
    }

    /**
     * Get a list of all the current buddy names
     * 
     * @return list
     */
    public List<String> getBuddyNames() {
    	Set<String> names = buddyHash.keySet();
    	String[] arrayNames = names.toArray(new String[names.size()]);
    	                                      
        return Arrays.asList(arrayNames);
    }

    /**
     * Sign off from aim server
     */
    public void signOff() {
    	connection.signOff("User request");
    }

    /**
     * Main processing method for the AIMClient object
     */
    public void run() {
    	connection.signOn();
    	connection.read();
    }

    public void addListener(AIMListener listener) {
        aimListeners.add(listener);
    }

    public void addRawListener(AIMRawListener listener) {
        rawListeners.add(listener);
    }
    
    public void sendMessageRaw(String buddy, String html) {
    	connection.sendMessage(buddy, html);
    }
    
    public void sendMessage(AIMBuddy buddy, String html) {
    	if (buddy == null)
    		throw new IllegalArgumentException("null buddy");
    	if (buddy.isBanned())
    		return;

        if (buddy.isOnline()) {
        	connection.sendMessage(buddy.getName(), html);
        } else {
            // for some reason we are sending a message to an offline buddy,
        	// generate a status update
        	connection.sendGetStatus(buddy.getName());
        }
    }

    public void addBuddy(AIMBuddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        if (getBuddy(buddy.getName()) != null) {
            return;
        }

        connection.addBuddy(buddy.getName(), buddy.getGroup());
        
        // logger.fine("Added buddy to hash");
        buddyHash.put(AIMBuddy.normalize(buddy.getName()), buddy);
    }

    public void removeBuddy(AIMBuddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        if (getBuddy(buddy.getName()) == null) {
            return;
        }

        String buddyname = AIMBuddy.normalize(buddy.getName());

        connection.removeBuddy(buddyname, buddy.getGroup());
        
        // logger.fine("Removed buddy from hash");
        buddyHash.remove(AIMBuddy.normalize(buddy.getName()));
    }

    /**
     * Warn a buddy
     * 
     * @param buddy
     */
    public void sendWarning(AIMBuddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        logger.fine("Attempting to warn: " + buddy.getName() + ".");
        connection.sendWarning(AIMBuddy.normalize(buddy.getName()));
    }

    public void banBuddy(AIMBuddy buddy) {
        if (buddy == null)
        	throw new IllegalArgumentException("null buddy");

        if (getBuddy(buddy.getName()) == null) {
            return;
        }
        
        buddy.setBanned(true);
        
        connection.sendDeny(AIMBuddy.normalize(buddy.getName()));
    }

    private void generateMessage(String from, String htmlMessage) {
        AIMBuddy aimbud = getBuddy(from);
        if (aimbud == null) {
            if (autoAddUsers) {
                aimbud = new AIMBuddy(from);
                addBuddy(aimbud);
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
            logger.fine("Ignoring message from banned user (" + from + "):" + htmlMessage);
        } else {
        	for (AIMListener l : aimListeners) {
                try {
                	l.handleMessage(aimbud, htmlMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void generateWarning(String from, int amount) {
        AIMBuddy aimbud = getBuddy(from);
        for (AIMListener l : aimListeners) {
            try {
            	l.handleWarning(aimbud, amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateConnected() {
    	for (AIMListener l : aimListeners) {
            try {
            	l.handleConnected();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateDisconnected() {
    	for (AIMListener l : aimListeners) {
            try {
            	l.handleDisconnected();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateError(String error, String message) {
    	for (AIMListener l : aimListeners) {
            try {
                l.handleError(error, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateBuddySignOn(String buddy, String message) {
        AIMBuddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.severe("ERROR:  NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        if (!aimbud.isOnline()) {
            aimbud.setOnline(true);
            for (AIMListener l : aimListeners) {
                try {
                    l.handleBuddySignOn(aimbud, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void generateBuddySignOff(String buddy, String message) {
        AIMBuddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.severe("ERROR:  NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        // logger.fine("XML = \n" + aimbud.toXML());
        aimbud.setOnline(false);
        for (AIMListener l : aimListeners) {
            try {
            	l.handleBuddySignOff(aimbud, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateBuddyAvailable(String buddy, String message) {
        AIMBuddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.severe("ERROR:  NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }
        for (AIMListener l : aimListeners) {
            try {
            	l.handleBuddyAvailable(aimbud, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateBuddyUnavailable(String buddy, String message) {
        AIMBuddy aimbud = getBuddy(buddy);
        if (aimbud == null) {
            logger.severe("ERROR:  NOTIFICATION ABOUT NON BUDDY(" + buddy + ")");
            return;
        }

        for (AIMListener l : aimListeners) {
            try {
            	l.handleBuddyUnavailable(aimbud, message);
            } catch (Exception e) {
                e.printStackTrace();
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
    public void denyBuddy(AIMBuddy buddy) {
        String bname = AIMBuddy.normalize(buddy.getName());
        permitted.remove(bname);
        denied.add(bname);
        connection.sendDeny(bname);
    }


    /**
     * Add a buddy to the permitted list
     * 
     * @param buddy
     */
    public void permitBuddy(AIMBuddy buddy) {
        String bname = AIMBuddy.normalize(buddy.getName());
        denied.remove(bname);
        permitted.add(bname);
        connection.sendPermit(bname);
    }

    /**
     * Gets the permit mode that is set on the server.
     * 
     * @return int representation (see public statics) of current permit mode.
     */
    public int getPermitMode() {
        return connection.getPermitMode();
    }

    /**
     * Sets the permit mode on the server. (Use constants from AIMSender)
     * 
     * @param mode
     */
    public void setPermitMode(int mode) {
    	if (mode == DENY_SOME && this.denied.size() == 0) {
            logger.info("Attempting to deny some, and none are denied, ignoring.");
            return;
        } else if (mode == PERMIT_SOME && this.permitted.size() == 0) {
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

	public String getName() {
		return connection.getName();
	}
	
	public long getLastMessageTimestamp() {
		return connection.getLastMessageTimestamp();
	}
	
	private class RawListener implements AIMRawListener {

		public void handleMessage(String buddy, String htmlMessage) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleMessage(buddy, htmlMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateMessage(buddy, htmlMessage);
		}

		public void handleSetEvilAmount(String buddy, int amount) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleSetEvilAmount(buddy, amount);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// TODO Auto-generated method stub
            AIMBuddy aimbud = getBuddy(buddy);
            
            // if what we have is less than what the server just sent, its
            // a warning otherwise it was just a server decrement update
            
            boolean increased = getBuddy(buddy).getWarningAmount() < amount;
            
            aimbud.setWarningAmount(amount);
            
			if (increased) {
                generateWarning(buddy, amount);
            }
		}

		public void handleBuddySignOn(String buddy, String htmlInfo) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleBuddySignOn(buddy, htmlInfo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateBuddySignOn(buddy, htmlInfo);
		}

		public void handleBuddySignOff(String buddy, String htmlInfo) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleBuddySignOff(buddy, htmlInfo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateBuddySignOff(buddy, htmlInfo);			
		}

		public void handleBuddyUnavailable(String buddy, String htmlMessage) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleBuddyUnavailable(buddy, htmlMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateBuddyUnavailable(buddy, htmlMessage);			
			
		}

		public void handleBuddyAvailable(String buddy, String htmlMessage) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleBuddyAvailable(buddy, htmlMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateBuddyAvailable(buddy, htmlMessage);			
		}

		public void handleConnected() {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleConnected();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateConnected();			
		}

		public void handleDisconnected() {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleDisconnected();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateDisconnected();
		}

		public void handleError(String error, String message) {
			for (AIMRawListener l : rawListeners) {
				try {
					l.handleError(error, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			generateError(error, message);
		}
	}
}

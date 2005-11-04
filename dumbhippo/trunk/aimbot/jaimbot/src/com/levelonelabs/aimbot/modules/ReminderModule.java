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

package com.levelonelabs.aimbot.modules;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMAdapter;
import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.XMLizable;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * A class to handle personal reminders
 * 
 * @author Will Gorman
 * @created May 7th, 2003
 */
public class ReminderModule extends BotModule implements XMLizable {
    private static ArrayList services;
    private static Logger logger = Logger.getLogger(ReminderModule.class.getName());
    private HashMap reminders = new HashMap();

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("remind");
        services.add("remember");
        services.add("forget");
    }


    /**
     * Constructor for the ReminderModule object
     * 
     * @param bot
     */
    public ReminderModule(AIMBot bot) {
        super(bot);
        // register to here about signon events
        super.addAIMListener(new AIMAdapter() {
            public void handleBuddySignOn(AIMBuddy buddy, String info) {
                String savePref = buddy.getPreference("reminderatsignon");
                if ((savePref != null) && savePref.equals("true")) {
                    retrieveReminders(buddy);
                }
            }
        });
    }


    /**
     * Gets the name attribute of the ReminderModule object
     * 
     * @return The name value
     */
    public String getName() {
        return "Reminder Module";
    }


    /**
     * Gets the services attribute of the ReminderModule object
     * 
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb.append("<B>remember <i>TEXT</i></B> (stores text in the system for retrieval later)\n");
        sb.append("<B>forget <i>All or #</i></B> (removes all items or a specific item)\n");
        sb.append("<B>remind</B> (lists all reminders)\n");
        sb
            .append("* If the preference \"reminderatsignon\" is set to true, you will automatically get a list when you login to aim.\n");

        return sb.toString();
    }


    /**
     * Grabs and sends any stored reminders for a buddy
     * 
     * @param buddy
     * @return whether any messages where found
     */
    public boolean retrieveReminders(AIMBuddy buddy) {
        // check for messages
        ArrayList br = (ArrayList) reminders.get(buddy.getName());
        if (br != null) {
            if (br.size() > 0) {
                String reminder = "Reminders:<BR>";

                // collect the messages
                for (int i = 0; i < br.size(); i++) {
                    reminder += ((i + 1) + ")" + (br.get(i) + "<BR>"));
                }

                // send the list
                super.sendMessage(buddy, reminder);
                return true;
            }
        }
        return false;
    }


    /**
     * Handle a messaging query
     * 
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.toLowerCase().startsWith("remember")) {
            handleRemember(buddy, query);
        } else if (query.toLowerCase().startsWith("forget")) {
            handleForget(buddy, query);
        } else if (query.toLowerCase().startsWith("remind")) {
            if (!retrieveReminders(buddy)) {
                sendMessage(buddy, "No reminders available.");
            }
        }
    }


    /**
     * Remember funcionality
     * 
     * @param buddy
     * @param query
     */
    private void handleRemember(AIMBuddy buddy, String query) {
        String str = query.substring(8);
        if ((str != null) && (str.trim().length() != 0)) {
            addReminder(buddy, str);
            sendMessage(buddy, "Reminder stored.");
        } else {
            if (!retrieveReminders(buddy)) {
                sendMessage(buddy, "No reminders available.");
            }
        }
    }


    /**
     * Forget functionality
     * 
     * @param buddy
     * @param query
     */
    private void handleForget(AIMBuddy buddy, String query) {
        // handle offline request
        StringTokenizer st = new StringTokenizer(query, " ");

        // check for proper params
        if (st.countTokens() != 2) {
            sendMessage(buddy, "ERROR:\n" + help());
            return;
        }
        st.nextToken();
        String tok = st.nextToken();
        if (tok.equalsIgnoreCase("all")) {
            clearReminders(buddy);
            sendMessage(buddy, "All reminders removed.");
        } else {
            try {
                int loc = Integer.parseInt(tok) - 1;
                if (removeReminder(buddy, loc)) {
                    sendMessage(buddy, "Reminder removed.");
                } else {
                    sendMessage(buddy, "ERROR: number " + tok + " not valid.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(buddy, "ERROR: number invalid: " + tok);
            }
        }
    }


    /**
     * Remove Specific Reminder
     * 
     * @param buddy
     *            buddy
     * @param i
     *            list item number
     * @return true if remove took place
     */
    public boolean removeReminder(AIMBuddy buddy, int i) {
        ArrayList br = (ArrayList) reminders.get(buddy.getName());
        if ((br != null) && (i >= 0) && (i < br.size())) {
            br.remove(i);
            return true;
        }
        return false;
    }


    /**
     * Clear reminders for a buddy
     * 
     * @param buddy
     *            buddy
     */
    public void clearReminders(AIMBuddy buddy) {
        ArrayList br = (ArrayList) reminders.get(buddy.getName());
        if (br != null) {
            br.clear();
        }
    }


    /**
     * Add a reminder for a buddy
     * 
     * @param buddy
     *            buddy
     * @param reminder
     *            reminder
     */
    public void addReminder(AIMBuddy buddy, String reminder) {
        ArrayList br = (ArrayList) reminders.get(buddy.getName());
        if (br == null) {
            br = new ArrayList();
            reminders.put(buddy.getName(), br);
        }
        br.add(reminder);
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#writeState(Element)
     */
    public void writeState(Element emptyStateElement) {
        Document doc = emptyStateElement.getOwnerDocument();
        Iterator iter = reminders.keySet().iterator();

        while (iter.hasNext()) {
            String name = (String) iter.next();
            ArrayList br = (ArrayList) reminders.get(name);
            if (br.size() > 0) {
                Element buddyElem = doc.createElement("buddy");
                buddyElem.setAttribute("name", name);
                for (int i = 0; i < br.size(); i++) {
                    Element remElem = doc.createElement("reminder");
                    String reminder = (String) br.get(i);
                    CDATASection data = doc.createCDATASection(reminder);
                    remElem.appendChild(data);
                    buddyElem.appendChild(remElem);
                }
                emptyStateElement.appendChild(buddyElem);
            }
        }
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#readState(Element)
     */
    public void readState(Element fullStateElement) {
        // parse reminders
        reminders = new HashMap();
        NodeList list = fullStateElement.getElementsByTagName("buddy");
        for (int i = 0; i < list.getLength(); i++) {
            Element buddyElem = (Element) list.item(i);
            String name = buddyElem.getAttribute("name");
            AIMBuddy buddy = getBuddy(name);
            if (buddy != null) {
                NodeList rs = buddyElem.getElementsByTagName("reminder");
                for (int j = 0; j < rs.getLength(); j++) {
                    Element remindElem = (Element) rs.item(j);
                    NodeList cdatas = remindElem.getChildNodes();
                    for (int k = 0; k < cdatas.getLength(); k++) {
                        Node node = cdatas.item(k);
                        if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
                            String reminder = node.getNodeValue();
                            addReminder(buddy, reminder);
                            break;
                        }
                    }
                }
            }
        }
    }
}

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.XMLizable;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * A class to handle scheduling commands to happen at certain times
 * 
 * @author Steve Zingelewicz
 * @created May 7th, 2003
 */
public class ScheduleModule extends BotModule implements XMLizable {
    private static ArrayList services;
    private static Logger logger = Logger.getLogger(ScheduleModule.class.getName());
    private static HashMap events = new HashMap();
    private static Timer timer;


    private class SchedulerTask extends TimerTask {
        AIMBuddy buddy;
        String cmd;
        String triggerTime;
        Date createDate;


        SchedulerTask(AIMBuddy _buddy, String _cmd) {
            buddy = _buddy;
            cmd = _cmd;
            createDate = new Date(); // Record the initial time when the
                                        // event
            // was requested
        }


        public void run() {
            System.err.println("Timeout for: " + buddy.getName() + " cmd:" + cmd);
            bot.handleMessage(buddy, cmd);

            removeEvent(this);
        }


        public AIMBuddy getBuddy() {
            return (buddy);
        }


        public String getCommand() {
            return (cmd);
        }


        public String getCreateDate() {
            return (createDate.toString());
        }
    }

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("schedule");
        services.add("cancel");
        services.add("events");
        // Create the timer to handle timed reminding events
        timer = new Timer();
    }


    /**
     * Constructor for the ReminderModule object
     * 
     * @param bot
     */
    public ScheduleModule(AIMBot bot) {
        super(bot);
    }


    /**
     * Gets the name attribute of the SchedulerModule object
     * 
     * @return The name value
     */
    public String getName() {
        return "Scheduler Module";
    }


    /**
     * Gets the services attribute of the SchedulerModule object
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
        sb.append("<B>schedule <i>TEXT</i></B> (stores text in the system for retrieval later)\n");
        sb.append("<B>cancel <i>All or #</i></B> (removes all items or a specific item)\n");
        sb.append("<B>events</B> (lists all reminders)\n");
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
    public boolean retrieveEvents(AIMBuddy buddy) {
        // check for messages
        ArrayList br = (ArrayList) events.get(buddy.getName());
        if (br != null) {
            String eventlist = "Events:<BR>";

            if (br.size() != 0) {
                // collect the messages
                for (int i = 0; i < br.size(); i++) {
                    SchedulerTask st = (SchedulerTask) br.get(i);
                    eventlist += ((i + 1) + ")" + (st.getCommand() + " scheduled " + st.getCreateDate() + "<BR>"));
                }
            } else
                eventlist = "No events scheduled.";

            // send the list
            super.sendMessage(buddy, eventlist);
            return true;
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
        if (query.toLowerCase().startsWith("schedule")) {
            handleSchedule(buddy, query);
        } else if (query.toLowerCase().startsWith("cancel")) {
            handleCancel(buddy, query);
        } else if (query.toLowerCase().startsWith("events")) {
            if (!retrieveEvents(buddy)) {
                sendMessage(buddy, "No events scheduled.");
            }
        }
    }


    /**
     * Schedule time parsing function This function is getting huge and
     * unwieldy. Once I get it all worked out, I'll figure out how to make it
     * more manageable.
     * 
     * @param buddy
     * @param query
     */
    private void handleSchedule(AIMBuddy buddy, String query) {
        String token;
        StringTokenizer st = new StringTokenizer(query, " ");
        int state = 0;
        int stop = 0;
        char ch;
        long period = 60000; // Default to 1 minute
        String command = "";
        SimpleDateFormat sdf = new SimpleDateFormat();
        Date eventDate = null;
        int sched_type = 0; // Schedule type

        try {

            while (st.hasMoreTokens() && stop == 0) {
                token = st.nextToken();
                System.err.println("token: " + token + "state:" + String.valueOf(state));

                switch (state) {
                    case 0 :
                        // If you don't have schedule here, something went
                        // horribly wrong
                        if (token.equals("schedule"))
                            state = 1;
                        else
                            state = 99;
                        break;
                    case 1 :
                        // Determine the flavor of the schedule command
                        if (token.equals("at")) {
                            sched_type = 1;
                            state = 2;
                        } else if (token.equals("in")) {
                            sched_type = 2;
                            state = 3;
                        } else if (token.equals("every")) {
                            sched_type = 3;
                            state = 4;
                        } else
                            state = 99;
                        break;
                    case 2 :
                        // At syntax
                        // Find the "at" and the "-" delimiter and give date
                        // format everything in there
                        int at_idx = query.indexOf("at");
                        int delim_idx = query.indexOf("-");
                        if (at_idx != -1 && delim_idx != -1) {

                            System.err.println("Schedtoken:" + query.substring(at_idx + 2, delim_idx).trim());
                            try {
                                eventDate = sdf.parse(query.substring(at_idx + 2, delim_idx).trim());
                                state = 51;
                            } catch (Exception e) {
                                System.err.println("Can't parse date & time, trying just time..");
                                state = 99;
                            }

                            try {
                                // Just getting a time. Assume today's date
                                sdf = new SimpleDateFormat("h:mm a");
                                eventDate = sdf.parse(query.substring(at_idx + 2, delim_idx).trim());
                                Calendar today = Calendar.getInstance();
                                today.set(Calendar.HOUR, 0);
                                today.set(Calendar.MINUTE, 0);
                                today.set(Calendar.SECOND, 0);
                                Date todaysDate = today.getTime();
                                System.err.println("event date:" + eventDate.toString());
                                System.err.println("today's date:" + todaysDate.toString());
                                System.err.println(eventDate.getTime());
                                System.err.println(todaysDate.getTime());
                                // For the moment, peel off the 5 hours EST is
                                // off from GMT
                                eventDate.setTime(eventDate.getTime() + todaysDate.getTime() - 18000000);
                                state = 51;
                            } catch (Exception e) {
                                System.err.println("Can't parse date or time, bailing");
                                state = 99;
                            }

                        } else
                            state = 99;
                        break;
                    case 3 :
                        // In syntax
                        // read the last character (s=seconds, m = minutes, h =
                        // hours, d=days)
                        // Strip the letter and get the number
                        ch = token.charAt(token.length() - 1);
                        period = Long.parseLong(token.substring(0, token.length() - 1));
                        state = 50;

                        if (ch == 's') { // seconds
                            period = period * 1000;
                        } else if (ch == 'm') { // minutes
                            period = period * 60000;
                        } else if (ch == 'h') { // hours
                            period = period * 3600000;
                        } else if (ch == 'd') { // days
                            period = period * 86400000;
                        } else
                            state = 99;
                        break;
                    case 4 :
                        // Every syntax
                        break;
                    case 50 :
                        // There should be a delimiter now.
                        if (token.equals("-")) {
                            stop = 1;
                            command = query.substring(query.indexOf('-') + 1).trim();
                        } else
                            state = 99;
                        break;
                    case 51 :
                        // Loop, looking for the delimiter now.
                        if (token.equals("-")) {
                            stop = 1;
                            command = query.substring(query.indexOf('-') + 1).trim();
                        }
                        break;
                    case 99 :
                        // Error state, stop the machine
                        stop = 1;
                        break;
                }
            }

            if (state == 1) // Machine ended with only a "Schedule" found. Just
            // list the events
            {
                if (!retrieveEvents(buddy)) {
                    sendMessage(buddy, "No events available.");
                }
            } else if (state == 50) { // Final state, all is well, schedule
                                        // "IN"
                // type
                System.err.println("Scheduling " + command + " in " + String.valueOf(period));
                addEvent(buddy, period, command);
                sendMessage(buddy, "Event scheduled.");
            } else if (state == 51) { // Final state, all is well, schedule
                                        // "AT"
                // type
                System.err.println("Scheduling " + command + " at " + eventDate.toString());
                addEvent(buddy, eventDate, command);
                sendMessage(buddy, "Event scheduled.");
            } else if (state == 99) {
                sendMessage(buddy, "Unable to understand schedule command");
            } else
                sendMessage(buddy, "That schedule command is not implemented yet");
        } catch (Exception e) {
            sendMessage(buddy, "Unable to understand schedule command");
            e.printStackTrace();
        }
    }


    /**
     * Forget functionality
     * 
     * @param buddy
     * @param query
     */
    private void handleCancel(AIMBuddy buddy, String query) {
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
            clearEvents(buddy);
            sendMessage(buddy, "All events removed.");
        } else {
            try {
                int loc = Integer.parseInt(tok) - 1;
                if (removeEvent(buddy, loc)) {
                    sendMessage(buddy, "Event cancelled.");
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
    public boolean removeEvent(AIMBuddy buddy, int i) {
        ArrayList br = (ArrayList) events.get(buddy.getName());
        if ((br != null) && (i >= 0) && (i < br.size())) {
            br.remove(i);
            return true;
        }
        return false;
    }


    /**
     * @brief Removes an event given the event's SchedulerTask reference
     * @param sTask
     *            The reference to the item to remove
     */
    void removeEvent(SchedulerTask sTask) {
        // Find the event to remove
        ArrayList al;
        al = (ArrayList) events.get(sTask.buddy.getName());
        System.err.println("Removing events - " + String.valueOf(al.size()));
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {

                if ((SchedulerTask) al.get(i) == sTask) {
                    al.remove(i);
                    System.err.println("Removing " + String.valueOf(i));
                    // removeEvent(sTask.buddy, i);
                    break;
                }
            }
        }
    }


    /**
     * clear reminders for a buddy
     * 
     * @param buddy
     *            buddy
     */
    public void clearEvents(AIMBuddy buddy) {
        ArrayList br = (ArrayList) events.get(buddy.getName());
        if (br != null) {
            br.clear();
        }
    }


    /**
     * Add a reminder for a buddy
     * 
     * @param buddy
     *            buddy
     * @param period
     *            Delay for timer
     */
    public void addEvent(AIMBuddy buddy, long period, String command) {
        ArrayList br = (ArrayList) events.get(buddy.getName());
        if (br == null) {
            br = new ArrayList();
            events.put(buddy.getName(), br);
        }

        SchedulerTask st = new SchedulerTask(buddy, command);
        timer.schedule(st, period);

        br.add(st);
    }


    /**
     * Add a reminder for a buddy
     * 
     * @param buddy
     *            buddy
     * @param eventDate
     *            Date of scheduled event
     */
    public void addEvent(AIMBuddy buddy, Date eventDate, String command) {
        ArrayList br = (ArrayList) events.get(buddy.getName());
        if (br == null) {
            br = new ArrayList();
            events.put(buddy.getName(), br);
        }

        SchedulerTask st = new SchedulerTask(buddy, command);
        timer.schedule(st, eventDate);

        br.add(st);
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#writeState(Element)
     */
    public void writeState(Element emptyStateElement) {
        Document doc = emptyStateElement.getOwnerDocument();
        Iterator iter = events.keySet().iterator();

        while (iter.hasNext()) {
            String name = (String) iter.next();
            ArrayList br = (ArrayList) events.get(name);
            if (br.size() > 0) {
                Element buddyElem = doc.createElement("buddy");
                buddyElem.setAttribute("name", name);
                for (int i = 0; i < br.size(); i++) {
                    Element remElem = doc.createElement("event");
                    SchedulerTask st = (SchedulerTask) br.get(i);

                    Element buddy = doc.createElement("buddy");
                    CDATASection buddy_data = doc.createCDATASection(st.getBuddy().getName());
                    buddy.appendChild(buddy_data);

                    Element cmd = doc.createElement("command");
                    CDATASection cmd_data = doc.createCDATASection(st.getCommand());
                    cmd.appendChild(cmd_data);

                    Element createDate = doc.createElement("createDate");
                    CDATASection cd_data = doc.createCDATASection(st.getCreateDate());
                    createDate.appendChild(cd_data);

                    remElem.appendChild(buddy);
                    remElem.appendChild(cmd);
                    remElem.appendChild(createDate);
                    buddyElem.appendChild(remElem);
                }
                emptyStateElement.appendChild(buddyElem);
            }
        }
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#readState(Element)
     */

    // This will be much harder to fix. Will have to diff the create date + the
    // period
    // with the current date to see if this reminder has been in cold storage
    // for too
    // long. If it hasn't, then figure out how long is left until the desired
    // time and
    // reschedule it with the new period.
    public void readState(Element fullStateElement) {
        /*
         * //parse reminders events=new HashMap(); NodeList
         * list=fullStateElement.getElementsByTagName("buddy"); for(int i=0; i <
         * list.getLength(); i++) { Element buddyElem=(Element) list.item(i);
         * String name=buddyElem.getAttribute("name"); AIMBuddy
         * buddy=getBuddy(name); if(buddy != null) { NodeList
         * rs=buddyElem.getElementsByTagName("event"); for(int j=0; j <
         * rs.getLength(); j++) { String cmd; Element remindElem=(Element)
         * rs.item(j); NodeList elems=remindElem.getChildNodes(); for(int k=0; k <
         * elems.getLength(); k++) { =cdatas.item(k); node.
         * if(node.getNodeType() == Node.CDATA_SECTION_NODE) { String
         * event=node.getNodeValue(); addEvent(buddy,10, event); break; } } } } }
         */
    }
}
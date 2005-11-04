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

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.StringTokenizer;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMGroup;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Implements list-based messaging services.
 * 
 * @author Scott Oster
 * @created May 28, 2002
 */
public class ListModule extends BotModule {
    private static ArrayList services;

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("subscribe");
        services.add("unsubscribe");
        services.add("post");
        services.add("members");
        services.add("makelist");
        services.add("lists");
        services.add("removelist");
        services.add("addlistmember");
        services.add("rmlistmember");
    }


    /**
     * Constructor for the ListModule object
     * 
     * @param bot
     */
    public ListModule(AIMBot bot) {
        super(bot);
    }


    /**
     * Gets the services attribute of the ListModule object
     * 
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the ListModule object
     * 
     * @return The name value
     */
    public String getName() {
        return "List Module";
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb.append("<B>lists</B> (shows all the current lists.)\n");
        sb.append("<B>subscribe <i>LISTNAME</i> </B> (subscribe yourself to the named list.)\n");
        sb.append("<B>unsubscribe <i>LISTNAME</i> </B> (unsubscribe yourself from the named list.)\n");
        sb.append("<B>post <i>LISTNAME</i> <i>MESSAGE</i></B> (will post the message to everyone in the list."
            + "  Offline members will be told when they sign on.)\n");
        sb.append("<B>members <i>LISTNAME</i></B> (will show the members of everyone in the list. *ADMIN ONLY*)\n");
        sb.append("<B>makelist <i>LISTNAME</i></B> (creates a new list. *ADMIN ONLY*)\n");
        sb.append("<B>removelist <i>LISTNAME</i></B> (Removes a list *ADMIN ONLY*)\n");
        sb.append("<B>addlistmember <i>LISTNAME BUDDYNAME</i></B> (adds a user to a list) *ADMIN ONLY*)\n");
        sb.append("<B>rmlistmember <i>LISTNAME BUDDYNAME</i> </B> (removes a user from a list) *ADMIN ONLY*)\n");
        return sb.toString();
    }


    /**
     * Handle a list query
     * 
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.toLowerCase().trim().equals("lists")) {
            processLists(buddy);
        } else {
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            if (query.toLowerCase().startsWith("subscribe")) {
                processSubscribe(buddy, st);
            } else if (query.toLowerCase().startsWith("unsubscribe")) {
                processUnsubscribe(buddy, st);
            } else if (query.toLowerCase().startsWith("members")) {
                processMembers(buddy, st);
            } else if (query.toLowerCase().startsWith("makelist")) {
                processMakeList(buddy, st);
            } else if (query.toLowerCase().startsWith("post")) {
                processPost(buddy, st);
            } else if (query.toLowerCase().startsWith("removelist")) {
                processRemoveList(buddy, st);
            } else if (query.toLowerCase().startsWith("addlistmember")) {
                processAddListMember(buddy, st);
            } else if (query.toLowerCase().startsWith("rmlistmember")) {
                processRemoveListMember(buddy, st);
            }
        }

    }


    /**
     * @param buddy
     */
    private void processLists(AIMBuddy buddy) {
        StringBuffer lists = new StringBuffer("The current lists are:\n");
        for (Enumeration enumer = super.bot.getGroups(); enumer.hasMoreElements();) {
            AIMGroup next = (AIMGroup) enumer.nextElement();
            lists.append(next.getName() + " with " + next.size() + " members.\n");
        }
        super.sendMessage(buddy, lists.toString());
    }


    /**
     * @param buddy
     * @param st
     */
    private void processRemoveListMember(AIMBuddy buddy, StringTokenizer st) {
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry. This command is for admins only.");
        } else if (st.countTokens() < 3) {
            super.sendMessage(buddy, "Error:\n" + help());
        } else {
            String imcommand = st.nextToken();
            String list = st.nextToken();
            String buddyname = st.nextToken();

            AIMGroup group = super.bot.getGroup(list);

            if (group != null) {
                if (group.remove(buddyname)) {
                    super.sendMessage(buddy, buddyname + " was successfully removed from list:" + list);
                } else {
                    super.sendMessage(buddy, buddyname + " was not a member of list:" + list);
                }
            } else {
                super.sendMessage(buddy, "The list, " + list + ", does not exist.");
            }

        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processAddListMember(AIMBuddy buddy, StringTokenizer st) {
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry. This command is for admins only.");
        } else if (st.countTokens() < 3) {
            super.sendMessage(buddy, "Error: \n" + help());

        } else {
            String imcommand = st.nextToken();
            String list = st.nextToken();
            String buddyname = st.nextToken();
            AIMBuddy ibuddy = getBuddy(buddyname);
            if (ibuddy == null) {
                super.sendMessage(buddy, buddyname + " is not a user, please ADDUSER");
            } else {
                AIMGroup group = super.bot.getGroup(list);
                if (group != null) {
                    if (group.add(ibuddy.getName())) {
                        super.sendMessage(buddy, buddyname + " was successfully added to list:" + list);
                    } else {
                        super.sendMessage(buddy, buddyname + " was already a member of list:" + list);
                    }
                } else {
                    super.sendMessage(buddy, "The list, " + list + ", does not exist.");
                }
            }
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processRemoveList(AIMBuddy buddy, StringTokenizer st) {
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry. This command is for admins only.");
        } else if (st.countTokens() < 1) {
            super.sendMessage(buddy, "Error: \n" + help());
        } else {
            String list = st.nextToken();
            list = st.nextToken();
            AIMGroup group = super.bot.getGroup(list);
            if (group != null) {
                super.bot.removeGroup(group);
                super.sendMessage(buddy, "The list " + list + " has been removed");
            } else {
                super.sendMessage(buddy, "The list " + list + " does not exist");
            }
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processPost(AIMBuddy buddy, StringTokenizer st) {
        if (st.countTokens() < 3) {
            super.sendMessage(buddy, "ERROR:\n" + help());
        } else {
            String imcommand = st.nextToken();
            String list = st.nextToken();

            // grab the rest of the message and send it to the group
            String message = "";
            while (st.hasMoreTokens()) {
                message += (" " + st.nextToken());
            }

            AIMGroup group = super.bot.getGroup(list);
            if (group != null) {
                sendGroupMessage(group, message, buddy);
            } else {
                super.sendMessage(buddy, "The list, " + list + ", does not exist.");
            }
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processMakeList(AIMBuddy buddy, StringTokenizer st) {
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry. This command is for admins only.");
        } else {
            String imcommand = st.nextToken();
            String list = st.nextToken();
            AIMGroup group = super.bot.getGroup(list);
            if (group != null) {
                super.sendMessage(buddy, "The list, " + list + ", already exists.");
            } else {
                super.bot.addGroup(new AIMGroup(list));
                super.sendMessage(buddy, "The list, " + list + ", was created with no members.");
            }
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processMembers(AIMBuddy buddy, StringTokenizer st) {
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry. This command is for admins only.");
        } else {
            String imcommand = st.nextToken();
            String list = st.nextToken();
            AIMGroup group = super.bot.getGroup(list);
            if (group != null) {
                super.sendMessage(buddy, "The list, " + list + ", contains: " + group.toString());
            } else {
                super.sendMessage(buddy, "The list, " + list + ", does not exist.");
            }
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processUnsubscribe(AIMBuddy buddy, StringTokenizer st) {
        String imcommand = st.nextToken();
        String list = st.nextToken();
        AIMGroup group = super.bot.getGroup(list);
        if (group != null) {
            if (group.remove(buddy.getName())) {
                super.sendMessage(buddy, "You were successfully removed from list:" + list);
            } else {
                super.sendMessage(buddy, "You were not a member of list:" + list);
            }
        } else {
            super.sendMessage(buddy, "The list, " + list + ", does not exist.");
        }
    }


    /**
     * @param buddy
     * @param st
     */
    private void processSubscribe(AIMBuddy buddy, StringTokenizer st) {
        String imcommand = st.nextToken();
        String list = st.nextToken();
        AIMGroup group = super.bot.getGroup(list);
        if (group != null) {
            if (group.add(buddy.getName())) {
                super.sendMessage(buddy, "You were successfully added to list:" + list);
            } else {
                super.sendMessage(buddy, "You were already a member of list:" + list);
            }
        } else {
            super.sendMessage(buddy, "The list, " + list + ", does not exist.");
        }
    }


    /**
     * Sends a message to a group of buddies
     * 
     * @param group
     * @param message
     * @param from
     */
    private void sendGroupMessage(AIMGroup group, String message, AIMBuddy from) {
        int toldNow = 0;
        for (ListIterator it = group.getList().listIterator(); it.hasNext();) {
            AIMBuddy next = getBuddy((String) it.next());
            if (next.isOnline()) {
                super.sendMessage(next, from.getName() + " posted to the " + group.getName() + " list:" + message);
                toldNow++;
            } else {
                next.addMessage(from.getName() + " posted to the " + group.getName() + " list: \"" + message + "\" at "
                    + new Date());
            }
        }
        super.sendMessage(from, "Told " + toldNow + " of " + group.size() + " list members.");
    }
}

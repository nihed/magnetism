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
import java.util.StringTokenizer;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Used to hanle administration of users.
 * 
 * @author Will Gorman, Scott Oster
 * @created January 31, 2002
 */
public class UserAdminModule extends BotModule {
    private static ArrayList services;

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("adduser");
        services.add("rmuser");
        services.add("denyuser");
        services.add("permituser");
        services.add("permitmode");
    }


    /**
     * Constructor for the UserAdminModule object
     * 
     * @param bot
     */
    public UserAdminModule(AIMBot bot) {
        super(bot);
    }


    /**
     * Gets the services attribute of the UserAdminModule object
     * 
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the UserAdminModule object
     * 
     * @return The name value
     */
    public String getName() {
        return "User Administration Module";
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb
            .append("<B>adduser <i>BUDDY</i> <ADMIN></B> (adds a user to the list, along with allowing for admin perms)\n");
        sb.append("<B>rmuser <i>BUDDY</i></B> (removes a user from the list)\n");
        sb.append("<B>denyuser <i>BUDDY</i></B> (deny a user *ADMIN ONLY*)\n");
        sb.append("<B>permituser <i>BUDDY</i></B> (permit a user *ADMIN ONLY*)\n");
        sb.append("<B>permitmode <i>[MODE]</i></B> (set or display the permitmode*ADMIN ONLY*)\n");
        return sb.toString();
    }


    /**
     * Perform user maintenance
     * 
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.toLowerCase().startsWith("adduser")) {
            // add a user
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            // grab the user to add
            String imcommand = st.nextToken();
            String imcommandTo = st.nextToken();
            AIMBuddy to = super.getBuddy(imcommandTo);
            if (to != null) {
                super.sendMessage(buddy, "User " + imcommandTo + " already exists in the system.");
                return;
            }

            // only admins can add other admins
            to = new AIMBuddy(imcommandTo);
            to.addRole(AIMBot.ROLE_USER);
            if (buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR) && st.hasMoreTokens()
                && st.nextToken().equalsIgnoreCase("ADMIN")) {
                to.addRole(AIMBot.ROLE_ADMINISTRATOR);
            }
            super.addBuddy(to);
            super.sendMessage(buddy, "User " + imcommandTo + " added.");
        } else if (query.toLowerCase().startsWith("rmuser")) {
            // remove a user
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            // grab the user to remove
            String imcommand = st.nextToken();
            String imcommandTo = st.nextToken();
            AIMBuddy to = super.getBuddy(imcommandTo);
            if (to == null) {
                super.sendMessage(buddy, "User " + imcommandTo + " does not exist in the system.");
            } else if (buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.removeBuddy(to);
                super.sendMessage(buddy, "User " + imcommandTo + " removed.");
            } else {
                super.sendMessage(buddy, "Only admins can remove users.");
            }
        } else if (query.toLowerCase().startsWith("denyuser")) {
            // deny a user
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            // grab the user to deny
            String imcommand = st.nextToken();
            String imcommandTo = st.nextToken();
            AIMBuddy to = super.getBuddy(imcommandTo);
            if (to == null) {
                super.sendMessage(buddy, "User " + imcommandTo + " does not exist in the system.");
            } else if (buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.denyBuddy(to);
                super.sendMessage(buddy, "User " + imcommandTo + " denied.");
            } else {
                super.sendMessage(buddy, "Only admins can deny users.");
            }
        } else if (query.toLowerCase().startsWith("permituser")) {
            // permit a user
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            // grab the user to permit
            String imcommand = st.nextToken();
            String imcommandTo = st.nextToken();
            AIMBuddy to = super.getBuddy(imcommandTo);
            if (to == null) {
                super.sendMessage(buddy, "User " + imcommandTo + " does not exist in the system.");
            } else if (buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.permitBuddy(to);
                super.sendMessage(buddy, "User " + imcommandTo + " permitted.");
            } else {
                super.sendMessage(buddy, "Only admins can permit users.");
            }
        } else if (query.toLowerCase().startsWith("permitmode")) {
            // set or display the permit mode
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() < 1) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

            if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.sendMessage(buddy, "Only admins can permit users.");
                return;
            }

            String imcommand = st.nextToken();
            if (st.hasMoreTokens()) {
                String newMode = st.nextToken();
                try {
                    int mode = Integer.parseInt(newMode);
                    if (mode < 1 || mode > 5) {
                        throw new NumberFormatException("Not within range.");
                    }
                    super.setPermitMode(mode);
                    super.sendMessage(buddy, "Set permit mode to: " + mode
                        + "\nPERMIT_ALL = 1, DENY_ALL = 2, PERMIT_SOME = 3, DENY_SOME = 4, PERMIT_BUDDIES = 5");

                } catch (NumberFormatException nfe) {
                    super.sendMessage(buddy, "Invalid permit mode.");
                }
            } else {
                super.sendMessage(buddy, "Current permit mode is: " + super.getPermitMode()
                    + "\nPERMIT_ALL = 1, DENY_ALL = 2, PERMIT_SOME = 3, DENY_SOME = 4, PERMIT_BUDDIES = 5");
            }
        }
    }
}
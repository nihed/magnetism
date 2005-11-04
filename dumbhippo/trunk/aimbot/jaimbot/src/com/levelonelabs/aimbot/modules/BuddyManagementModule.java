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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Used to hanle administration of buddies.
 * 
 * @author Scott Oster
 * @created January 09, 2004
 */
public class BuddyManagementModule extends BotModule {
    private static ArrayList services;
    static Logger logger = Logger.getLogger(BuddyManagementModule.class.getName());

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("makeusers");
        services.add("prune");
    }


    /**
     * Constructor for the BuddyManagementModule object
     * 
     * @param bot
     */
    public BuddyManagementModule(AIMBot bot) {
        super(bot);
    }


    /**
     * Gets the services attribute of the BuddyManagementModule object
     * 
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the BuddyManagementModule object
     * 
     * @return The name value
     */
    public String getName() {
        return "Buddy Management Module";
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb.append("<B>makeusers</B> (converts all non-user buddies to users *ADMIN ONLY*)\n");
        sb.append("<B>prune buddies</B> (removes all non-user buddies *ADMIN ONLY*)\n");
        sb.append("<B>prune inactive</B> (removes \"inactive\" users *ADMIN ONLY*)\n");
        return sb.toString();
    }


    /**
     * Perform buddy maintenance
     * 
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        StringTokenizer st = new StringTokenizer(query, " ");
        if (!st.hasMoreTokens()) {
            return;
        }
        String imcommand = st.nextToken().trim();

        // verify admin
        if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
            super.sendMessage(buddy, "Sorry, you must be an admin to use this command.");
            return;
        }

        if (imcommand.equalsIgnoreCase("makeusers")) {
            if (st.hasMoreTokens()) {
                super.sendMessage(buddy, "ERROR:\n" + help());
            }
            int numConverted = 0;
            Iterator iter = super.getBuddyNames();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                AIMBuddy bud = super.getBuddy(name);
                // if we found a buddy without user role, that isnt us
                if (!bud.hasRole(AIMBot.ROLE_USER) && !super.bot.getUsername().equals(name)) {
                    logger.fine("Adding " + AIMBot.ROLE_USER + " role to " + bud.getName());
                    bud.addRole(AIMBot.ROLE_USER);
                    numConverted++;
                }
            }
            super.sendMessage(buddy, "Converted " + numConverted + " buddies to users.");
            return;
        } else if (imcommand.equalsIgnoreCase("prune")) {
            if (st.countTokens() != 1) {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }
            String option = st.nextToken().trim();
            if (option.equalsIgnoreCase("buddies")) {
                int numRemoved = 0;
                Iterator iter = super.getBuddyNames();
                List toRemoveList = new ArrayList();
                while (iter.hasNext()) {
                    String name = (String) iter.next();
                    AIMBuddy bud = super.getBuddy(name);
                    // if we found a buddy without user/admin role, that isnt
                    // us
                    if (!bud.hasRole(AIMBot.ROLE_USER) && !bud.hasRole(AIMBot.ROLE_ADMINISTRATOR)
                        && !super.bot.getUsername().equalsIgnoreCase(name)) {
                        logger.fine("Removing non-user buddy " + bud.getName());
                        toRemoveList.add(bud);
                        numRemoved++;
                    }
                }
                super.removeBuddies(toRemoveList);
                super.sendMessage(buddy, "Removed " + numRemoved + " non-user buddies.");
                return;
            } else if (option.equalsIgnoreCase("inactive")) {
                int numRemoved = 0;
                Iterator iter = super.getBuddyNames();
                List toRemoveList = new ArrayList();
                while (iter.hasNext()) {
                    String name = (String) iter.next();
                    AIMBuddy bud = super.getBuddy(name);
                    // if we found a user without admin role, that isnt us
                    if (bud.hasRole(AIMBot.ROLE_USER) && !bud.hasRole(AIMBot.ROLE_ADMINISTRATOR)
                        && !super.bot.getUsername().equalsIgnoreCase(name)) {
                        // if they are inactive, remove them
                        if (isInactive(bud)) {
                            logger.fine("Removing inactive buddy " + bud.getName());
                            toRemoveList.add(bud);
                            numRemoved++;
                        }
                    }
                }
                super.removeBuddies(toRemoveList);
                super.sendMessage(buddy, "Removed " + numRemoved + " inactive users.");
                return;
            } else {
                super.sendMessage(buddy, "ERROR:\n" + help());
                return;
            }

        }
    }


    /**
     * Makes the decision of a user should be pruned
     * 
     * @todo add agressiveness option
     * @param bud
     * @return true if we consider the buddy inactive
     */
    private static boolean isInactive(AIMBuddy bud) {
        return !bud.hasMessages() && bud.getPreferences().size() == 0;
    }
}
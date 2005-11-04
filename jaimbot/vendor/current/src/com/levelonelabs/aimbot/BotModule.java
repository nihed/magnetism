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

package com.levelonelabs.aimbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.AIMListener;


/**
 * All Modules that wish to provide services to AIMBot should implement this.
 * This base class acts as a facade to allow a extending module to be pretty
 * much self-contained and just call methods here without needing to know about
 * the AIMSender.
 * 
 * @author Scott Oster
 * @created January 20, 2002
 */
public abstract class BotModule {
    /** A handle to the hosting AIMBot */
    protected AIMBot bot;


    /**
     * Constructor for the BotModule object
     * 
     * @param bot
     *            A handle to the hosting AIMBot
     */
    public BotModule(AIMBot bot) {
        this.bot = bot;
    }


    /**
     * Gets the canonical name of the BotModule
     * 
     * @return The name of the module
     */
    public abstract String getName();


    /**
     * Convience method to call aimsender's getBuddy
     * 
     * @param name
     * @return The buddy value
     */
    public AIMBuddy getBuddy(String name) {
        return this.bot.aim.getBuddy(name);
    }


    /**
     * Convience method to call aimsender's getBuddyIterator
     * 
     * @return iterator
     */
    public Iterator getBuddyNames() {
        return this.bot.aim.getBuddyNames();
    }


    /**
     * Must implement this to return a list of Strings that represent the
     * keywords the module wants to be called for
     * 
     * @return all list of strings that are the keywords the module wants to
     *         hear about
     */
    public abstract ArrayList getServices();


    /**
     * Convience method to call aimsender's addBuddy
     * 
     * @param buddy
     *            The feature to be added to the Buddy attribute
     */
    public void addBuddy(AIMBuddy buddy) {
        this.bot.aim.addBuddy(buddy);
    }


    /**
     * Convience method to call aimsender's addBuddies
     * 
     * @param aimbuddies
     *            List of AIMBuddys
     */
    public void addBuddies(List aimbuddies) {
        this.bot.aim.addBuddies(aimbuddies);
    }


    /**
     * Convience method to call aimsender's removeBuddy
     * 
     * @param buddy
     *            remove the specified buddy
     */
    public void removeBuddy(AIMBuddy buddy) {
        this.bot.aim.removeBuddy(buddy);
    }


    /**
     * Convience method to call aimsender's removeBuddies
     * 
     * @param aimbuddies
     *            List of AIMBuddys
     */
    public void removeBuddies(List aimbuddies) {
        this.bot.aim.removeBuddies(aimbuddies);
    }


    /**
     * Deny a buddy
     * 
     * @param buddy
     */
    public void denyBuddy(AIMBuddy buddy) {
        this.bot.aim.denyBuddy(buddy);
    }


    /**
     * Permit a buddy
     * 
     * @param buddy
     */
    public void permitBuddy(AIMBuddy buddy) {
        this.bot.aim.permitBuddy(buddy);
    }


    /**
     * Sets the permit mode on the server. (Use constants from AIMSender)
     * 
     * @param mode
     */
    public void setPermitMode(int mode) {
        this.bot.aim.setPermitMode(mode);
    }


    /**
     * Gets the permit mode that is set on the server.
     * 
     * @return int representation (see constants) of current permit mode.
     */
    public int getPermitMode() {
        return this.bot.aim.getPermitMode();
    }


    /**
     * Convenience method to call aimsender's sendMessage
     * 
     * @param buddy
     *            buddy to message
     * @param text
     *            text to send buddy
     */
    public void sendMessage(AIMBuddy buddy, String text) {
        this.bot.aim.sendMessage(buddy, text);
    }


    /**
     * Convenience method to call aimsender's sendWarning
     * 
     * @param buddy
     *            buddy to warn
     */
    public void sendWarning(AIMBuddy buddy) {
        this.bot.aim.sendWarning(buddy);
    }


    /**
     * Clear unvailable message
     */
    public void setAvailable() {
        this.bot.aim.setAvailable();
    }


    /**
     * Set unvailable message
     * 
     * @param reason
     */
    public void setUnavailable(String reason) {
        this.bot.aim.setUnavailable(reason);
    }


    /**
     * Convenience method to call aimsender's addAIMListener
     * 
     * @param listener
     *            The listener
     */
    public void addAIMListener(AIMListener listener) {
        this.bot.aim.addAIMListener(listener);
    }


    /**
     * A String to be presented to a client that descibes the services
     * (keywords) this module provides. NOTE: a URL to documentation is
     * acceptable.
     * 
     * @return the help text
     */
    public abstract String help();


    /**
     * This will be called when the AIMBot gets a string starting with a keyword
     * that this module advertised as a service.
     * 
     * @param buddy
     * @param query
     */
    public abstract void performService(AIMBuddy buddy, String query);
}

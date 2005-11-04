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
import java.util.Random;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aim.XMLizable;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Default module to give non AI responses to non-commands
 * 
 * @author (matty501 AT yahoo.com)
 */
public class DefaultModule extends BotModule implements XMLizable {
    private static ArrayList services;

    private static final String ADD_RESPONSE = "addResponse";
    private static final String REMOVE_RESPONSE = "rmResponse";
    private static final String LIST_RESPONSES = "listResponses";
    private static final String DEFAULT_RESPONSE = "Sorry, I don't understand.\nType \"help\" to see available commands";

    private List responses;
    private Random rand = new Random();

    /**
     * Initialize the service commands.
     */
    static {
        // init the services
        services = new ArrayList();
        services.add(ADD_RESPONSE);
        services.add(REMOVE_RESPONSE);
        services.add(LIST_RESPONSES);
    }


    /**
     * Constructor for the Default module
     * 
     * @param bot
     */
    public DefaultModule(AIMBot bot) {
        super(bot);
        this.responses = new ArrayList();
    }


    /**
     * Gets the services
     * 
     * @return The services
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name of the module
     * 
     * @return The name value
     */
    public String getName() {
        return "Default Module";
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb.append("Default Module\nThis will output a resonse selected randomly from a list of responses.\n");
        sb.append("<B>" + ADD_RESPONSE + " <i>TEXT</i></B> " + "(Add a response to the list) *Admin only*");
        sb.append("<B>" + REMOVE_RESPONSE + " <i>RESPONSE_NUMBER</i></B> "
            + "(Remove the specified response from the list) *Admin only*");
        sb.append("<B>" + LIST_RESPONSES + "</B> " + "(Lists the current responses.) *Admin only*");
        return sb.toString();
    }


    /**
     * Responds to an unknown command, or adds a term to respond with
     * 
     * @param buddy
     *            requesting buddy
     * @param query
     *            the message
     */
    public void performService(AIMBuddy buddy, String query) {
        StringTokenizer st = new StringTokenizer(query);
        if (query.toLowerCase().startsWith(ADD_RESPONSE.toLowerCase())) {
            if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.sendMessage(buddy, "Sorry, only admins can add responses.");
                return;
            }
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR: invalide format.\n" + help());
                return;
            }

            String imcommand = st.nextToken();
            String response = st.nextToken();
            while (st.hasMoreTokens()) {
                response = response + " " + st.nextToken();
            }

            this.responses.add(response);
            super.sendMessage(buddy, "Added response to the current list of " + this.responses.size()
                + " available choices.");
        } else if (query.toLowerCase().startsWith(REMOVE_RESPONSE.toLowerCase())) {
            if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.sendMessage(buddy, "Sorry, only admins can remove responses.");
                return;
            }
            if (st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR: invalide format.\n" + help());
                return;
            }

            String imcommand = st.nextToken();
            String response = st.nextToken();
            while (st.hasMoreTokens()) {
                response = response + " " + st.nextToken();
            }

            try {
                int index = Integer.parseInt(response) - 1;
                this.responses.remove(index);
                super.sendMessage(buddy, "Removed response from the current list of " + this.responses.size()
                    + " available choices.");
            } catch (Exception e) {
                super.sendMessage(buddy, "That response number was invalid.");
            }
        } else if (query.toLowerCase().startsWith(LIST_RESPONSES.toLowerCase())) {
            if (!buddy.hasRole(AIMBot.ROLE_ADMINISTRATOR)) {
                super.sendMessage(buddy, "Sorry, only admins can list responses.");
                return;
            }

            if (this.responses.size() > 0) {
                StringBuffer sb = new StringBuffer("Current list of responses:\n");
                for (int i = 0; i < this.responses.size(); i++) {
                    String res = (String) this.responses.get(i);
                    sb.append("<b>" + (i + 1) + ")</b>" + res + "\n");
                    if (i % 4 == 3) {
                        super.sendMessage(buddy, sb.toString());
                        sb.delete(0, sb.length());
                    }
                }
                if (sb.length() > 0) {
                    super.sendMessage(buddy, sb.toString());
                }
            } else {
                super.sendMessage(buddy, "No custom responses configured, using default.");
            }
        } else {
            String response = DEFAULT_RESPONSE;
            if (this.responses.size() > 0) {
                response = (String) this.responses.get(this.rand.nextInt(this.responses.size()));
            }
            super.sendMessage(buddy, response);
        }
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#writeState(Element)
     */
    public void writeState(Element emptyStateElement) {
        Document doc = emptyStateElement.getOwnerDocument();
        Iterator iter = this.responses.iterator();

        while (iter.hasNext()) {
            String response = (String) iter.next();
            Element responseElem = doc.createElement("response");
            responseElem.setAttribute("value", response);
            emptyStateElement.appendChild(responseElem);
        }
    }


    /**
     * @see com.levelonelabs.aim.XMLizable#readState(Element)
     */
    public void readState(Element fullStateElement) {
        // parse reminders
        this.responses = new ArrayList();
        NodeList list = fullStateElement.getElementsByTagName("response");
        for (int i = 0; i < list.getLength(); i++) {
            Element responseElem = (Element) list.item(i);
            String response = responseElem.getAttribute("value");
            this.responses.add(response);
        }
    }
}

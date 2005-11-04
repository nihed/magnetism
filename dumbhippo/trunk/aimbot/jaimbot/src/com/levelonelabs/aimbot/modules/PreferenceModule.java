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
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests to store and retrieve preferences
 *
 * @author Will Gorman
 *
 * @created January 31, 2002
 */
public class PreferenceModule extends BotModule {
    private static ArrayList services;

    /**
     * Initialize the service commands.
     */
    static {
        services=new ArrayList();
        services.add("listprefs");
        services.add("setpref");
    }

    /**
     * Constructor for the PreferenceModule object
     *
     * @param bot
     */
    public PreferenceModule(AIMBot bot) {
        super(bot);
    }

    /**
     * Gets the services attribute of the PreferenceModule object
     *
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the PreferenceModule object
     *
     * @return The name value
     */
    public String getName() {
        return "Preference Module";
    }


    /**
     * Describes the usage of the module
     *
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb=new StringBuffer();
        sb.append("<B>listprefs</B> (displays current preferences)\n");
        sb.append("<B>setpref <i>PREFERENCE</i> <i>VALUE</i></B> (sets a preference for the user)\n");
        sb.append("\n<b>EXAMPLE: setpref zipcode 12345</b>\n");
        return sb.toString();
    }


    /**
     * Handle a preference query
     *
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if(query.toLowerCase().startsWith("listprefs")) {
            // RETURN A LIST OF PREFERENCES
            HashMap prefs=buddy.getPreferences();
            Iterator iter=prefs.keySet().iterator();
            StringBuffer sb=new StringBuffer();
            sb.append("Preferences:\n");
            while(iter.hasNext()) {
                String key=(String) iter.next();
                sb.append(key).append(" = ").append(prefs.get(key)).append("\n");
            }
            super.sendMessage(buddy, sb.toString());
        } else if(query.toLowerCase().startsWith("setpref")) {
            //store the desired pref
            StringTokenizer st=new StringTokenizer(query, " ");
            if(st.countTokens() < 3) {
                super.sendMessage(buddy, "ERROR:\n"+help());
            } else {
                String imcommand=st.nextToken();
                String preference=st.nextToken();
                String value=st.nextToken();
                while(st.hasMoreTokens()) {
                    value+=(" "+st.nextToken());
                }
                buddy.setPreference(preference, value);
                super.sendMessage(buddy, "Preference ("+preference+" = "+value+") added.");
            }
        }
    }
}

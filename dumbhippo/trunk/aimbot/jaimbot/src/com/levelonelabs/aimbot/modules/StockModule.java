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

/**
 * StockModule.java
 *
 * @created Oct 26, 2002
 */
package com.levelonelabs.aimbot.modules;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;

/**
 * Handles requests to get stock quotes
 *
 * @author Scott Oster
 *
 * @created Oct 26, 2002
 */
public class StockModule extends BotModule {
    private static ArrayList services;
    /**
     * http://finance.yahoo.com/d/quotes.csv?f=snl1d1t1c1ohgv&s=GE (s= is the symbol to get, f= is
     * the order of the info)  Here is list of known variables that can be passed as options for
     * "f=" s  = Symbol                   l9 = Last Trade d1 = Date of Last Trade       t1 = Time
     * of Last Trade c1 = Change                   c  = Change - Percent Change o  = Open Trade
     * h  = High Trade g  = Low Trade                v  = Volume a  = Ask Price
     * b  = Bid Price j  = 52 week low              k  = 52 week high n  = Name of company
     * p  = Previous close x  = Name of Stock Exchange
     */
    private static final String URL= "http://finance.yahoo.com/d/quotes.csv";

    /**
     * Initialize the service commands.
     */
    static {
        services= new ArrayList();
        services.add("stock");
    }

    //May want to change options, but for now I'll just set them for the user.
    private String options= "snd1t1l9c";

    /**
     * Constructor for StockModule.
     *
     * @param bot
     */
    public StockModule(AIMBot bot) {
        super(bot);
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#getName()
     */
    public String getName() {
        return "Stock Module";
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#getServices()
     */
    public ArrayList getServices() {
        return services;
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#help()
     */
    public String help() {
        StringBuffer sb= new StringBuffer();
        sb.append("<B>stock <i>SYMBOLS</i></B> (displays stock price and other info for the given space separated symbols)\n");
        sb.append("* If the preference \"portfolio\" is set, you can omit the symbols to use those listed in the preference.");
        sb.append("<b>\n\nEXAMPLE: stock ge ibm</b>\n");
        return sb.toString();
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#performService(AIMBuddy, String)
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.trim().toLowerCase().startsWith("stock")) {
            StringTokenizer st= new StringTokenizer(query, " ");
            String portfolioPref= buddy.getPreference("portfolio");
            if (st.countTokens() < 2 && portfolioPref == null) {
                super.sendMessage(buddy, "ERROR:\n" + help());
            } else {
                String imcommand= st.nextToken();
                String symbol= "";
                while (st.hasMoreElements()) {
                    String temp= (String) st.nextElement();
                    symbol += temp;
                    if (st.hasMoreElements()) {
                        symbol += "+";
                    }
                }

                //no symbols listed; use the preference
                if (symbol.equals("")) {
                    symbol=portfolioPref;
                }
                
                symbol=symbol.replaceAll(" ", "+");

                String quote= "Couldn't get stock information";

                //grab the quote
                try {
                    String queryURL= URL + "?" + "f=" + this.options + "&s=" + symbol;
                    URL url= new URL(queryURL);

                    //setup the connection (do POST)
                    URLConnection connection= url.openConnection();
                    connection.setDoOutput(true);
                    connection.connect();

                    //read the results page
                    BufferedReader br= new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuffer sb= new StringBuffer();
                    while ((line= br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();

                    //extract the results
                    st= new StringTokenizer(sb.toString(), ",");
                    String results= "";
                    while (st.hasMoreElements()) {
                        results += (st.nextElement() + "\t");
                    }
                    if (!results.trim().equals("")) {
                        quote= results;
                    }

                    //error checking?
                } catch (Exception e) {
                    e.printStackTrace();
                }

                super.sendMessage(buddy, "\n" + quote);
            }
        }
    }
}

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


import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Creates Babel poetry from user input
 *
 * @author Scott Oster
 *
 * @created February 3, 2002
 */
public class BabelModule extends BotModule {
    /** Maximum number of trails to run */
    public static final int MAX_TRIALS=8;
    private static ArrayList services;
    private static Vector langs;
    private static Logger logger=Logger.getLogger(BabelModule.class.getName());

    /**
     * Initialize the service commands.
     */
    static {
        //init the services
        services=new ArrayList();
        services.add("babel");

        //init the langs
        langs=new Vector();
        langs.add("de");
        langs.add("it");
        langs.add("pt");
        langs.add("fr");
        langs.add("es");
    }

    /**
     * Constructor for the BabelModule object
     *
     * @param bot
     */
    public BabelModule(AIMBot bot) {
        super(bot);
    }

    /**
     * Talk to babel fish
     *
     * @param passage
     * @param translation
     *
     * @return the resulting translation
     */
    private static String runQuery(String passage, String translation) {
        try {
            String marker="XXXXX";

            //build the query
            String base="http://world.altavista.com/babelfish/tr";
            String query=base+"?"+"doit=done"+"&intl=1"+"&tt=urltext"+"&urltext="+
                URLEncoder.encode(marker+" "+passage)+"&lp="+translation;
            URL url=new URL(query);

            //setup the connection (do POST)
            URLConnection connection=url.openConnection();
            connection.setDoOutput(true);
            connection.connect();

            //read the results page
            BufferedReader br=new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuffer sb=new StringBuffer();
            while((line=br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();

            logger.finest("THE HTML IS:\n"+sb);
            //extract the results
            String result=sb.toString();
            if(result.indexOf("BabelFish Error") > -1) {
                //use a custom exception here instead
                logger.severe("#####ERROR#####:BabelFish puked\n");
                logger.fine(result);
                return "ERROR";
            }

            int index=result.indexOf(marker);
            int error=0;
            if(index >= 0) {
                result=result.substring(index+marker.length());
                int end=result.indexOf("</");
                if(end >= 0) {
                    result=result.substring(0, end);
                } else {
                    error=1;
                }
            } else {
                error=2;
            }

            if(error != 0) {
                logger.severe("There was a parsing error("+error+")!");
                logger.fine("HERE IS THE HTML:\n"+sb);
                return null;
            } else {
                return result.trim();
            }
        } catch(Exception e) {
            logger.severe("Got an error:"+e);
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Runs a sentance thru another language and resturns the convolution
     *
     * @param passage
     * @param lang
     *
     * @return convoluted english
     */
    private static String convolute(String passage, String lang) {
        String result=passage;
        result=runQuery(result, "en_"+lang);

        logger.finest("In "+lang+": "+result);
        result=runQuery(result, lang+"_en");
        logger.finest("[English]:"+result);
        return result;
    }


    /**
     * Turn text into poetry
     *
     * @param input text
     * @param numtrials
     *
     * @return babel poetry
     */
    private static String makePoetry(String input, int numtrials) {
        String result=input.trim();
        logger.fine("Running "+numtrials+" trials.");

        for(int i=0, actual_count=0; (i < numtrials) && (actual_count <= MAX_TRIALS);
                i++, actual_count++) {
            int ind=(int) (Math.random()*langs.size());
            String old=result;
            result=convolute(result, (String) langs.get(ind));
            if(old.equals(result)) {
                i--;
            }

            if(result.equals("ERROR") || result.trim().equals("")) {
                return old;
            }
        }

        return result;
    }


    /**
     * Gets the services attribute of the BabelModule object
     *
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the BabelModule object
     *
     * @return The name value
     */
    public String getName() {
        return "Babel Poetry Module";
    }


    /**
     * Describes the usage of the module
     *
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb=new StringBuffer();
        sb.append(
            "<B>babel [-n <i>DISTORTION</i>] <i>TEXT</i></B> (creates poetry from <TEXT>, this may take several seconds)\n");
        sb.append("NOTE: Using -n optionally specifies amount to distort meaning (MAX:"+MAX_TRIALS+
            ")");
        sb.append("\n\n<b>EXAMPLE: babel Today is a great day to talk to a robot.</b>");
        return sb.toString();
    }


    /**
     * Create poetry
     *
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if(query.trim().toLowerCase().startsWith("babel")) {
            StringTokenizer st=new StringTokenizer(query, " ");

            //check for enough args
            if(st.countTokens() < 2) {
                super.sendMessage(buddy, "ERROR:\n"+help());
                return;
            }

            String imcommand=st.nextToken();

            //check for distortion modifier
            int distortion=5+(int) (Math.random()*5);
            String temp="";
            if(st.hasMoreTokens()) {
                temp=st.nextToken();
                //look for -n
                if(temp.toLowerCase().equals("-n")) {
                    //if we got -n we need an int next
                    if(st.hasMoreTokens()) {
                        temp=st.nextToken();
                        //set the distortion level (within max) or error out
                        try {
                            distortion=Integer.parseInt(temp);
                            if(distortion > MAX_TRIALS) {
                                distortion=MAX_TRIALS;
                            }

                            temp="";
                        } catch(NumberFormatException nfe) {
                            super.sendMessage(buddy, "ERROR:\n"+help());
                            return;
                        }
                    } else {
                        super.sendMessage(buddy, "ERROR:\n"+help());
                        return;
                    }
                }
            }

            //grab the rest of the message and make poetry with it
            String imcommandText=temp;
            while(st.hasMoreTokens()) {
                imcommandText=imcommandText+" "+st.nextToken();
            }

            //only send a message if there is somethign there.
            if(!imcommandText.equals("")) {
                String result=makePoetry(imcommandText, distortion);
                super.sendMessage(buddy, result);
                return;
            }
        }
    }
}

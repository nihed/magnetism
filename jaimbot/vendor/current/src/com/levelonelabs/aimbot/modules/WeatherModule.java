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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests to get weather
 *
 * @author Scott Oster
 *
 * @created March 14, 2003
 */
public class WeatherModule extends BotModule {
    static Logger logger=Logger.getLogger(WeatherModule.class.getName());
    private static ArrayList services;
    private static final String TEMP_URL="http://mobile.wunderground.com/cgi-bin/findweather/getForecast?query=";
    private static final String MAP_URL="http://www.weather.com/weather/map/";

    /**
     * Initialize the service commands.
     */
    static {
        services=new ArrayList();
        services.add("weather");
    }

    /**
     * Constructor for StockModule.
     *
     * @param bot
     */
    public WeatherModule(AIMBot bot) {
        super(bot);
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#getName()
     */
    public String getName() {
        return "Weather Module";
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
        StringBuffer sb=new StringBuffer();
        sb.append(
            "<B>weather <i>ZIPCODE</i></B> (displays the 5 day forcast for the specified zipcode)\n");
        sb.append(
            "* If the preference \"zipcode\" is set, you can omit the zipcode to use your default.");
        return sb.toString();
    }


    /**
     * Return 5 deay forecast
     *
     * @param buddy the buddy
     * @param query the weather request
     */
    public void performService(AIMBuddy buddy, String query) {
        if(query.trim().toLowerCase().startsWith("weather")) {
            StringTokenizer st=new StringTokenizer(query, " ");
            String zipPref=buddy.getPreference("zipcode");
            if((zipPref == null) && (st.countTokens() < 2)) {
                super.sendMessage(buddy, "ERROR:\n"+help());
            } else {
                String imcommand=st.nextToken();
                String zipcode="";
                if(st.hasMoreElements()) {
                    zipcode=((String) st.nextElement()).trim();
                } else if(zipPref != null) {
                    zipcode=zipPref;
                }
                String result="Couldn't get weather information";
                if(zipcode.length() == 5) {
                    WeatherScrapper scrapper=new WeatherScrapper();
                    String temp=scrapper.getWeather(zipcode);
                    if(!temp.trim().equals("")) {
                        //seemed to have gotten a decent result, so display it
                        String mapURL=WeatherModule.MAP_URL+zipcode;
                        result="Weather for zipcode <a href=\""+mapURL+"\">"+zipcode+"</a>:\n"+temp;

                        if(zipPref == null) {
                            //if the buddy didnt have a zippref set, set this one for him
                            buddy.setPreference("zipcode", zipcode);
                        }
                    }
                }
                super.sendMessage(buddy, result);
            }
        }
    }
    
    public static void main(String[] args) {
        WeatherModule mod= new WeatherModule(null);
        String zipcode = args[0];
        String result="Couldn't get weather information";
        if(zipcode.length() == 5) {
            WeatherScrapper scrapper=mod.new WeatherScrapper();
            String temp=scrapper.getWeather(zipcode);
            if(!temp.trim().equals("")) {
                //seemed to have gotten a decent result, so display it
                String mapURL=WeatherModule.MAP_URL+zipcode;
                result="Weather for zipcode <a href=\""+mapURL+"\">"+zipcode+"</a>:"+temp;
            }
        }
        System.out.println(result);
    }

    class WeatherScrapper extends ParserCallback {
        private boolean grabIt=false;
        private int count=12;
        private StringBuffer result=new StringBuffer();

        public void handleText(char[] data, int pos) {
            String temp=new String(data);
            if(temp.startsWith("Forecast as of") && (count > 0)) {
                grabIt=true;
            } else if((count > 0) && grabIt && temp.length() > 2 ) {
                if(temp.startsWith("> ")){
                    temp=temp.substring(2);
                }
                //this bolds every other line (%2 == 0 means even numbers)
                result.append((((count%2) == 0) ? "<b>" : "")+temp+
                    (((count%2) == 0) ? "</b>\n" : "\n"));
                count--;
            }
        }


        private String getResult() {
            return result.toString();
        }


        public String getWeather(String zipcode) {
            String tempResult="";
            try {
                URL url=new URL(WeatherModule.TEMP_URL+zipcode);

                //System.out.println("Scrapping URL= " + TEMP_URL);
                URLConnection conn=url.openConnection();

                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));

                ParserDelegator pd=new ParserDelegator();
                WeatherScrapper s=new WeatherScrapper();
                pd.parse(br, s, true);
                tempResult=s.getResult();
            } catch(Exception e) {
                e.printStackTrace();
                return "";
            }
            return tempResult;
        }       
    }
}

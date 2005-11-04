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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests to get ski snow reports
 * 
 * @author Scott Oster
 * @created November 23, 2003
 */
public class SkiReportModule extends BotModule {
    private static final String MOUNTAIN_PREF = "mountain";
    static Logger logger = Logger.getLogger(SkiReportModule.class.getName());
    private static ArrayList services;

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("ski");
        services.add("snow");
        services.add("mountains");
    }


    /**
     * Constructor for StockModule.
     * 
     * @param bot
     */
    public SkiReportModule(AIMBot bot) {
        super(bot);
    }


    /**
     * @see com.levelonelabs.aimbot.BotModule#getName()
     */
    public String getName() {
        return "Ski Report Module";
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
        StringBuffer sb = new StringBuffer();
        sb
            .append("<B>mountains <i>STATE ABBREVIATION</i></B> (displays the resort codes for the specified state code)\n");
        sb.append("<B>ski <i>STATE ABBREVIATION:RESORT CODE</i></B> (displays the ski reports for the given resort)\n");
        sb
            .append("* If the preference \"mountain\" is set, you can omit the [STATE ABBREVIATION:RESORT CODE] to use your default.");
        return sb.toString();
    }


    /**
     * Return snow report
     * 
     * @param buddy
     *            the buddy
     * @param query
     *            the weather request
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.trim().toLowerCase().startsWith("snow") || query.trim().toLowerCase().startsWith("ski")) {
            StringTokenizer st = new StringTokenizer(query, " ");
            String mntPref = buddy.getPreference(MOUNTAIN_PREF);
            if ((mntPref == null) && (st.countTokens() < 2)) {
                super.sendMessage(buddy, "ERROR:\n" + help());
            } else {
                String imcommand = st.nextToken();
                String state = "";
                String resortCode = "";
                String code = "";
                if (st.hasMoreElements()) {
                    code = ((String) st.nextElement()).trim();
                } else if (mntPref != null) {
                    code = mntPref;
                }

                int ind = code.indexOf(":");
                if (ind <= 0) {
                    super.sendMessage(buddy, "ERROR: must use [STATE ABBREVIATION:RESORT CODE] format (e.g. CO:77)\n"
                        + help());
                    return;
                } else {
                    state = code.substring(0, ind).toUpperCase();
                    resortCode = code.substring(ind + 1);
                }
                String result = "Couldn't get ski resort information.";
                if (!resortCode.trim().equals("") && !state.trim().equals("")) {
                    SkiScraper scrapper = new SkiScraper(state, resortCode);
                    SnowReport temp = scrapper.getSnowReport();
                    if (temp != null) {
                        result = temp.getTitle() + "\n" + "<b>New Snow:</b> " + temp.getNewSnow() + "  \t"
                            + "<b>Surface:</b> " + temp.getSurfaceSnow() + "  \t" + "<b>Base:</b> "
                            + temp.getBaseDepth() + "\n" + "<b>Open Runs:</b> " + temp.getRunsOpen() + "  \t"
                            + "<b>Open Lifts:</b> " + temp.getLiftsOpen() + "  \t" + "<b>Open Acres:</b> "
                            + temp.getAcresOpen();

                        if (mntPref == null) {
                            // if the buddy didnt have a mountain perference
                            // set, set this one for him
                            buddy.setPreference(MOUNTAIN_PREF, state + ":" + resortCode);
                        }
                    }
                }
                super.sendMessage(buddy, result);
            }
        } else if (query.trim().toLowerCase().startsWith("mountains")) {
            StringTokenizer st = new StringTokenizer(query, " ");
            if (st.countTokens() == 2) {
                String imcommand = st.nextToken();
                String state = st.nextToken().toUpperCase();

                String result = "Couldn't get locate ski resorts in " + state;
                if (state.length() == 2) {
                    MountainScraper scrapper = new MountainScraper(state);
                    Map mntMap = scrapper.getMountains();
                    if (mntMap.size() > 0) {
                        result = "Ski Resort codes for " + state + ":\n";
                        for (Iterator iter = mntMap.keySet().iterator(); iter.hasNext();) {
                            String key = (String) iter.next();
                            String value = (String) mntMap.get(key);
                            logger.fine(value + "\t use --> " + state + ":" + key);
                            result += (value + "\t --> <b>" + state + ":" + key + "</b>\n");
                        }
                    }
                }
                super.sendMessage(buddy, result);
            } else {
                super.sendMessage(buddy, help());
            }
        }
    }

}

class MountainScraper extends ParserCallback {
    static Logger logger = Logger.getLogger(TVListingsModule.class.getName());
    private static final String BASE_URL = "http://www.onthesnow.com/";
    private String state;
    private List codes;
    private List names;
    private boolean validText = false;
    private boolean validLink = false;
    private boolean grabURLText = false;


    /**
     * Contructor for TVResultsScraper
     * 
     * @param state
     */
    public MountainScraper(String state) {
        this.state = state.toUpperCase();
        this.codes = new ArrayList();
        this.names = new ArrayList();
    }


    /**
     * Look for resort name and code
     * 
     * @param t
     *            select or option tag
     * @param a
     *            looks for name
     * @param pos
     *            not used
     */
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t == HTML.Tag.A && validLink) {
            String urlString = a.getAttribute(HTML.Attribute.HREF).toString();
            int ind = urlString.indexOf(BASE_URL + this.state);
            if (ind >= 0) {
                try {
                    String temp = urlString.substring(ind + (BASE_URL + this.state + "/").length());
                    String code = temp.substring(ind, temp.indexOf("/"));
                    if (!codes.contains(code)) {
                        codes.add(code);
                    }
                    grabURLText = true;
                } catch (Exception e) {
                    logger.fine("Parser didn't handle parsing [" + urlString + "] cleanly:" + e.getMessage());
                }
            }
        }
    }


    /**
     * Looks for the end of table
     * 
     * @param t
     *            table
     * @param pos
     *            unused
     */
    public void handleEndTag(Tag t, int pos) {
        if (t == HTML.Tag.TABLE && validText) {
            validText = false;
        }
    }


    /**
     * Grab the resorts when appropriate
     * 
     * @param data
     *            the text
     * @param pos
     *            unused
     */
    public void handleText(char[] data, int pos) {
        if (grabURLText == true) {
            String name = new String(data).trim();
            if (!name.startsWith("Don't see the resort")) {
                names.add(name);
            }
            grabURLText = false;
        } else {
            String line = new String(data).trim();
            // ignore the time zone providers
            if (line.equals("Resort Name")) {
                validLink = true;
            }
        }
    }


    /**
     * Uses the parser to return a map of appropriate providers
     * 
     * @return map of providers keyed by codes
     */
    public Map getMountains() {
        HashMap result = new HashMap();
        try {
            URL url = new URL(BASE_URL + this.state + "/index.html");
            logger.fine("Looking for mountains in:" + state + " using URL= " + url);
            URLConnection conn = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ParserDelegator pd = new ParserDelegator();
            pd.parse(br, this, true);
            if (codes.size() == names.size()) {
                for (int i = 0; i < codes.size(); i++) {
                    result.put(codes.get(i), names.get(i));
                }
            }
        } catch (Exception e) {
            logger.fine("Error looking for mountains, ERROR:" + e.getStackTrace());
            return new HashMap();
        }
        return result;
    }


    /**
     * Testing method
     * 
     * @param args
     *            state
     */
    public static void main(String[] args) {
        String state = args[0];
        MountainScraper scrapper = new MountainScraper(state);
        Map mounts = scrapper.getMountains();
        for (Iterator iter = mounts.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String value = (String) mounts.get(key);
            System.out.println(value + "\t use --> " + key);
        }
    }
}

class SkiScraper extends ParserCallback {
    static Logger logger = Logger.getLogger(SkiScraper.class.getName());
    private static final String BASE_URL = "http://www.onthesnow.com/";
    private static final String URL_END = "skireport.html";

    boolean grabTitle = false;

    private static final int WAIT_STATE = 0;
    private static final int NEW_SNOW_STATE = 1;
    private static final int BASE_DEPTH_STATE = 2;
    private static final int SURFACE_SNOW_STATE = 3;
    private static final int RUNS_OPEN_STATE = 4;
    private static final int LIFTS_OPEN_STATE = 5;
    private static final int ACRES_OPEN_STATE = 6;

    private int machineState = WAIT_STATE;
    private int statesVisited = 0;

    //private static final String NEW_SNOW_MARKER = "New Snow:";
    private static final String NEW_SNOW_MARKER = "(past 48hr):";
    private static final String BASE_DEPTH_MARKER = "Base Depth:";
    private static final String SURFACE_SNOW_MARKER = "Surface Snow:";
    private static final String RUNS_OPEN_MARKER = "Runs Open:";
    private static final String LIFTS_OPEN_MARKER = "Lifts Open:";
    private static final String ACRES_OPEN_MARKER = "Acres Open:";

    private SnowReport result;
    private String resortCode;
    private String state;


    /**
     * Contructor for SkiScraper
     * 
     * @param url
     * @param show
     */
    public SkiScraper(String state, String resortCode) {
        this.result = new SnowReport(state);
        this.state = state;
        this.resortCode = resortCode;
    }


    /**
     * Looks for start of links, paragraphs and HRs
     * 
     * @param t
     *            the tag
     * @param a
     *            unused
     * @param pos
     *            unused
     */
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if (t == HTML.Tag.TITLE) {
            grabTitle = true;
        }
    }


    /**
     * Grabs the show info
     * 
     * @param data
     *            the text
     * @param pos
     *            unused
     */
    public void handleText(char[] data, int pos) {
        String line = new String(data).trim();
        if (grabTitle) {
            int ind = line.indexOf(":");
            if (ind > 0) {
                line = line.substring(0, ind);
            }
            this.result.setTitle(line);
            grabTitle = false;
        } else if (line.equals(ACRES_OPEN_MARKER)) {
            machineState = ACRES_OPEN_STATE;
            statesVisited++;
        } else if (line.equals(BASE_DEPTH_MARKER)) {
            machineState = BASE_DEPTH_STATE;
            statesVisited++;
        } else if (line.equals(LIFTS_OPEN_MARKER)) {
            machineState = LIFTS_OPEN_STATE;
            statesVisited++;
        } else if (line.equals(NEW_SNOW_MARKER)) {
            machineState = NEW_SNOW_STATE;
            statesVisited++;
        } else if (line.equals(RUNS_OPEN_MARKER)) {
            machineState = RUNS_OPEN_STATE;
            statesVisited++;
        } else if (line.equals(SURFACE_SNOW_MARKER)) {
            machineState = SURFACE_SNOW_STATE;
            statesVisited++;
        } else if (machineState != WAIT_STATE) {
            // read the machine
            switch (machineState) {
                case ACRES_OPEN_STATE :
                    result.setAcresOpen(line);
                    machineState = WAIT_STATE;
                    break;
                case BASE_DEPTH_STATE :
                    result.setBaseDepth(line);
                    machineState = WAIT_STATE;
                    break;
                case LIFTS_OPEN_STATE :
                    result.setLiftsOpen(line);
                    machineState = WAIT_STATE;
                    break;
                case NEW_SNOW_STATE :
                    result.setNewSnow(line);
                    machineState = WAIT_STATE;
                    break;
                case RUNS_OPEN_STATE :
                    result.setRunsOpen(line);
                    machineState = WAIT_STATE;
                    break;
                case SURFACE_SNOW_STATE :
                    result.setSurfaceSnow(line);
                    machineState = WAIT_STATE;
                    break;
            }

        }

    }


    /**
     * Grabs the summary info
     * 
     * @return The text
     */
    public SnowReport getSnowReport() {
        URL url = null;
        try {
            url = new URL(BASE_URL + state + "/" + resortCode + "/" + URL_END);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        logger.fine("Looking for Snow report for resort " + resortCode + " in state" + state + " using URL= " + url);
        SnowReport tempResult;
        try {
            URLConnection conn = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ParserDelegator pd = new ParserDelegator();
            pd.parse(br, this, true);

            tempResult = result;
            if (statesVisited != 6) {
                tempResult = null;
            }
        } catch (Exception e) {
            // e.printStackTrace();
            logger.severe(e.toString());
            return null;
        }

        return tempResult;
    }


    /**
     * Testing method
     * 
     * @param args
     *            state
     */
    public static void main(String[] args) {
        String state = args[0];
        String code = args[1];
        SkiScraper scrapper = new SkiScraper(state, code);
        SnowReport report = scrapper.getSnowReport();
        System.out.println(report);
    }

}
class SnowReport {
    private String title;
    private String newSnow;
    private String baseDepth;
    private String surfaceSnow;
    private String runsOpen;
    private String liftsOpen;
    private String acresOpen;
    private String state;


    public SnowReport(String state) {
        this.state = state;
    }


    /**
     * @return
     */
    public String getAcresOpen() {
        return acresOpen;
    }


    /**
     * @return
     */
    public String getBaseDepth() {
        return baseDepth;
    }


    /**
     * @return
     */
    public String getLiftsOpen() {
        return liftsOpen;
    }


    /**
     * @return
     */
    public String getNewSnow() {
        return newSnow;
    }


    /**
     * @return
     */
    public String getRunsOpen() {
        return runsOpen;
    }


    /**
     * @return
     */
    public String getState() {
        return state;
    }


    /**
     * @return
     */
    public String getSurfaceSnow() {
        return surfaceSnow;
    }


    /**
     * @return
     */
    public String getTitle() {
        return title;
    }


    /**
     * @param string
     */
    public void setAcresOpen(String string) {
        acresOpen = string;
    }


    /**
     * @param string
     */
    public void setBaseDepth(String string) {
        baseDepth = string;
    }


    /**
     * @param string
     */
    public void setLiftsOpen(String string) {
        liftsOpen = string;
    }


    /**
     * @param string
     */
    public void setNewSnow(String string) {
        newSnow = string;
    }


    /**
     * @param string
     */
    public void setRunsOpen(String string) {
        runsOpen = string;
    }


    /**
     * @param string
     */
    public void setState(String string) {
        state = string;
    }


    /**
     * @param string
     */
    public void setSurfaceSnow(String string) {
        surfaceSnow = string;
    }


    /**
     * @param string
     */
    public void setTitle(String string) {
        title = string;
    }


    public String toString() {
        return title + ":\n" + "New Snow: " + newSnow + "\t\t" + "Surface: " + surfaceSnow + "\t\t" + "Base: "
            + baseDepth + "\n" + "Open Runs: " + runsOpen + "\t\t" + "Open Lifts: " + liftsOpen + "\t\t"
            + "Open Acres: " + acresOpen;
    }
}
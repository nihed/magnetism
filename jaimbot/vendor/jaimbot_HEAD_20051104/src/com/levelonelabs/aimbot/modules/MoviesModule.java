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

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Handles requests to get movie times based on zip and/or theater and lists theaters by zip code.
 *
 * @author John G Ledo <A HREF="mailto:john@imssoft.com">john@imssoft.com</A>
 *
 * @created October 3, 2003
 */
public class MoviesModule extends BotModule {
    static Logger logger=Logger.getLogger(MoviesModule.class.getName());
    private static ArrayList services;

    /**
     * Initialize the service commands.
     */
    static {
        services=new ArrayList();
        services.add("movies");
        services.add("movieslegend");
        services.add("theaters");
    }

    /**
     * Constructor for MoviesModule.
     *
     * @param bot
     */
    public MoviesModule(AIMBot bot) {
        super(bot);
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#getName()
     */
    public String getName() {
        return "Movies Module";
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
        sb.append("<B>theaters <i>ZIPCODE<i></B> (displays a list of theaters in your area)\n");
        sb.append(
            "* If the preference \"zipcode\" is set, you can omit the zipcode to use your default.\n");
        sb.append(
            "<B>movies <i>ZIPCODE [Theater Number]</i></B> (displays movie times for the theater closest to your zipcode if a theater number is not provided)\n");
        sb.append(
            "* If the preference \"zipcode\" is set, you can omit the zipcode to use your default.\n");
        sb.append("<B>movieslegend</B> (displays the legend for movie times)");
        return sb.toString();
    }


    /**
     * Grab the movie times (or list the theater not done)
     *
     * @param buddy the buddy
     * @param query the request
     */
    public void performService(AIMBuddy buddy, String query) {
        StringTokenizer st=new StringTokenizer(query, " ");
        String zipPref=buddy.getPreference("zipcode");
        String zipMovie=buddy.getPreference("movieszipcode");
        String imcommand=st.nextToken();
        String zipcode="";
        String theatercode="";

        if(imcommand.trim().equalsIgnoreCase("MOVIES")) {
            /* If there are three tokens then it's command, zip, theater.
             * If there are only two then the second can either be theater
             * or zip code (I check by length).  If the second is a theater
             * then I first use the zip from the theater search done to get
             * that number.  If there is no zip, because they went of memeory
             * or used their default then I use the zipcode pref.  If there
             * are no tocken then I only use the zipcode pref not the zip
             * saved if they did a theater search (bad? not sure).
             */
            if(st.countTokens() == 2) {
                zipcode=((String) st.nextElement()).trim();
                theatercode=((String) st.nextElement()).trim();
            } else {
                if(st.hasMoreElements()) {
                    zipcode=((String) st.nextElement()).trim();
                    if(zipcode.length() == 1) {
                        theatercode=zipcode;
                        zipcode=zipMovie;
                        if((zipcode == null) || (zipcode.length() != 5)) {
                            zipcode=zipPref;
                        }
                    }
                } else if(zipPref != null) {
                    zipcode=zipPref;
                }
            }

            if( zipcode!=null && zipcode.length() == 5) {
                MovieTimesScraper scrapper;
                if(theatercode.length() == 1) {
                    scrapper=new MovieTimesScraper(zipcode, theatercode);
                } else {
                    scrapper=new MovieTimesScraper(zipcode);
                }
                if(zipPref == null) {
                    //if the buddy didnt have a zippref set, set this one for him
                    buddy.setPreference("zipcode", zipcode);
                }
                Object[] list=scrapper.getTimes().toArray();
                if((list.length < 1) || (((String) list[0]).length() < 1)) {
                    super.sendMessage(buddy, "Could not find movie times.");
                    return;
                }
                for(int i=0; i < list.length; i++) {
                    logger.fine("Sending movie message #"+i);
                    super.sendMessage(buddy, ((String) list[i]));
                }
            } else {
                super.sendMessage(buddy, "ERROR:\n"+help());
            }
        } else if(imcommand.trim().equalsIgnoreCase("THEATERS")) {
            if(st.hasMoreElements()) {
                zipcode=((String) st.nextElement()).trim();
            } else if(zipPref != null) {
                zipcode=zipPref;
            }
            if(zipcode.length() == 5) {
                buddy.setPreference("movieszipcode", zipcode);
                MovieTheaterScraper scrapper=new MovieTheaterScraper(zipcode);
                if(zipPref == null) {
                    //If the buddy didnt have a zippref set, set this one for him
                    buddy.setPreference("zipcode", zipcode);
                }
                String list=scrapper.getTheaters();
                if((list == null) || (list.length() < 1)) {
                    super.sendMessage(buddy, "Could not find theaters.");
                } else {
                    list="<B>Theaters for "+zipcode+"</B>\n"+list;
                    list=list+
                        "\n\nSend <b>movies <i>theaternumber</i></b> to obtain movie times for that theater.\n";
                    super.sendMessage(buddy, list);
                }
            } else {
                super.sendMessage(buddy, "ERROR:\n"+help());
            }
        } else if(imcommand.trim().equalsIgnoreCase("MOVIESLEGEND")) {
            super.sendMessage(buddy,
                "<b>Legend:</b>\n"+"( ) - discounted / bargain shows.\n"+
                "* - Sony Digital Dynamic Sound (SDDS)\n"+"** - Digital Theater Systems (DTS)\n"+
                "*** - Dolby Digital Surround EX\n"+"**** - Dolby Digital\n"+
                "***** - Dolby Stereo\n");
        }
    }
}



class MovieTimesScraper extends ParserCallback {
    static Logger logger=Logger.getLogger(MoviesModule.class.getName());
    private String urlPart="http://movies.yahoo.com/showtimes/showtimes.html?m=&t=&a=&s=tm&r=sim&p=0&get=Get+Showtimes&z=";
    private String zipcode;
    private String line;
    private int stage;
    private String theater;
    private String theaterNext;
    private boolean areLinkTimes;

    // Results are kept in ArrayList because they tend to exceed
    // AOL's buffer size so we have to send a couple of posts
    private ArrayList result;

    /**
     * Contructor for MovieTimesScraper
     *
     * @param zipcode
     */
    public MovieTimesScraper(String zipcode) {
        this.zipcode=zipcode;
        this.result=new ArrayList();
        this.line="";
        this.theater="0";
        this.theaterNext="1";
        this.stage=0;
        this.areLinkTimes=false;
    }


    /**
     * Contructor for MovieTimesScraper
     *
     * @param zipcode
     * @param theater
     */
    public MovieTimesScraper(String zipcode, String theater) {
        this.zipcode=zipcode;
        this.result=new ArrayList();
        this.line="";
        this.stage=0;
        this.areLinkTimes=false;
        try {
            int num=Integer.parseInt(theater);
            this.theater=theater;
            this.theaterNext=String.valueOf((num+1));
        } catch(Exception ex) {
            this.theater="0";
            this.theaterNext="1";
        }
    }

    /**
     * Looks for link 'T0' for start of first theater.  Then scrape text until T1 link.
     *
     * @param t the tag
     * @param a unused
     * @param pos unused
     */
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if((t == HTML.Tag.A) && (a.getAttribute(HTML.Attribute.NAME) != null) &&
                a.getAttribute(HTML.Attribute.NAME).equals("T"+theater)) {
            stage=1;
        } else if((t == HTML.Tag.A) && (stage == 1)) {
            stage=2;
        } else if((t == HTML.Tag.A) && (a.getAttribute(HTML.Attribute.NAME) != null) &&
                a.getAttribute(HTML.Attribute.NAME).equals("T"+theaterNext)) {
            stage=5;
        }
    }


    /**
     * Looks for links
     *
     * @param t A tag
     * @param pos unused
     */
    public void handleEndTag(Tag t, int pos) {
        if((t == HTML.Tag.A) && (stage == 2)) {
            stage=3;
        } else if((t == HTML.Tag.A) && (stage == 3)) {
            stage=4;
        } else if((t == HTML.Tag.HTML) && (line.length() > 0)) {
            // End of document so add last line to the arraylist
            result.add(line);
            line="";
        } else if(t == HTML.Tag.TD) {
            if(areLinkTimes) {
                areLinkTimes=false;
                line=line+"\n";
            }
            if(stage == 4) {
                stage=3;
            }
        }
    }


    /**
     * Grabs the theater text
     *
     * @param data the text
     * @param pos unused
     */
    public void handleText(char[] data, int pos) {
        String temp=new String(data);
        if(stage == 2 && !temp.equals("Add to My Favorite Theaters")) {
            line="\n<B>"+temp+"</B>\n";
        } else if((stage == 3) && (temp.length() > 1) &&
                (temp.toUpperCase().indexOf("CLICK") == -1) &&
                (temp.toUpperCase().indexOf(" ]") == -1) &&
                (temp.toUpperCase().indexOf(" |") == -1) &&
                (temp.toUpperCase().indexOf("THEATER INFO") == -1) &&
                (temp.toUpperCase().indexOf("MAP IT") == -1)) {
            if(temp.toUpperCase().indexOf("BUY TICKETS") != -1) {
                /* Some theaters sell ticket online so each time is a link
                 * instead of one long string. Flag is set to create the
                 * one string.
                 */
                areLinkTimes=true;
            } else {
                if((line.length()+data.length) > 950) {
                    result.add(line);
                    line="<B>"+temp+"</B>";
                } else {
                    line=line+"<B>"+temp+"</B>";
                }
                if(!areLinkTimes) {
                    line=line+"\n";
                }
            }
        } else if((stage == 4) && (temp.length() > 1) &&
                (temp.toUpperCase().indexOf("CLICK") == -1) &&
                (temp.toUpperCase().indexOf(" |") == -1) &&
                (temp.toUpperCase().indexOf("THEATER INFO") == -1) &&
                (temp.toUpperCase().indexOf("MAP IT") == -1)) {
            if(temp.toUpperCase().indexOf("BUY TICKETS") != -1) {
                /* Some theaters sell ticket online so each time is a link
                 * instead of one long string. Flag is set to create the
                 * one string.
                 */
                areLinkTimes=true;
            } else {
                if((line.length()+data.length) > 950) {
                    result.add(line);
                    line=temp;
                } else {
                    line=line+temp;
                }
                if(!areLinkTimes) {
                    line=line+"\n";
                }
            }
        }
    }


    /**
     * Grabs the movie times
     *
     * @return The text
     */
    public ArrayList getTimes() {
        try {
            URL url=new URL(urlPart+zipcode);
            logger.fine("Looking for movies for: "+zipcode+" using URL="+url);
            URLConnection conn=url.openConnection();
            BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ParserDelegator pd=new ParserDelegator();
            pd.parse(br, this, true);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return this.result;
    }
}



class MovieTheaterScraper extends ParserCallback {
    static Logger logger=Logger.getLogger(MoviesModule.class.getName());
    private String urlPart="http://movies.yahoo.com/showtimes/showtimes.html?m=&t=&a=&s=tm&r=sim&p=0&get=Get+Showtimes&z=";
    private String zipcode;
    private String result;
    private String line;
    private boolean startA=false;
    private boolean createList=true;

    /**
     * Contructor for MovieTimesScraper
     *
     * @param zipcode
     */
    public MovieTheaterScraper(String zipcode) {
        this.zipcode=zipcode;
        this.result="";
        this.line="";
    }

    /**
     * Looks for links 'T0' to 'T9' and creates a list
     *
     * @param t the tag
     * @param a unused
     * @param pos unused
     */
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        if((a.getAttribute(HTML.Attribute.HREF) != null) &&
                a.getAttribute(HTML.Attribute.HREF).equals("#T10")) {
            createList=false;
        }

        if((t == HTML.Tag.A) && (a.getAttribute(HTML.Attribute.HREF) != null) &&
                (((String) a.getAttribute(HTML.Attribute.HREF)).startsWith("#T")) && createList) {
            startA=true;
            line=a.getAttribute(HTML.Attribute.HREF)+" ";
        }
    }


    /**
     * Looks for links
     *
     * @param t A tag
     * @param pos unused
     */
    public void handleEndTag(Tag t, int pos) {
        if((t == HTML.Tag.A) && startA) {
            startA=false;
        }
    }


    /**
     * Grabs the theater text
     *
     * @param data the text
     * @param pos unused
     */
    public void handleText(char[] data, int pos) {
        if(createList && startA) {
            result=result+line.substring(2)+": "+new String(data)+"\n";
        }
    }


    /**
     * Grabs a list of theaters
     *
     * @return The text
     */
    public String getTheaters() {
        try {
            URL url=new URL(urlPart+zipcode);
            logger.fine("Looking for theaters for: "+zipcode+" using URL="+url);
            URLConnection conn=url.openConnection();
            BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ParserDelegator pd=new ParserDelegator();
            pd.parse(br, this, true);
        } catch(Exception e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }
}

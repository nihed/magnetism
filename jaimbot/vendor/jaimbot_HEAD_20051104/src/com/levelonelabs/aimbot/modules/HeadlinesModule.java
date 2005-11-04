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
 * Module to read headlines using RSS
 *
 * @created Nov 6, 2002
 */
package com.levelonelabs.aimbot.modules;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;
import com.levelonelabs.rdf.Channel;
import com.levelonelabs.rdf.Item;


/**
 * Handles requests to get headlines
 *
 * @author Scott Oster
 *
 * @created Jan 15, 2003
 */
public class HeadlinesModule extends BotModule {
    private static ArrayList services;
    private static final int MAX_NUM_HEADLINES=5;
    private static Hashtable channels;

    /**
     * Initialize the service commands.
     */
    static {
        services=new ArrayList();
        channels=new Hashtable();
        String channelName;
        Channel channel;

        channelName="slashdot";
        channel=new Channel(channelName, "http://slashdot.org/slashdot.rdf");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="ars";
        channel=new Channel(channelName, "http://arstechnica.com/etc/rdf/ars.rdf");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="cnn";
        channel=new Channel(channelName, "http://www.newsisfree.com/HPE/xml/feeds/15/2315.xml");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="wired";
        channel=new Channel(channelName, "http://www.wired.com/news_drop/netcenter/netcenter.rdf");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="zdnet";
        channel=new Channel(channelName, "http://p.moreover.com/cgi-local/page?feed=129&o=rss");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="word";
        channel=new Channel(channelName, "http://dictionary.reference.com/wordoftheday/wotd.rss");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="jsurf";
        channel=new Channel(channelName, "http://www.jsurfer.org/backend.php");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="robots";
        channel=new Channel(channelName, "http://robots.net/rss/articles.xml");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="kuro";
        channel=new Channel(channelName, "http://www.kuro5hin.org/backend.rdf");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="fark";
        channel=new Channel(channelName, "http://www.newsisfree.com/HPE/xml/feeds/17/1717.xml");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="jworld";
        channel=new Channel(channelName, "http://www.newsisfree.com/HPE/xml/feeds/59/959.xml");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="mets";
        channel=new Channel(channelName, "http://p.moreover.com/cgi-local/page?feed=559&o=rss");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="islanders";
        channel=new Channel(channelName, "http://p.moreover.com/cgi-local/page?feed=336&o=rss");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="nfl";
        channel=new Channel(channelName, "http://www.newsisfree.com/HPE/xml/feeds/52/1852.xml");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="deals";
        channel=new Channel(channelName, "http://www.totaldeals.com/rss/new.asp");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);

        channelName="cnet";
        channel=new Channel(channelName, "http://www.moreover.com/cgi-local/page?feed=139&o=rss");
        services.add(channelName);
        channels.put(channelName.toLowerCase(), channel);
    }

    /**
     * Constructor for HeadlinesModule.
     *
     * @param bot
     */
    public HeadlinesModule(AIMBot bot) {
        super(bot);
    }

    /**
     * @see com.levelonelabs.aimbot.BotModule#getName()
     */
    public String getName() {
        return "Headlines Module";
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
        sb.append("Type one of the following site names to see the latest headlines:\n");
        for(Enumeration enumer=channels.keys(); enumer.hasMoreElements();) {
            sb.append("<B>"+enumer.nextElement()+"</B>     ");
        }
        return sb.toString();
    }


    /**
     * @see com.levelonelabs.aimbot.BotModule#performService(AIMBuddy, String)
     */
    public void performService(AIMBuddy buddy, String query) {
        String channelName=query.trim().toLowerCase();
        Channel chan=(Channel) channels.get(channelName);
        if(chan != null) {
            chan.update();
            String results="";
            if(chan.getErrorMessage() != null) {
                super.sendMessage(buddy, "\nERROR["+chan.getErrorMessage()+"]");
                results="Cached Results:";
            }
            ArrayList arr=chan.getItems();
            if(arr.size() > 0) {
                for(int i=0; (i < arr.size()) && (i < HeadlinesModule.MAX_NUM_HEADLINES); i++) {
                    Item item=(Item) arr.get(i);
                    results+=("<BR>"+item);
                }
                super.sendMessage(buddy, results);
            }
        } else {
            super.sendMessage(buddy, "ERROR:\n"+help());
        }
    }
}

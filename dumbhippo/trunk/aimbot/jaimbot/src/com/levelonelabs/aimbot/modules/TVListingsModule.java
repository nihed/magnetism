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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
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
 * Handles requests to get tv episode summaries for a particular show. This is
 * extremely brittle, and should be considered expiremental.
 * 
 * @author Scott Oster
 * 
 * @todo: find the "best" matching show and use that instead of just the first
 *        one.
 * 
 * @created March 25, 2003
 */
public class TVListingsModule extends BotModule {
	static Logger logger = Logger.getLogger(TVListingsModule.class.getName());
	private static ArrayList services;

	/**
	 * Initialize the service commands.
	 */
	static {
		services = new ArrayList();
		services.add("tv");
		services.add("tvlist");
	}


	/**
	 * Constructor for TVListingsModule.
	 * 
	 * @param bot
	 */
	public TVListingsModule(AIMBot bot) {
		super(bot);
	}


	/**
	 * @see com.levelonelabs.aimbot.BotModule#getName()
	 */
	public String getName() {
		return "TV Listings Module";
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
		sb.append("<B>tvlist <i>ZIPCODE</i></B> (displays the tv providers in the area of the zipcode)\n");
		sb.append("* If the preference \"zipcode\" is set, you can omit the zipcode to use your default.\n");
		sb
			.append("<B>tv <i>SHOW<i></B> (displays the synopsis of the next upcomming episode of the specified show in your area)\n");
		sb.append("* The preference \"tvprovider\" must be set!");
		return sb.toString();
	}


	/**
	 * Grab show detail, or list the providers
	 * 
	 * @param buddy
	 *            the buddy
	 * @param query
	 *            the request
	 */
	public void performService(AIMBuddy buddy, String query) {
		if (query.trim().toLowerCase().startsWith("tvlist")) {
			StringTokenizer st = new StringTokenizer(query, " ");
			String zipPref = buddy.getPreference("zipcode");
			if ((zipPref == null) && (st.countTokens() < 2)) {
				super.sendMessage(buddy, "ERROR:\n" + help());
			} else {
				String imcommand = st.nextToken();
				String zipcode = "";
				if (st.hasMoreElements()) {
					zipcode = ((String) st.nextElement()).trim();
				} else if (zipPref != null) {
					zipcode = zipPref;
				}
				String result = "Couldn't get provider list.";
				if (zipcode.length() == 5) {
					TVProviderScraper scrapper = new TVProviderScraper(zipcode);
					Map providers = scrapper.getProviders();

					if (providers.size() > 0) {
						result = "Set the preference <b>tvprovider</b> to your providers code.\n";
						//seemed to have gotten a decent result, so display it
						for (Iterator iter = providers.keySet().iterator(); iter.hasNext();) {
							String key = (String) iter.next();
							String value = (String) providers.get(key);
							logger.fine(value + "\t use --> " + key);
							result += (value + "\t --> <b>" + key + "</b>\n");
						}
						if (zipPref == null) {
							//if the buddy didnt have a zippref set, set this
							// one for him
							buddy.setPreference("zipcode", zipcode);
						}
					}
				}
				super.sendMessage(buddy, result);
			}
		} else if (query.trim().toLowerCase().startsWith("tv")) {
			StringTokenizer st = new StringTokenizer(query, " ");
			String provider = buddy.getPreference("tvprovider");
			if ((provider == null) || provider.trim().equals("")) {
				String message = "ERROR: You must set the <b>tvprovider</b> preference first!\n";
				message += "See help for the TV Module and the Preference Module.";
				super.sendMessage(buddy, message);
			} else if (st.countTokens() < 2) {
				super.sendMessage(buddy, "ERROR:\n" + help());
			} else {
				String imcommand = st.nextToken();
				String searchString = "";
				while (st.hasMoreElements()) {
					searchString += (((String) st.nextElement()).trim() + " ");
				}

				TVListingsScraper listings = new TVListingsScraper(searchString, provider);
				ShowDetail showDetail = listings.getShowDetail();

				String result = "Unable to find show information for " + searchString;

				if ((showDetail != null) && (showDetail.getUrl() != null) && (showDetail.getShow() != null)) {
					TVResultsScraper scrapper = new TVResultsScraper(showDetail.getUrl(), showDetail.getShow());
					String summary = scrapper.getSummary();
					String channelInfo = "";
					if (showDetail.getChannel() != null) {
						channelInfo = showDetail.getChannel() + " ";
					}
					if (summary.trim().equals("")) {
						result = "<a href=\"" + showDetail.getUrl() + "\">" + showDetail.getShow() + "</a>";
					} else {
						result = showDetail.getShow() + "\n" + channelInfo + summary;
					}
				}

				super.sendMessage(buddy, result);
			}
		}
	}
}

class TVProviderScraper extends ParserCallback {
	static Logger logger = Logger.getLogger(TVListingsModule.class.getName());
	private static final String BASE_URL = "http://tv.yahoo.com/lineup?co=us&.intl=us&zip=";
	private String zipcode;
	private List codes;
	private List names;
	private boolean validSelect = false;
	private boolean validText = false;


	/**
	 * Contructor for TVResultsScraper
	 * 
	 * @param zipcode
	 */
	public TVProviderScraper(String zipcode) {
		this.zipcode = zipcode;
		this.codes = new ArrayList();
		this.names = new ArrayList();
	}


	/**
	 * Look for "lineup" dropdown and its values
	 * 
	 * @param t
	 *            select or option tag
	 * @param a
	 *            looks for name
	 * @param pos
	 *            not used
	 */
	public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
		if ((t == HTML.Tag.SELECT) && (a.getAttribute(HTML.Attribute.NAME) != null)
			&& a.getAttribute(HTML.Attribute.NAME).equals("lineup")) {
			validSelect = true;
		} else if ((t == HTML.Tag.OPTION) && validSelect && (a.getAttribute(HTML.Attribute.VALUE) != null)) {
			String code = a.getAttribute(HTML.Attribute.VALUE).toString();
			codes.add(code);
			validText = true;
		}
	}


	/**
	 * Looks for the end of select and option tags
	 * 
	 * @param t
	 *            select or option
	 * @param pos
	 *            unused
	 */
	public void handleEndTag(Tag t, int pos) {
		if (t == HTML.Tag.SELECT) {
			validSelect = false;
		} else if (t == HTML.Tag.OPTION) {
			validText = false;
		}
	}


	/**
	 * Grab the providers when appropriate
	 * 
	 * @param data
	 *            the text
	 * @param pos
	 *            unused
	 */
	public void handleText(char[] data, int pos) {
		if (validText) {
			String line = new String(data).trim();

			//ignore the time zone providers
			if ((line.indexOf("Time Zone") == -1) && (line.indexOf("None") == -1)) {
				names.add(line);
			} else {
				codes.remove(codes.size() - 1);
			}
		}
	}


	/**
	 * Uses the parser to return a map of appropriate providers
	 * 
	 * @return map of providers keyed by codes
	 */
	public Map getProviders() {
		HashMap result = new HashMap();
		try {
			URL url = new URL(BASE_URL + this.zipcode);
			logger.fine("Looking for prividers for:" + zipcode + " using URL= " + url);
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
			e.printStackTrace();
			return new HashMap();
		}
		return result;
	}


	/**
	 * Testing method
	 * 
	 * @param args
	 *            zipcode
	 */
	public static void main(String[] args) {
		String zip = args[0];
		TVProviderScraper scrapper = new TVProviderScraper(zip);
		Map providers = scrapper.getProviders();
		for (Iterator iter = providers.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String value = (String) providers.get(key);
			System.out.println(value + "\t use --> " + key);
		}
	}
}

class TVResultsScraper extends ParserCallback {
	static Logger logger = Logger.getLogger(TVListingsModule.class.getName());
	private URL url;
	private String show;
	boolean useText = false;
	boolean inLink = false;
	boolean ignore = false;
	boolean foundStart = false;
	boolean foundEnd = false;
	int showCount = 2;
	private StringBuffer result;


	/**
	 * Contructor for TVResultsScraper
	 * 
	 * @param url
	 * @param show
	 */
	public TVResultsScraper(URL url, String show) {
		this.url = url;
		this.show = show;
		this.result = new StringBuffer();
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
		if (t == HTML.Tag.HR) {
			useText = false;
		} else if (t == HTML.Tag.A) {
			inLink = true;
		} else if ((t == HTML.Tag.P) && useText) {
			result.append("\n");
		} else if ((t == HTML.Tag.TABLE) && result.length() > 0) {
            useText=false;
            foundEnd=true;
		}
	}


	/**
	 * Looks for end of links
	 * 
	 * @param t
	 *            A tag
	 * @param pos
	 *            unused
	 */
	public void handleEndTag(Tag t, int pos) {
		if (t == HTML.Tag.A) {
			inLink = false;
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
		if (!ignore && line.startsWith(this.show)) {
			if (--showCount < 0) {
				foundStart = useText = ignore = true;
			}
		} else if (line.startsWith("Original Airdate:")) {
			useText = false;
			foundEnd = true;
			result.append(line + " ");
		} else if (line.startsWith("Future Airings:")) {
			useText = false;
			foundEnd = true;
		} else if (useText && !inLink && !line.equals("")) {
			result.append(line + " ");
		}
	}


	private String getResult() {
		return result.toString();
	}


	/**
	 * Grabs the summary info
	 * 
	 * @return The text
	 */
	public String getSummary() {
		logger.fine("Looking for Show:" + show + " using URL= " + url);
		String tempResult = "";
		try {
			URLConnection conn = this.url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			ParserDelegator pd = new ParserDelegator();
			pd.parse(br, this, true);

			tempResult = getResult();
			if (!foundStart || !foundEnd) {
				tempResult = "";
			}
		} catch (Exception e) {
			//e.printStackTrace();
			logger.severe(e.toString());
			return "";
		}
		return tempResult;
	}


	/**
	 * Looks for BRs and appends newlines
	 * 
	 * @param t
	 *            the tag
	 * @param a
	 *            unused
	 * @param pos
	 *            unused
	 */
	public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos) {
		if ((t == HTML.Tag.BR) && useText) {
			result.append("\n");
		}
	}
}

class TVListingsScraper extends ParserCallback {
	static Logger logger = Logger.getLogger(TVListingsModule.class.getName());
	private static final String BASE_URL = "http://search.tv.yahoo.com";
	private static final String SEARCH_URL = "/search/tv";
	private static final String SHOW_URL = "?title=";
	private static final String PROVIDER_URL = "&type=n&lineup=";
	private static final String END_URL = "&search=true&.intl=us&range=7";
	private String provider;
	private String show;
	private boolean foundURL = false;
	private boolean foundText = false;
	private boolean grabURL = false;
	private boolean grabChannel = false;
	private ShowDetail result = new ShowDetail();


	/**
	 * Creates a new TVListingsScraper object.
	 * 
	 * @param show
	 *            shows name
	 * @param provider
	 *            provider to use for lookup
	 */
	public TVListingsScraper(String show, String provider) {
		this.show = show.replace(' ', '+');
		this.provider = provider;
	}


	/**
	 * Grabs the shows URL when its appropriate
	 * 
	 * @param t
	 *            the tag!
	 * @param a
	 *            looks for hrefs
	 * @param pos
	 *            unused
	 */
	public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
		if (!foundURL) {
			if (grabURL) {
				try {
					String urlString = a.getAttribute(HTML.Attribute.HREF).toString();
					StringTokenizer stok = new StringTokenizer(urlString);
					String resultString = "";
					while (stok.hasMoreElements()) {
						resultString += stok.nextElement();
					}

					URL url = new URL(resultString);
					result.setUrl(url);
					foundURL = true;
				} catch (Exception e) {
				}
			}
		}
	}


	/**
	 * DOCUMENT ME!
	 * 
	 * @param t
	 *            DOCUMENT ME!
	 * @param pos
	 *            DOCUMENT ME!
	 */
	public void handleEndTag(Tag t, int pos) {
		if (t == HTML.Tag.A) {
			if (foundURL && grabURL) {
				grabURL = false;
				grabChannel = true;
			}
		}
	}


	/**
	 * Looks for link to show detail
	 * 
	 * @param data
	 *            the text
	 * @param pos
	 *            unused
	 */
	public void handleText(char[] data, int pos) {
		if (foundURL && !foundText) {
			logger.finer("FOUND TEXT=" + new String(data));
			result.setShow(new String(data));
			foundText = true;
		} else if (new String(data).trim().equals("Chronologically")) {
			//grab the first link after the Chronologically link
			grabURL = true;
		} else if (grabChannel) {
			String line = new String(data).trim();
			int index = line.indexOf(",");
			if (index >= 0) {
				grabChannel = false;
				result.setChannel(line.substring(0, index));
			}
		}
	}


	/**
	 * Grabs the show detail
	 * 
	 * @return the show detail
	 */
	public ShowDetail getShowDetail() {
		try {
			URL url = new URL(BASE_URL + SEARCH_URL + SHOW_URL + this.show + PROVIDER_URL + this.provider + END_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			int resCode = conn.getResponseCode();
			logger.fine("THE URL=" + conn.getURL() + " using code:" + resCode);

			if (logger.isLoggable(Level.FINEST)) {
				Map headers = conn.getHeaderFields();
				for (Iterator iter = headers.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					List values = (List) headers.get(key);
					for (Iterator iterator = values.iterator(); iterator.hasNext();) {
						String value = (String) iterator.next();
						logger.finest("Header:" + key + " = " + value);
					}
				}
			}

			//handle relative redirects
			if ((resCode > 300) && (resCode < 400)) {
				String newURL = conn.getHeaderField("Location");
				if ((newURL != null) && newURL.startsWith("/")) {
					logger.fine("Redirecting to new URL=" + newURL);
					url = new URL(BASE_URL + newURL);
					conn.disconnect();
					conn = (HttpURLConnection) url.openConnection();
				}
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			ParserDelegator pd = new ParserDelegator();
			pd.parse(br, this, true);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
}

class ShowDetail {
	private URL url;
	private String show;
	private String channel;


	/**
	 * Get Show name
	 * 
	 * @return Show name
	 */
	public String getShow() {
		return show;
	}


	/**
	 * Get Show Detail URL
	 * 
	 * @return Show Detail URL
	 */
	public URL getUrl() {
		return url;
	}


	/**
	 * Set Show name
	 * 
	 * @param string
	 *            the show's name
	 */
	public void setShow(String string) {
		show = string;
	}


	/**
	 * Set the Show Detail URL
	 * 
	 * @param url
	 *            Show Detail URL
	 */
	public void setUrl(URL url) {
		this.url = url;
	}


	/**
	 * Get the Channel the show is on
	 * 
	 * @return the Channel the show is on
	 */
	public String getChannel() {
		return channel;
	}


	/**
	 * Set the Channel the show is on
	 * 
	 * @param string
	 *            the Channel the show is on
	 */
	public void setChannel(String string) {
		channel = string;
	}
}
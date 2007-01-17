<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Features</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="features"/>	
	<dh:script module="dh.tooltip"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="features">
	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Mugshot Features</div>
		<div class="dh-features-header">
			Mugshot makes it easy to keep up with what your friends are doing online at different sites, all in one place.
		</div>
		<table cellspacing="0" cellpadding="0" class="dh-features-list">
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">Get updates from these sites, and more:</td>
			</tr>
			<tr>
		      <dht3:featuresItem prefix="Music"
		                         icon="musicradar_icon.png"
		                         text="iTunes, Last.fm, Rhapsody, and Yahoo! Music playlists"
		                         tipIcon="music_block.png"
		                         tipIconWidth="400"
		                         tipIconHeight="73"
		                         tipText="Get updates when your Mugshot friends play music, and they'll see yours too."/>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/blog_icon.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">Blogger, LiveJournal, MySpace, and more blogs</td>
			</tr>
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_flickr.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">Flickr photo sets</td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_digg.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">Stories rated on Digg and Reddit</td>			  
			</tr>		
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_youtube.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">YouTube videos</td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_twitter.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">Twitter status updates</td>			  
			</tr>		
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_delicious.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">deli.icio.us bookmarks</td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_facebook.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">Facebook messages, photos, pokes, and wall messages</td>			  
			</tr>	
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/feed_icon16x16.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">RSS feeds</td>
			  <td></td>
			  <td></td>	  
			</tr>			
			
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">Share links and updates with friends and groups with similar interests.</td>
			</tr>		
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/webswarm_icon.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">"Swarm" notifications of new and popular content</td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/quips_16x16.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">"Quips" to rate and comment on anything shared on Mugshot</td>	  
			</tr>	
			
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">Keep up with everything. </td>
			</tr>		
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/mugshot_icon.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">On the web: through your own Mugshot page</td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/mini_16x16.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">On your blog or Web page: using <a href="/badges">Mugshot Mini</a> and <a href="/radar-learnmore">Music Radar</a> widgets</td>
			</tr>		
			<tr>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/stacker_16x16.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">On your desktop: using the <a href="/stacker-learnmore">Mugshot Stacker</a></td>
			  <td class="dh-features-list-icon-column"><dh:png src="/images3/${buildStamp}/favicon_gdesktop.png" style="width: 16; height: 16;"/></td>
			  <td class="dh-features-list-content-column">On your Google Desktop and home page: using the <a href="/google-stacker">Mugshot Google Gadget</a></td>
			</tr>	
			
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">It's free, fun, and easy to use.</td>
			</tr>		
			<tr>
			  <td colspan="4"><a href="/signup">Sign up</a> for a free account to get started right away.  You can just use
			  the Web version, or make the most of Mugshot with our desktop software.  It's easy to install,
			  completely <a href="http://developer.mugshot.org">open source</a>, and there's no risk of nasty spyware.  Mugshot
			  works with most popular Web sites, with more added every day.</td>
			</tr>																			
		</table>
		<div class="dh-features-header">
			<a href="/signup">Sign up today!</a>
		</div>		
	</dht3:shinyBox>
</dht3:page>

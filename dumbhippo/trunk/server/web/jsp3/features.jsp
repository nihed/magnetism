<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Features</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
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
		                         tipText="Get updates when people in your Mugshot network play music."/>
		      <dht3:featuresItem prefix="Blog"
		                         icon="blog_icon.png"
		                         text="Blogger, LiveJournal, MySpace, and more blogs"
		                         tipIcon="blog_block.png"
		                         tipIconWidth="450"
		                         tipIconHeight="34"
		                         tipText="Get updates when people in your Mugshot network post blog entries."/>
			</tr>
			<tr>
			  <dht3:featuresItem prefix="Flickr"
			                     icon="favicon_flickr.png"
			                     text="Flickr photo sets"
			                     tipIcon="flickr_thumbs.png"
			                     tipIconWidth="400"
			                     tipIconHeight="118"
			                     tipText="Get updates when people in your Mugshot network post new photos."/>
			  <dht3:featuresItem prefix="Digg"
			                     icon="favicon_digg.png"
			                     text="Stories rated on Digg and Reddit"
			                     tipIcon="digg_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="34"
			                     tipText="Get updates when people in your Mugshot network rate stories."/>	  
			</tr>		
			<tr>
			  <dht3:featuresItem prefix="YouTube"
			                     icon="favicon_youtube.png"
			                     text="YouTube videos"
			                     tipIcon="youtube_thumbs.png"
			                     tipIconWidth="400"
			                     tipIconHeight="120"
			                     tipText="Get updates when people in your Mugshot network post new YouTube videos."/>
			  <dht3:featuresItem prefix="Twitter"
			                     icon="favicon_twitter.png"
			                     text="Twitter status updates"
			                     tipIcon="twitter_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="34"
			                     tipText="Get updates when people in your Mugshot network send Twitter updates."/>			  
			</tr>		
			<tr>
			  <dht3:featuresItem prefix="Delicious"
			                     icon="favicon_delicious.png"
			                     text="del.icio.us bookmarks"
			                     tipIcon="delicious_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="34"
			                     tipText="Get updates when people in your Mugshot network post new public del.icio.us bookmarks"/>
			  <dht3:featuresItem prefix="Facebook"
			                     icon="favicon_facebook.png"
			                     text="Facebook messages, photos, pokes, and wall messages"
			                     tipIcon="facebook_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="34"
			                     tipText="Get updates when you get new Facebook updates like messages, photos, and pokes."/>			  
			</tr>	
			<tr>
			  <dht3:featuresItem prefix="Netflix"
			                     icon="favicon_netflix.png"
			                     text="Netflix video queue"
			                     tipIcon="netflix_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="79"
			                     tipText="Get updates when people in your Mugshot network queue Netflix movies."/> 			
			  <dht3:featuresItem prefix="RSS"
			                     icon="feed_icon16x16.png"
			                     text="RSS feeds"
			                     tipIcon="feed_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="34"
			                     tipText="Get updates from other RSS feeds"/>  
			</tr>			
			
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">Share links and updates with friends and groups with similar interests.</td>
			</tr>		
			<tr>
			  <dht3:featuresItem prefix="Webswarm"
			                     icon="webswarm_icon.png"
			                     text="\"Swarm\" notifications of new and popular content"
			                     tipIcon="webswarm_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="70"
			                     tipText="Get updates when people in your Mugshot network share and swarm around links"/>
			  <dht3:featuresItem prefix="Quips"
			                     icon="quips_16x16.png"
			                     text="\"Quips\" to rate and comment on anything shared on Mugshot"
			                     tipIcon="quip.png"
			                     tipIconWidth="243"
			                     tipIconHeight="116"
			                     tipText="See quips people add to Mugshot items"/>
			</tr>	
			
			<tr class="dh-features-list-section">
			<td class="dh-features-list-header" colspan="4">Keep up with everything. </td>
			</tr>		
			<tr>
			  <dht3:featuresItem prefix="OnWeb"
			                     icon="mugshot_icon.png"
			                     text="On the web: through your own Mugshot page"
			                     tipIcon="webswarm_block.png"
			                     tipIconWidth="450"
			                     tipIconHeight="70"
			                     tipText="Your Mugshot page shows you a stack of updates"/>
			  <dht3:featuresItem prefix="Mini"
			                     icon="mini_16x16.png"
								 tipIcon="mini.png"
								 tipIconWidth="250"
								 tipIconHeight="180"
								 tipText="Put the mini on your own website">
			    On your blog or Web page: using <a href="/badges">Mugshot Mini</a> and <a href="/radar-learnmore">Music Radar</a> widgets								 
			  </dht3:featuresItem>
			</tr>		
			<tr>
			  <dht3:featuresItem prefix="Stacker"
			                     icon="stacker_16x16.png"
			                     tipIcon="stacker.png"
			                     tipIconWidth="400"
			                     tipIconHeight="237"
			                     tipText="Get updates on your desktop">
			    On your desktop: using the <a href="/stacker-learnmore">Mugshot Stacker</a>
			  </dht3:featuresItem>
			  <dht3:featuresItem prefix="GDesktop"
			                     icon="favicon_gdesktop.png"
			                     tipIcon="google_gadget.png"
			                     tipIconWidth="400"
			                     tipIconHeight="267"
			                     tipText="Get updates on your Google desktop">
			    On your Google Desktop and home page: using the <a href="/google-stacker">Mugshot Google Gadget</a>
			  </dht3:featuresItem>
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
		<div class="dh-shinybox-bottom-content dh-features-header">
			<a href="/signup">Sign up today!</a>
		</div>		
	</dht3:shinyBox>
</dht3:page>

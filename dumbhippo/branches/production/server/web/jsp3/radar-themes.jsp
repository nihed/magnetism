<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingThemesPage" scope="request"/>

<head>
        <title>Music Radar Themes</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="radar"/>
	<dht:scriptIncludes/>
    <dh:script module="dh.nowplaying"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="radar-themes">
	<dht3:pageSubHeader title="Music Radar Themes" />
	<dht3:shinyBox color="grey">
	        <div class="dh-page-shinybox-title-large">Current Music Radar Theme</div>
		<div class="dh-radar-explanatory">
		<img src="/images2/${buildStamp}/beacon60x60.gif" align="left" />
		Show off your iTunes, Rhapsody, or Yahoo! Music Engine playlist.  See what your friends are listening to and explore new music.  When someone clicks on your Music Radar, they'll be taken to your Mugshot page to see more about you and your tastes. <a href="/radar-learnmore">Learn More</a>
		</div>
		<c:if test="${nowplaying.signin.valid}">
			<div class="dh-radar-explanatory">
				<strong>Music sharing:</strong>
				<c:choose>
					<c:when test="${nowplaying.signin.musicSharingEnabled}">
						<dh:script module="dh.actions"/>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>			
					</c:when>
					<c:otherwise>
						<dh:script module="dh.actions"/>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>
					</c:otherwise>
				</c:choose>
			</div>
		</c:if>
		<br clear="all"/>
		<c:if test="${nowplaying.signin.valid}">
			<c:if test="${!empty nowplaying.currentTheme}">
				<dht:radarTheme theme="${nowplaying.currentTheme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}" alreadyCurrent="true"/>		
			</c:if>
			<hr height="1px" color="#666666" style="margin: 10px 0px"/>
			<div class="dh-radar-explanatory">
			<strong>If Music Radar doesn't work:</strong>
			<ul>
				<li>Make sure Music sharing is on, you can turn it on with the On / Off switch above</li>
				<li>You must be running iTunes, Rhapsody, or Yahoo! Music Engine</li>
			</ul>
			</div>
		</c:if>
	</dht3:shinyBox>

	<c:if test="${nowplaying.signin.valid}">
		<c:if test="${nowplaying.myThemes.totalCount > 0}">
			<dht3:shinyBox color="grey">
		        <div class="dh-page-shinybox-title-large">Themes I've Created (<c:out value="${nowplaying.myThemes.totalCount}"/>)</div>
			<div>
				<c:forEach items="${nowplaying.myThemes.results}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}"/>
					<c:if test="${!status.last}"><hr height="1px" color="#666666"/></c:if>
				</c:forEach>
			</div>
			<dht:expandablePager pageable="${nowplaying.myThemes}" anchor="dhMyThemes"/>			
			</dht3:shinyBox>			
		</c:if>
		<c:if test="${nowplaying.friendsThemes.totalCount > 0}">
			<dht3:shinyBox color="grey">
		        <div class="dh-page-shinybox-title-large">My Friends' Theme Creations (<c:out value="${nowplaying.friendsThemes.totalCount}"/>)</div>
			<div>
		                <c:forEach items="${nowplaying.friendsThemes.results}" var="theme" varStatus="status">
					<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}"/>
					<c:if test="${!status.last}"><hr height="1px" color="#666666"/></c:if>
		                </c:forEach>
		        </div>
			<dht:expandablePager pageable="${nowplaying.friendsThemes}" anchor="dhFriendsThemes"/>			
			</dht3:shinyBox>
		</c:if>
	</c:if>

	<c:if test="${nowplaying.randomThemes.totalCount > 0}">
		<dht3:shinyBox color="grey">
	        <div class="dh-page-shinybox-title-large">Public Themes (<c:out value="${nowplaying.randomThemes.totalCount}"/>)</div>
		<div>
			<c:forEach items="${nowplaying.randomThemes.results}" var="theme" varStatus="status">
				<dht:radarTheme theme="${theme}" signin="${nowplaying.signin}" userId="${nowplaying.radarCharacterId}"/>
					<c:if test="${!status.last}"><hr height="1px" color="#666666"/></c:if>
			</c:forEach>		
		</div>
		<dht:expandablePager pageable="${nowplaying.randomThemes}" anchor="dhAllThemes"/>	
		</dht3:shinyBox>			
	</c:if>

</dht3:page>

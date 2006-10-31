<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingThemeCreatorPage" scope="request"/>
<jsp:setProperty name="nowplaying" property="themeId" param="theme"/>

<c:if test="${empty nowplaying.theme}">
	<dht:errorPage>No theme!</dht:errorPage>
</c:if>

<head>
        <title>Create a Music Radar Theme</title>
		<dht:siteStyle/>        
        <link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/radar.css"/>
		<dht:faviconIncludes/>
        <dht:scriptIncludes>
        	<dht:script src="dh/nowplaying.js"/>
        </dht:scriptIncludes>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxMusic>
		<c:if test="${!nowplaying.theme.draft}">
			This theme is already published so other people might be using it! If you're going to change 
			it a lot, consider 
			<a href="javascript:dh.nowplaying.createNewTheme('${nowplaying.theme.id}');">creating a new theme based on it</a> rather
			than changing this one. To edit this one, first <a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'true', '/radar-theme-creator?theme=${nowplaying.themeId}');">unpublish it</a>.
		</c:if>
		<dht:zoneBoxSubtitle>Preview: Music Playing</dht:zoneBoxSubtitle>
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active" hasLabel="false"/>
			<dht:radarPhotoUpload themeId="${nowplaying.themeId}" mode="active" linkText="Change Background Image" reloadTo="/radar-theme-creator?theme=${nowplaying.themeId}"/>
		</div>

		<dht:zoneBoxSubtitle>Preview: Music Stopped</dht:zoneBoxSubtitle>
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive" hasLabel="false"/>
			<dht:radarPhotoUpload themeId="${nowplaying.themeId}" mode="inactive" linkText="Change Background Image" reloadTo="/radar-theme-creator?theme=${nowplaying.themeId}"/>
		</div>
		
		<div>
			<c:choose>
				<c:when test="${!nowplaying.theme.draft}">
					<a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'true', '/radar-theme-creator?theme=${nowplaying.themeId}');">Unpublish</a>
				</c:when>
				<c:otherwise>
					<a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'false', '/radar-themes');">Publish</a>
					<a href="/radar-themes">Save Draft</a>	
				</c:otherwise>
			</c:choose>
		</div>
		
		<c:if test="${nowplaying.theme.draft}">
			<div>
			Themes are 440 pixels wide and 120 pixels high.
			</div>
	
			<dht:radarProperty themeId="${nowplaying.theme.id}"
				property="name" currentValue="${nowplaying.theme.name}"
				label="Theme Name"/>
				
			<dht:zoneBoxSubtitle>Album art</dht:zoneBoxSubtitle>				
			<div class="dh-radar-propset">
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumArtX" currentValue="${nowplaying.theme.albumArtX}"
					label="Album Art X"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumArtY" currentValue="${nowplaying.theme.albumArtY}"
					label="Album Art Y"/>
			</div>

			<dht:zoneBoxSubtitle>Song title</dht:zoneBoxSubtitle>				
			<div class="dh-radar-propset">
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="titleTextX" currentValue="${nowplaying.theme.titleTextX}"
					label="Song Title X"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="titleTextY" currentValue="${nowplaying.theme.titleTextY}"
					label="Song Title Y"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="titleTextColor" currentValue="${nowplaying.theme.titleTextColor}"
					label="Song Title Text Color"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="titleTextFontSize" currentValue="${nowplaying.theme.titleTextFontSize}"
					label="Song Title Size"/>
			</div>
	
			<dht:zoneBoxSubtitle>Artist</dht:zoneBoxSubtitle>				
			<div class="dh-radar-propset">		
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="artistTextX" currentValue="${nowplaying.theme.artistTextX}"
					label="Artist X"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="artistTextY" currentValue="${nowplaying.theme.artistTextY}"
					label="Artist Y"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="artistTextColor" currentValue="${nowplaying.theme.artistTextColor}"
					label="Artist Text Color"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="artistTextFontSize" currentValue="${nowplaying.theme.artistTextFontSize}"
					label="Artist Text Size"/>
			</div>
			
			<dht:zoneBoxSubtitle>Album</dht:zoneBoxSubtitle>				
			<div class="dh-radar-propset">		
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumTextX" currentValue="${nowplaying.theme.albumTextX}"
					label="Album Title X"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumTextY" currentValue="${nowplaying.theme.albumTextY}"
					label="Album Title Y"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumTextColor" currentValue="${nowplaying.theme.albumTextColor}"
					label="Album Title Color"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="albumTextFontSize" currentValue="${nowplaying.theme.albumTextFontSize}"
					label="Album Title Size"/>
			</div>

			<dht:zoneBoxSubtitle>Status</dht:zoneBoxSubtitle>				
			<div class="dh-radar-propset">
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="statusTextX" currentValue="${nowplaying.theme.statusTextX}"
					label="Status Title X"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="statusTextY" currentValue="${nowplaying.theme.statusTextY}"
					label="Status Title Y"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="statusTextColor" currentValue="${nowplaying.theme.statusTextColor}"
					label="Status Text Color"/>
	
				<dht:radarProperty themeId="${nowplaying.theme.id}"
					property="statusTextFontSize" currentValue="${nowplaying.theme.statusTextFontSize}"
					label="Status Text Size"/>
			</div>
		</c:if>
		
		<c:if test="${!empty theme.basedOn}">
			'<c:out value="${nowplaying.theme.name}"/>' by <c:out value="${nowplaying.theme.creator.nickname}"/> based on
			<%-- <a href="/radar-theme?theme=${nowplaying.theme.basedOn.id}">--%>'<c:out value="${theme.basedOn.name}"/>' by
			<c:out value="${theme.basedOn.creator.nickname}"/><%-- </a> --%>
		</c:if>
		</dht:zoneBoxMusic>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

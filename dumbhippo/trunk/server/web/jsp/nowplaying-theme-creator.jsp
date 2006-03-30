<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.NowPlayingThemeCreatorPage" scope="request"/>
<jsp:setProperty name="nowplaying" property="themeId" param="theme"/>

<c:if test="${empty nowplaying.theme}">
	<dht:errorPage>No theme!</dht:errorPage>
</c:if>

<head>
        <title>Create a Now Playing Theme</title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
        <script type="text/javascript">
	        dojo.require("dh.nowplaying");
		</script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>

		<c:choose>
			<c:when test="${!nowplaying.theme.draft}">
				This theme is already published so other people might be using it -
				you can't edit it anymore. However, you can 
				<a href="javascript:dh.nowplaying.createNewTheme('${nowplaying.theme.id}');">create a new theme based on it</a>.
			</c:when>
			<c:otherwise>
						
					<dht:smallTitle>Preview: While Music Is Playing</dht:smallTitle>		
					<div>
						<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active" hasLabel="false"/>
						<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="active" linkText="Change Background Image" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
					</div>
			
					<dht:smallTitle>Preview: When Not Listening</dht:smallTitle>
					
					<div>
						<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive" hasLabel="false"/>
						<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="inactive" linkText="Change Background Image" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
					</div>
					
					<div>			
						<a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'false', '/nowplaying-theme?theme=${nowplaying.themeId}');">Publish</a>
						<a href="/nowplaying-themes">Save Draft</a>
					</div>
					
					<dht:smallTitle>Theme Properties</dht:smallTitle>
			
					<div>
					Themes are 440 pixels wide and 120 pixels high.
					</div>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="name" currentValue="${nowplaying.theme.name}"
						label="Theme Name"/>
					
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumArtX" currentValue="${nowplaying.theme.albumArtX}"
						label="Album Art X"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumArtY" currentValue="${nowplaying.theme.albumArtY}"
						label="Album Art Y"/>
					
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="titleTextX" currentValue="${nowplaying.theme.titleTextX}"
						label="Song Title X"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="titleTextY" currentValue="${nowplaying.theme.titleTextY}"
						label="Song Title Y"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="titleTextColor" currentValue="${nowplaying.theme.titleTextColor}"
						label="Song Title Text Color"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="titleTextFontSize" currentValue="${nowplaying.theme.titleTextFontSize}"
						label="Song Title Size"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="artistTextX" currentValue="${nowplaying.theme.artistTextX}"
						label="Artist X"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="artistTextY" currentValue="${nowplaying.theme.artistTextY}"
						label="Artist Y"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="artistTextColor" currentValue="${nowplaying.theme.artistTextColor}"
						label="Artist Text Color"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="artistTextFontSize" currentValue="${nowplaying.theme.artistTextFontSize}"
						label="Artist Text Size"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumTextX" currentValue="${nowplaying.theme.albumTextX}"
						label="Album Title X"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumTextY" currentValue="${nowplaying.theme.albumTextY}"
						label="Album Title Y"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumTextColor" currentValue="${nowplaying.theme.albumTextColor}"
						label="Album Title Color"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="albumTextFontSize" currentValue="${nowplaying.theme.albumTextFontSize}"
						label="Album Title Size"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="statusTextX" currentValue="${nowplaying.theme.statusTextX}"
						label="Status Title X"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="statusTextY" currentValue="${nowplaying.theme.statusTextY}"
						label="Status Title Y"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="statusTextColor" currentValue="${nowplaying.theme.statusTextColor}"
						label="Status Text Color"/>
			
					<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
						property="statusTextFontSize" currentValue="${nowplaying.theme.statusTextFontSize}"
						label="Status Text Size"/>
						
					<c:if test="${!empty theme.basedOn}">
						'<c:out value="${nowplaying.theme.name}"/>' by <c:out value="${nowplaying.theme.creator.nickname}"/> based on
						<a href="/nowplaying-theme?theme=${nowplaying.theme.basedOn.id}">'<c:out value="${theme.basedOn.name}"/>' by
						<c:out value="${theme.basedOn.creator.nickname}"/></a>
					</c:if>
	
			</c:otherwise>
		</c:choose>			
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

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

		<dht:largeTitle>Preview: While Music is Playing</dht:largeTitle>
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active" hasLabel="false"/>
			<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="active" linkText="Change Active Background" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
		</div>
		
		<dht:largeTitle>Preview: When Not Listening</dht:largeTitle>
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive" hasLabel="false"/>
			<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="inactive" linkText="Change Inactive Background" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
		</div>
		
		<div>
			<a href="javascript:alert('not implemented yet');">Publish and Set As My Theme</a>
			<a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'false');">Publish</a>
			<a href="/nowplaying-themes">Save Draft</a>
		</div>
		
		<dht:largeTitle>Theme Properties</dht:largeTitle>

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
			property="artistTextX" currentValue="${nowplaying.theme.artistTextX}"
			label="Artist X"/>

		<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
			property="artistTextY" currentValue="${nowplaying.theme.artistTextY}"
			label="Artist Y"/>

		<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
			property="artistTextColor" currentValue="${nowplaying.theme.artistTextColor}"
			label="Artist Text Color"/>

		<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
			property="albumTextX" currentValue="${nowplaying.theme.albumTextX}"
			label="Album Title X"/>

		<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
			property="albumTextY" currentValue="${nowplaying.theme.albumTextY}"
			label="Album Title Y"/>

		<dht:nowPlayingProperty themeId="${nowplaying.theme.id}"
			property="albumTextColor" currentValue="${nowplaying.theme.albumTextColor}"
			label="Album Title Text Color"/>
			
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

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
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active"/>
			<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="active" linkText="Change Active Background" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
		</div>
		
		<dht:largeTitle>Preview: When Not Listening</dht:largeTitle>
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive"/>
			<dht:nowPlayingPhotoUpload themeId="${nowplaying.themeId}" mode="inactive" linkText="Change Inactive Background" reloadTo="/nowplaying-theme-creator?theme=${nowplaying.themeId}"/>
		</div>
		
		<div>
			<a href="javascript:alert('not implemented yet');">Publish and Set As My Theme</a>
			<a href="javascript:dh.nowplaying.modify('${nowplaying.themeId}', 'draft', 'false');">Publish</a>
			<a href="/nowplaying-themes">Save Draft</a>
		</div>
		
		<dht:largeTitle>Theme Properties</dht:largeTitle>
		
		<div>
		FIXME
		</div>
			
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

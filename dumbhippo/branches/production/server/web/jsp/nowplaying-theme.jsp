<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="nowplaying" class="com.dumbhippo.web.pages.NowPlayingThemeCreatorPage" scope="request"/>
<jsp:setProperty name="nowplaying" property="themeId" param="theme"/>

<c:if test="${empty nowplaying.theme}">
	<dht:errorPage>No theme!</dht:errorPage>
</c:if>

<head>
        <title>Now Playing Theme '<c:out value="${nowplaying.theme.name}"/></title>
        <dht:stylesheets />
        <dht:scriptIncludes/>
        <script type="text/javascript">
	        dojo.require("dh.nowplaying");
		</script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>

		<dht:largeTitle>'<c:out value="${nowplaying.theme.name}"/>' Preview</dht:largeTitle>
		<dht:smallTitle>While Music Is Playing</dht:smallTitle>		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="active" hasLabel="false"/>
		</div>
		
		<dht:smallTitle>When Not Listening</dht:smallTitle>		
		
		<div>
			<dh:nowPlaying userId="${nowplaying.signin.userId}" themeId="${nowplaying.themeId}" forceMode="inactive" hasLabel="false"/>
		</div>
		
		<div>
			<c:if test="${nowplaying.theme.creator eq signin.user}">
				<a href="/radar-theme-creator?theme=${nowplaying.theme.id}">Edit</a>
			</c:if>
			<c:if test="${!nowplaying.theme.draft}">
				<a href="javascript:dh.nowplaying.setTheme('${nowplaying.theme.id}');">Set As Current Theme</a>
			</c:if>
		</div>
		
		<c:if test="${!empty nowplaying.theme.basedOn}">
			'<c:out value="${nowplaying.theme.name}"/>' by <c:out value="${nowplaying.theme.creator.nickname}"/> based on
			<a href="/nowplaying-theme?theme=${nowplaying.theme.basedOn.id}">'<c:out value="${nowplaying.theme.basedOn.name}"/>' by
			<c:out value="${nowplaying.theme.basedOn.creator.nickname}"/></a>
		</c:if>

		<div>
			<a href="/nowplaying-themes">More Themes</a>
		</div>
			
	</dht:mainArea>
	
</dht:bodyWithAds>
</html>

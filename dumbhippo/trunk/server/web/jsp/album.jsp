<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="musicsearch" class="com.dumbhippo.web.MusicSearchPage" scope="request"/>
<jsp:setProperty name="musicsearch" property="albumMode" value="true"/>

<jsp:setProperty name="musicsearch" property="song" param="track"/>
<jsp:setProperty name="musicsearch" property="album" param="album"/>
<jsp:setProperty name="musicsearch" property="artist" param="artist"/>

<head>
	<title><c:out value="${musicsearch.album} - ${musicsearch.artist}"/></title>
	<dht:stylesheets href="music.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
</head>
<dht:bodyWithAds>

	<dht:mainArea>
		<c:choose>
			<c:when test="${empty musicsearch.albumView}">
				We got nothing! We couldn't find anything about
				the album "<c:out value="${musicsearch.album}"/>" from
				the artist "<c:out value="${musicsearch.artist}"/>."
			</c:when>
			<c:when test="${musicsearch.signin.disabled}">
				<%-- note this message appears even when viewing other people's pages --%>
				Your account is disabled; <a href="javascript:dh.actions.setAccountDisabled(false);">enable it again</a>
				to share stuff with friends.
			</c:when>
			<c:when test="${!musicsearch.signin.musicSharingEnabled}">
				<%-- again, we're using .signin, so appears even for others' pages --%>
				You haven't turned on music sharing. Turn it on to see what your friends are listening 
				to lately, and share your music with them. 
					<a href="javascript:dh.actions.setMusicSharingEnabled(true);">Click here to turn it on</a>
			</c:when>
			<c:otherwise>
				<div>
					<dht:album album="${musicsearch.albumView}"/>
				</div>
				<c:if test="${musicsearch.relatedPeople.size > 0}">
					<dht:largeTitle>Some Friends Who Listened to This Album</dht:largeTitle>
		
					<div>
						<c:forEach items="${musicsearch.relatedPeople.list}" var="personmusic">
							<div>
								<dht:headshot person="${personmusic.person}"/>
								<c:out value="${personmusic.person.name}"/>
								<c:forEach items="${personmusic.albums}" var="album">
									<dht:album album="${album}"/>
								</c:forEach>
							</div>
						</c:forEach>
					</div>
				</c:if>
				<c:if test="${musicsearch.albumRecommendations.size > 0}">
					<dht:largeTitle>Related Music</dht:largeTitle>
		
					<div>
						Other people who listened to <c:out value="${musicsearch.album}"/> also listened to:
					</div>
		
					<div>
						<c:forEach items="${musicsearch.albumRecommendations.list}" var="album">
							<dht:album album="${album}"/>
						</c:forEach>
					</div>
				</c:if>
			</c:otherwise>
		</c:choose>
	</dht:mainArea>

</dht:bodyWithAds>
</html>

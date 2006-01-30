<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="musicsearch" class="com.dumbhippo.web.MusicSearchPage" scope="request"/>
<jsp:setProperty name="musicsearch" property="song" param="track"/>
<jsp:setProperty name="musicsearch" property="album" param="album"/>
<jsp:setProperty name="musicsearch" property="artist" param="artist"/>

<head>
	<title><c:out value="${musicsearch.song}"/></title>
	<dht:stylesheets href="music.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<c:choose>
			<c:when test="${musicsearch.weGotNothing}">
				We got nothing! We couldn't find anything about
				the track "<c:out value="${musicsearch.song}"/>" on the album
				"<c:out value="${musicsearch.album}"/>" from the artist "<c:out value="${musicsearch.artist}"/>."
			</c:when>
			<c:when test="${musicsearch.signin.disabled}">
				<% /* note this message appears even when viewing other people's pages */ %>
				Your account is disabled; <a href="javascript:dh.actions.setAccountDisabled(false);">enable it again</a>
				to share stuff with friends.
			</c:when>
			<c:when test="${!musicsearch.signin.musicSharingEnabled}">
				<% /* again, we're using .signin, so appears even for others' pages */ %>
				You haven't turned on music sharing. Turn it on to see what your friends are listening 
				to lately, and share your music with them. 
					<a href="javascript:dh.actions.setMusicSharingEnabled(true);">Click here to turn it on</a>
			</c:when>
			<c:otherwise>
				<h2 class="dh-title"></h2>
	
				<div>
					<dht:track track="${musicsearch.song}"/>
				</div>
				<c:if test="${musicsearch.relatedPeople.size > 0}">
					<h2 class="dh-title">Some Friends Who Listened to This Song</h2>
		
					<div>
						<c:forEach items="${musicsearch.relatedPeople.list}" var="personmusic">
							<div>
								<dht:headshot person="${personmusic.person}"/>
								<c:forEach items="${personmusic.tracks}" var="track">
									<dht:track track="${track}"/>
								</c:forEach>
							</div>
						</c:forEach>
					</div>
				</c:if>
				<c:if test="${musicsearch.recommendations.size > 0}">
					<h2 class="dh-title">Related Music</h2>
		
					<div>
						Other people who listened to <c:out value="${musicsearch.song}"/> also listened to:
					</div>
		
					<div>
						<c:forEach items="${musicsearch.recommendations.list}" var="track">
							<dht:track track="${track}"/>
						</c:forEach>				
					</div>
				</c:if>
			</c:otherwise>
		</c:choose>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<c:if test="${!musicsearch.disabled}">
			<div class="person">
				<dht:headshot person="${musicsearch.person}" size="192"/>
				<div id="dhName"><c:out value="${personName}"/></div>
			</div>
		</c:if>
		</div>

		<div class="dh-right-box-area">
		
			<div class="dh-right-box">
				<h5 class="dh-title">Groups' Music</h5>
				<div class="dh-groups">
				<c:choose>
					<c:when test="${musicsearch.groups.size > 0}">
						<dh:entityList value="${musicsearch.groups.list}" photos="true" music="true"/>
					</c:when>
					<c:otherwise>
					</c:otherwise>
				</c:choose>
				</div>
			</div>
		
			<div class="dh-right-box">
				<h5 class="dh-title">Friends' Music</h5>
				<div class="dh-people">
					<c:choose>
						<c:when test="${musicsearch.contacts.size > 0}">
							<dh:entityList value="${musicsearch.contacts.list}" showInviteLinks="false" photos="true" music="true"/>
						</c:when>
						<c:otherwise>
							<% /* no contacts shown, probably because viewer isn't a contact of viewee */ %>
						</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>

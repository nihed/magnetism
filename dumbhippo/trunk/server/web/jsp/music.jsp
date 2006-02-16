<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewperson" class="com.dumbhippo.web.PersonMusicPage" scope="request"/>
<jsp:setProperty name="viewperson" property="viewedPersonId" param="who"/>

<c:if test="${!viewperson.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:set var="personName" value="${viewperson.person.name}" scope="page"/>
<c:set var="personId" value="${viewperson.person.viewPersonPageId}" scope="page"/>

<head>
	<title><c:out value="${personName}'s Music"/></title>
	<dht:stylesheets href="music.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<c:if test="${viewperson.signin.valid && !viewperson.disabled}">
		    <dht:toolbar account="false">
				&#151; <a class="dh-toolbar-item" style="font-weight:bold" href='/person?who=${personId}'>Public Page for <c:out value="${personName}"/></a>
			</dht:toolbar>
		</c:if>

		<c:if test="${viewperson.disabled}">
			<div id="dhInformationBar">This person's account is not active</div>
		</c:if>

		<c:choose>
			<c:when test="${viewperson.disabled && !viewperson.self}">
				This account is disabled. Ask your friend to switch it back on!
			</c:when>
			<c:when test="${viewperson.signin.valid && viewperson.signin.disabled}">
				<% /* note this message appears even when viewing other people's pages */ %>
				Your account is disabled; <a href="javascript:dh.actions.setAccountDisabled(false);">enable it again</a>
				to share stuff with friends.
			</c:when>
			<c:when test="${viewperson.signin.valid && !viewperson.signin.musicSharingEnabled}">
				<% /* again, we're using viewperson.signin, so appears even for others' pages */ %>
				<div>
				<p><dht:musicToggle musicOn="${viewperson.signin.musicSharingEnabled}"/></p>
				<p>You haven't turned on music sharing. Turn it on to see what your friends are listening to lately, and share your music with them.</p>
				</div>
			</c:when>
			<c:when test="${!viewperson.musicSharingEnabled}">
				<c:out value="${personName}"/> hasn't turned on music sharing. Ask them to
				switch it on and you can see each other's impeccable musical tastes.
			</c:when>
			<c:when test="${!viewperson.signin.valid}">
				<% /* anonymous viewer */ %>
				<h2 class="dh-title"><c:out value="${personName}"/> is listening to:</h2>
				<div>
					<dht:track track="${viewperson.currentTrack}" linkifySong="false"/>
				</div>
				<h2 class="dh-title">LIMITED TIME OFFER</h2>
				<div>
					AS SEEN ON TV: <a href="${viewperson.downloadUrlWindows}">Download Now</a>
					to take advantage! Create an account to see more of this person's music and publish your own.
				</div>
				
				<h2 class="dh-title">Popular Songs on DumbHippo</h2>
	
				<div>
					Stuff people are listening to:
				</div>
				<div>
					<c:forEach items="${viewperson.popularTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
				
			</c:when>
			<c:otherwise>
				<h2 class="dh-title"><c:out value="${personName}"/>'s Recent Songs</h2>
	
				<div>
					<c:forEach items="${viewperson.latestTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
				
				<h2 class="dh-title"><c:out value="${personName}"/>'s Most Played Songs</h2>
	
				<div>
					<c:forEach items="${viewperson.frequentTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
			</c:otherwise>
		</c:choose>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<c:if test="${!viewperson.disabled}">
			<div class="person">
				<dht:headshot person="${viewperson.person}" size="192"/>
				<div id="dhName"><c:out value="${personName}"/></div>
			</div>
		</c:if>
		</div>

		<div class="dh-right-box-area">
			<c:if test="${viewperson.signin.valid}">
				<div class="dh-right-box">
					<h5 class="dh-title">Music</h5>
					<p><a href="/nowplaying?who=${personId}">Show your music</a> on <strong>MySpace</strong> and other sites</p>
					</p>
					<c:if test="${viewperson.self && viewperson.signin.musicSharingEnabled}">
						<p class="dh-right-box-text"><dht:musicToggle musicOn="${viewperson.signin.musicSharingEnabled}"/></p>
					</c:if>	
				</div>		
	
				<div class="dh-right-box">
					<h5 class="dh-title">Groups' Music</h5>
					<div class="dh-groups">
					<c:choose>
						<c:when test="${viewperson.groups.size > 0}">
							<dh:entityList value="${viewperson.groups.list}" photos="true" music="true"/>
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
							<c:when test="${viewperson.contacts.size > 0}">
								<dh:entityList value="${viewperson.contacts.list}" showInviteLinks="false" photos="true" music="true"/>
							</c:when>
							<c:otherwise>
								<% /* no contacts shown, probably because viewer isn't a contact of viewee */ %>
							</c:otherwise>
						</c:choose>
					</div>
				</div>
			</c:if>
		</div>
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>

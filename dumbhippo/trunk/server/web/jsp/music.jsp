<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewperson" class="com.dumbhippo.web.PersonMusicPage" scope="request"/>

<%-- If there's no who= param, we want to redirect to the signed-in user's who=, 
     but if who=invalid, we want an error message --%>
<c:if test="${empty param.who && viewperson.signin.valid}">
	<c:redirect url="/music?who=${viewperson.signin.userId}"></c:redirect>
</c:if>

<jsp:setProperty name="viewperson" property="viewedUserId" param="who"/>

<%-- this treats an invalid guid and missing who= the same, thus the other check above 
	since we want to special-case missing who= --%>
<c:if test="${!viewperson.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:set var="personName" value="${viewperson.viewedPerson.name}" scope="page"/>
<c:set var="personId" value="${viewperson.viewedPerson.viewPersonPageId}" scope="page"/>

<head>
	<title><c:out value="${personName}'s Music"/></title>
	<dht:stylesheets href="music.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
</head>
<dht:bodyWithAds>

	<dht:mainArea>
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
				<%-- note this message appears even when viewing other people's pages --%>
				Your account is disabled; <a href="javascript:dh.actions.setAccountDisabled(false);">enable it again</a>
				to share stuff with friends.
			</c:when>
			<c:when test="${viewperson.signin.valid && !viewperson.signin.musicSharingEnabled}">
				<%-- again, we're using viewperson.signin, so appears even for others' pages --%>
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
				<%-- anonymous viewer --%>
				<c:choose>
					<c:when test="${viewperson.selfInvitations > 0}">
						<dht:largeTitle>Create an Account</dht:largeTitle>
						<div>
							With a DumbHippo account, we'll put your current song 
							on the net so you can display it on your MySpace or other web page.
							You can also browse the music your friends on DumbHippo have been listening
							to.
						</div>
						<div>
							To create an account, click on the link we send you in email. Then you can 
							download the DumbHippo software which tracks what you're listening to 
							in iTunes or Yahoo! Music Engine. You can disable your account 
							easily at any time, and we promise we don't send out spam.
							&nbsp;
							<dht:selfInvite promotion="${viewperson.promotion}" invitesAvailable="${viewperson.selfInvitations}"/>
						</div>
						<div>
							Check out 
							<a href="http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=59629551&blogID=93563677">our MySpace page</a>
							and <a href="/privacy">our privacy policy</a>. Please send us questions and feedback!
						</div>
						<div>
						P.S. if you create an account, you can try out some of 
						our other DumbHippo features as well - for example, a new 
						way to share cool web sites with your friends.
					</c:when>
					<c:otherwise>
						<dht:largeTitle>CHECK BACK SOON</dht:largeTitle>
						<div>
							Sometimes we have invites to DumbHippo available
							on this page. Right now we're all out. If you leave your 
							email address, we'll let you know when we have something.
							<dht:wantsIn/>
						</div>
						<div>
							With a DumbHippo account, we'll put your current song 
							on the net so you can display it on your MySpace or other web page.
							You can also browse the music your friends on DumbHippo have been listening
							to.
						</div>
						<div>
							Also check out 
							<a href="http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=59629551&blogID=93563677">our MySpace page</a>.
						</div>		
					</c:otherwise>
				</c:choose>
				<c:if test="${viewperson.exampleThemes.size > 0}">
					&nbsp;
					<dht:smallTitle>Some Example "Now Playing" Embeds</dht:smallTitle>
					<div>
					You can put a display of your current song on your MySpace page, blog, or web site.
					Choose from these themes, or create your own, once you have a DumbHippo account.
					</div>
					<div>
						<c:forEach items="${viewperson.exampleThemes.list}" var="theme" varStatus="status">
							&nbsp;
							<dht:nowPlayingTheme theme="${theme}" signin="${viewperson.signin}" userId="${personId}"/>
						</c:forEach>
					</div>
				</c:if>
				<c:if test="${!empty viewperson.currentTrack}">
					<dht:largeTitle><c:out value="${personName}"/> is listening to:</dht:largeTitle>
					<div>
						<dht:track track="${viewperson.currentTrack}" linkifySong="false"/>
					</div>
				</c:if>
				
				<dht:largeTitle>Popular Songs on DumbHippo</dht:largeTitle>
	
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
				<dht:largeTitle><c:out value="${personName}"/>'s Recent Songs</dht:largeTitle>
	
				<div>
					<c:forEach items="${viewperson.latestTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
				
				<dht:largeTitle><c:out value="${personName}"/>'s Most Played Songs</dht:largeTitle>
	
				<div>
					<c:forEach items="${viewperson.frequentTracks.list}" var="track">
						<dht:track track="${track}"/>
					</c:forEach>
				</div>
			</c:otherwise>
		</c:choose>
	</dht:mainArea>

	<dht:sidebarArea>
		<dht:sidebarAreaHeader>
			<c:if test="${!viewperson.disabled}">
				<dht:headshot person="${viewperson.viewedPerson}" size="192"/>
				<dht:sidebarAreaHeaderName value="${personName}" canModify="false"/>
			</c:if>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
			<c:if test="${viewperson.signin.valid}">
				<dht:sidebarPane title="Music">
					<p><a href="/nowplaying?who=${personId}">Show your music</a> on <strong>MySpace</strong> and other sites</p>
					</p>
					<c:if test="${viewperson.self && viewperson.signin.musicSharingEnabled}">
						<p class="dh-right-box-text"><dht:musicToggle musicOn="${viewperson.signin.musicSharingEnabled}"/></p>
					</c:if>	
				</dht:sidebarPane>		
	
				<dht:sidebarPane title="Groups' Music">
					<div class="dh-groups">
						<c:choose>
							<c:when test="${viewperson.groups.size > 0}">
								<dh:entityList value="${viewperson.groups.list}" photos="true" music="true"/>
							</c:when>
							<c:otherwise>
							</c:otherwise>
						</c:choose>
					</div>
				</dht:sidebarPane>
			
				<dht:sidebarPane title="Friends' Music" last="true">
					<div class="dh-people">
						<c:choose>
							<c:when test="${viewperson.contacts.size > 0}">
								<dh:entityList value="${viewperson.contacts.list}" showInviteLinks="false" photos="true" music="true"/>
							</c:when>
							<c:otherwise>
								<%-- no contacts shown, probably because viewer isn't a contact of viewee --%>
							</c:otherwise>
						</c:choose>
					</div>
				</dht:sidebarPane>
			</c:if>
		</dht:sidebarPanes>
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>

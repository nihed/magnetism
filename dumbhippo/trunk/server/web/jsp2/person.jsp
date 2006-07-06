<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>
<jsp:setProperty name="person" property="asOthersWouldSee" value="true"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/person.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<c:choose>
<c:when test="${!person.disabled}">
	<c:if test="${person.self}">
		<c:set var="topMessageHtml" value="Here is how friends see you on Mugshot. Strangers see only some of this. You can go back <a href='/'>home</a> or <a href='/account'>edit your account</a>." scope="page"/>
	</c:if>
<dht:twoColumnPage alwaysShowSidebar="true" topMessageHtml="${topMessageHtml}">
	<dht:sidebarPerson who="${person.viewedUserId}" asOthersWouldSee="true"/>
	<dht:contentColumn>
		<c:set var="haveWhereAtSection" value="${!empty person.viewedPerson.email || !empty person.viewedPerson.aim || person.lovedAccounts.size > 0}"/>
		<c:set var="haveWhereNotAtSection" value="${person.hatedAccounts.size > 0}"/>
		<c:if test="${haveWhereAtSection || haveWhereNotAtSection}">
			<dht:zoneBoxWhereAt>
				<c:if test="${haveWhereAtSection}">
					<c:if test="${!empty person.viewedPerson.aim}">
						<c:if test="${!empty person.aimPresenceImageLink}">
							<img src="${person.aimPresenceImageLink}" border="0"/>
						</c:if>
						<dht:whereAtItem label="AIM" linkText="${person.viewedPerson.aim.screenName}" linkTarget="${person.aimLink}"/>
					</c:if>
					<c:if test="${!empty person.viewedPerson.email}">
						<dh:png src="/images2/${buildStamp}/email_icon.png" style="width: 16px; height: 11px;"/>
						<dht:whereAtItem linkText="Send me email" linkTarget="${person.emailLink}"/>
					</c:if>
					<c:forEach var="account" items="${person.lovedAccounts.list}">
						<div>
							<dht:whereAtItem label="${account.siteName}" linkText="${account.linkText}" linkTarget="${account.link}"/>
						</div>
					</c:forEach>
				</c:if>
				<c:if test="${haveWhereAtSection && haveWhereNotAtSection}">
					<dht:zoneBoxSeparator/>
				</c:if>		
				<c:if test="${haveWhereNotAtSection}">
					<dht:zoneBoxTitle>WHERE YOU WON'T FIND ME</dht:zoneBoxTitle>
					<c:forEach var="account" items="${person.hatedAccounts.list}">
						<div class="dh-hated-place">
							<dh:png src="/images2/${buildStamp}/hate15x15.png" style="width: 15px; height: 15px;"/>
							<span class="dh-hated-place-name"><c:out value="${account.siteName}"/></span> &#8212; <c:out value="${account.quip}"/>
						</div>
					</c:forEach>
				</c:if>
			</dht:zoneBoxWhereAt>
		</c:if>
		<dht:zoneBoxWeb disableJumpTo="true">
			<dht:requireLinksPersonBean who="${person.viewedUserId}"/>
	        <dht:linkSwarmPromo separator="true" linksLink="true" browserInstructions="${signin.valid}"/>
			<c:if test="${links.favoritePosts.resultCount > 0}">
				<dht:zoneBoxTitle a="dhFavoritePosts">FAVES</dht:zoneBoxTitle>
				<dht:postList posts="${links.favoritePosts.results}" format="simple"/>
				<dht:expandablePager pageable="${links.favoritePosts}" anchor="dhFavoritePosts"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle a="dhSentPosts">SHARED BY <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.sentPosts.resultCount > 0}">
					<dht:postList posts="${links.sentPosts.results}" format="simple"/>
					<dht:expandablePager pageable="${links.sentPosts}" anchor="dhSentPosts"/>
				</c:when>
				<c:otherwise>
					Nothing shared by <c:out value="${person.viewedPerson.name}"/> yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic disableJumpTo="true">
			<dht:requireMusicPersonBean who="${person.viewedUserId}"/>		
	        <dht:musicRadarPromo separator="true" musicLink="${!signin.valid}"/>
			<dht:zoneBoxTitle>CURRENT SONG FOR <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<dh:nowPlaying userId="${person.viewedUserId}" hasLabel="false"/>
			<dht:zoneBoxSeparator/>
			
			<c:choose>
			<c:when test="${!empty person.viewedPerson.musicBioAsHtml}">
			    <dht:zoneBoxSubcolumns>
				    <dht:zoneBoxSubcolumn which="one">
			            <c:if test="${musicPerson.recentTracks.resultCount > 0}">
				            <dht:trackList name="RECENT SONGS" id="dhRecentSongs" tracks="${musicPerson.recentTracks.results}"
				            pageable="${musicPerson.recentTracks}" separator="false" oneLine="false" playItLink="false"/>
			            </c:if>
				    </dht:zoneBoxSubcolumn>
				    <dht:zoneBoxSubcolumn which="two">
						<dht:zoneBoxTitle>MUSIC BIO</dht:zoneBoxTitle>
						<div class="dh-bio">
						    <c:out value="${person.viewedPerson.musicBioAsHtml}" escapeXml="false"/>
						</div>
			 	    </dht:zoneBoxSubcolumn>
			    </dht:zoneBoxSubcolumns>
			</c:when>
			<c:otherwise>
				<c:if test="${musicPerson.recentTracks.resultCount > 0}">
					<dht:trackList name="RECENT SONGS" id="dhRecentSongs" tracks="${musicPerson.recentTracks.results}"
			            		pageable="${musicPerson.recentTracks}" separator="false" oneLine="true" playItLink="false"/>
				</c:if>
			</c:otherwise>
			</c:choose>
		</dht:zoneBoxMusic>
		<dht:zoneBoxTv disableJumpTo="true">
			<dht:zoneBoxTitle>COMING SOON</dht:zoneBoxTitle>
			<p>Check the <a href="/tv">TV Party</a> page for more!</p>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</c:when>
<c:otherwise>
<dht:systemPage topText="ACCOUNT DISABLED" disableJumpTo="true">
	<p>The account for this person is currently disabled.</p>
	<p><dht:backLink/></p>	
</dht:systemPage>
</c:otherwise>
</c:choose>
</html>

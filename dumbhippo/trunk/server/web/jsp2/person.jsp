<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Mugshot</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<c:choose>
<c:when test="${!person.disabled}">
<dht:twoColumnPage alwaysShowSidebar="true">
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxWeb disableJumpTo="true">
			<dht:requireLinksPersonBean who="${person.viewedUserId}"/>
			<c:if test="${!signin.valid}">
			        <dht:linkSwarmPromo separator="true" linksLink="true"/>
			</c:if>
			<c:if test="${links.favoritePosts.resultCount > 0}">
				<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
				<dht:postList posts="${links.favoritePosts.results}" format="simple"/>
				<dht:zoneBoxSeparator/>
			</c:if>
			<dht:zoneBoxTitle>SHARED BY <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<c:choose>
				<c:when test="${links.sentPosts.resultCount > 0}">
					<dht:postList posts="${links.sentPosts.results}" format="simple"/>
				</c:when>
				<c:otherwise>
					Nothing shared by <c:out value="${person.viewedPerson.name}"/> yet!
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
		<dht:zoneBoxMusic disableJumpTo="true">
			<c:if test="${!signin.valid}">
			        <dht:musicRadarPromo separator="true" musicLink="true"/>
			</c:if>
			<dht:requireMusicPersonBean who="${person.viewedUserId}"/>
			<dht:zoneBoxTitle>CURRENT SONG FOR <c:out value="${fn:toUpperCase(person.viewedPerson.name)}"/></dht:zoneBoxTitle>
			<dh:nowPlaying userId="${person.viewedUserId}" hasLabel="false"/>
			<dht:zoneBoxSeparator/>
			
			<c:choose>
			<c:when test="${!empty person.viewedPerson.musicBioAsHtml}">
			    <dht:zoneBoxSubcolumns>
				    <dht:zoneBoxSubcolumn which="one">
			            <c:if test="${musicPerson.recentTracks.resultCount > 0}">
				            <dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
				            <c:forEach items="${musicPerson.recentTracks.results}" var="track">
				            <dht:track track="${track}" oneLine="false" playItLink="false"/>
				            </c:forEach>
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
				            <dht:zoneBoxTitle>RECENT SONGS</dht:zoneBoxTitle>
				            <c:forEach items="${musicPerson.recentTracks.results}" var="track">
				            <dht:track track="${track}" oneLine="true" playItLink="false"/>
				            </c:forEach>
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

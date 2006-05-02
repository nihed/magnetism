<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="find" class="com.dumbhippo.web.FindPage" scope="request"/>
<jsp:setProperty name="find" property="searchText" param="q"/>

<head>
	<title>Mugshot Search</title>
	<link rel="stylesheet" type="text/css" href="/css2/search.css"/>
	<dht:scriptIncludes/>
	<dht:embedObject/>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxSearch>
			<c:choose>
				<c:when test='${empty param["q"]}'>
					Please enter some search terms
				</c:when>
				<c:otherwise>
					<dht:searchSectionTitle a="dhMusicRadar">MUSIC RADAR</dht:searchSectionTitle>
			
					<dht:zoneBoxSeparator/>
					
					<dht:searchSectionTitle a="dhLinkSwarm" query='${param["q"]}' pageable="${find.posts}">LINK SWARM</dht:searchSectionTitle>
					<c:if test="${!empty find.error}">
						<c:out value="${find.error}"/>
					</c:if>
					
					<dht:postList posts="${find.posts.results}" format="enumerated" enumerateStart="${find.posts.start + 1}"/>
			
					<dht:expandablePager pageable="${find.posts}" anchor="dhLinkSwarm"/>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxSearch>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="find" class="com.dumbhippo.web.pages.FindPage" scope="request"/>
<jsp:setProperty name="find" property="searchText" param="q"/>

<head>
	<title>Mugshot Search</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/search.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<dht:embedObject/>
</head>
<dht:twoColumnPage neverShowSidebar="true" searchText='${param["q"]}'>
	<dht:contentColumn>
		<dht:zoneBoxSearch>
			<c:choose>
				<c:when test='${empty param["q"]}'>
					Please enter some search terms
				</c:when>
				<c:otherwise>
					<dht:searchSectionTitle a="dhPeople" query='${param["q"]}' pageable="${find.people}">PEOPLE</dht:searchSectionTitle>

					<c:choose>
						<c:when test="${signin.valid && find.people.totalCount > 0}">
							<c:forEach items="${find.people.results}" var="person">
								<div class="dh-item dh-item-with-photo">
									<div class="dh-image">
										<dht:headshot person="${person}"/>
									</div>
									<div class="dh-next-to-image">
										<div class="dh-title">
											<dht:personName who="${person}"/>
										</div>
										<div class="dh-info dh-blurb">
											<c:out value="${person.bioAsHtml}" escapeXml="false"/>
										</div>
									</div>
									<div class="dh-grow-div-around-floats"></div>
								</div>
							</c:forEach>
							<dht:expandablePager pageable="${find.people}" anchor="dhPeople"/>
						</c:when>
						<c:when test="${signin.valid && find.people.totalCount == 0}">
							<div>
								<i>No people found (try searching for an exact email address or AIM screen name).</i>
							</div>
						</c:when>
						<c:otherwise>
							<div>
								<i>You must be <a href="/who-are-you">logged in</a> to search for people.</i>
							</div>
						</c:otherwise>
					</c:choose>						

					<dht:zoneBoxSeparator/>
				
					<dht:searchSectionTitle a="dhMusicRadar" query='${param["q"]}' pageable="${find.tracks}">MUSIC RADAR</dht:searchSectionTitle>

					<c:if test="${!empty find.trackError}">
						<c:out value="${find.trackError}"/>
					</c:if>
					
					<c:forEach items="${find.tracks.results}" var="track">
						<dht:track track="${track}" playItLink="false"/>
					</c:forEach>

					<dht:expandablePager pageable="${find.tracks}" anchor="dhMusicRadar"/>

					<dht:zoneBoxSeparator/>
					
					<dht:searchSectionTitle a="dhLinkSwarm" query='${param["q"]}' pageable="${find.posts}">WEB SWARM</dht:searchSectionTitle>
					<c:if test="${!empty find.postError}">
						<c:out value="${find.postError}"/>
					</c:if>
					
					<dht:postList posts="${find.posts.results}" format="enumerated" enumerateStart="${find.posts.start + 1}"/>
			
					<dht:expandablePager pageable="${find.posts}" anchor="dhLinkSwarm"/>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxSearch>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

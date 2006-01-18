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

		<c:if test="${viewperson.disabled}">
			<div id="dhInformationBar">This person's account is not active</div>
		</c:if>

		<c:choose>
			<c:when test="${viewperson.disabled}">
				This page is disabled.
				<c:if test="${viewperson.self}">
					<a href="javascript:dh.actions.setAccountDisabled(false);">Enable it again</a>
				</c:if>
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
		</div>
	</div>

</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>

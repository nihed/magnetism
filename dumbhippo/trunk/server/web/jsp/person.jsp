<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewperson" class="com.dumbhippo.web.ViewPersonPage" scope="request"/>
<jsp:setProperty name="viewperson" property="viewedPersonId" param="who"/>

<c:if test="${!viewperson.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:set var="personName" value="${viewperson.person.name}" scope="page"/>
<c:set var="personId" value="${viewperson.person.viewPersonPageId}" scope="page"/>

<head>
	<title><c:out value="${personName}"/>'s Public Page</title>
	<dht:stylesheets href="person.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
	        dojo.require("dh.util");
	</script>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
	    <dht:toolbar account="false">
	    	<c:if test="${!viewperson.disabled && !viewperson.self}">
		    	<c:choose>
		    		<c:when test="${viewperson.contact}">
		    			&#151;
		    			<a class="dh-toolbar-item" style="font-weight:bold" href='javascript:dh.actions.removeContact("${personId}")'>Remove <c:out value="${personName}"/> from my friends</a>
			    	</c:when>
					<c:otherwise>
			    		&#151;
						<a class="dh-toolbar-item" style="font-weight:bold" href='javascript:dh.actions.addContact("${personId}")'>I know <c:out value="${personName}"/></a>
					</c:otherwise>
				</c:choose>
			</c:if>
		</dht:toolbar>
		<dht:infobar/>		

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
				<h2><c:out value="${personName}"/>'s Public Page</h2>
				<c:choose>
					<c:when test="${!empty viewperson.currentTrack}">
						<h5 class="dh-title">What <c:out value="${personName}"/> is listening to</h5>
						<dht:track track="${viewperson.currentTrack}" linkifySong="false" playItLink="false"/>
						<div class="dh-more-songs"><a class="dh-more-songs" href="/music?who=${personId}">More songs</a></div>
					</c:when>
					<c:when test="${empty viewperson.currentTrack && viewperson.signin.musicSharingEnabled && viewperson.self}">
						<h5 class="dh-title">What <c:out value="${personName}"/> is listening to</h5>
						<p>Play some songs in iTunes and refresh this page.</p>
					</c:when>
					<c:when test="${!viewperson.signin.musicSharingEnabled && viewperson.self}">
						<h5 class="dh-title">Share Music</h5>
						<p><dht:musicToggle musicOn="${viewperson.signin.musicSharingEnabled}"/> and try it out!(you can always turn it off later).  <a href="/music?who=${personId}">Learn more</a> about Music Sharing.</p>
					</c:when>
				</c:choose>
		
				<h3 class="dh-title"><c:out value="${personName}"/>'s Links</h3>
				<div id="dhSharesArea">
					<dht:postList posts="${viewperson.posts.list}" maxPosts="${viewperson.maxPostsShown}" posterId="${personId}" posterName="${personName}"/>
				</div>
			</c:otherwise>
		</c:choose>
	</dht:mainArea>

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
			<h5 class="dh-title">Groups They're In</h5>
			<div class="dh-groups">
			<c:choose>
				<c:when test="${viewperson.groups.size > 0}">
					<dh:entityList value="${viewperson.groups.list}" photos="true" bodyLengthLimit="8" twoLineBody="true"/>
				</c:when>
				<c:otherwise>
					<!-- FIXME: need class definition for this -->
					<div class="dh-groups-none"><c:out value="${personName}"/> has no public groups</div>
				</c:otherwise>
			</c:choose>
			</div>
		</div>
		<div class="dh-right-box dh-right-box-last">
			<c:choose>
				<c:when test="${viewperson.contacts.size > 0}">
					<h5 class="dh-title">People They Know</h5>
					<div class="dh-people">
						<dh:entityList value="${viewperson.contacts.list}" showInviteLinks="false" photos="true" bodyLengthLimit="8" longBodyLengthLimit="24" twoLineBody="true"/>
						<p class="dh-right-box-text">
				        	<c:out value="${personName}"/> has a total of ${viewperson.totalContacts} friends on DumbHippo. You can view all <c:out value="${personName}"/>'s  <a href="/friends?who=${personId}">friends</a>.
					    </p> 
					</div>
				</c:when>
				<c:otherwise>
					<%-- no contacts shown, probably because viewer isn't a contact of viewee --%>
				</c:otherwise>
			</c:choose>
		</div>
		</div>
	</div>
</dht:bodyWithAds>
</html>

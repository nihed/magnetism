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
	<title><c:out value="${personName}"/></title>
	<dht:stylesheets href="person.css" iehref="person-iefixes.css" />
	<dht:scriptIncludes/>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<img src="/images/dh-logo.jpg"/>

	    <dht:toolbar>
	    	<c:if test="${!viewperson.disabled}">
		    	<c:choose>
		    		<c:when test="${viewperson.contact}">
		    			&#151;
		    			<a class="dh-toolbar-item" style="font-weight:bold" href='javascript:dh.actions.removeContact("${personId}")'>Remove <c:out value="${personName}"/> from my contact list</a>
			    	</c:when>
		    		<c:otherwise>
			    		&#151;
						<a class="dh-toolbar-item" style="font-weight:bold" href='javascript:dh.actions.addContact("${personId}")'>I know <c:out value="${personName}"/></a>
					</c:otherwise>
				</c:choose>
			</c:if>
		</dht:toolbar>

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
			<h2 class="dh-title">Links Shared By <c:out value="${personName}"/></h2>

			<div id="dhSharesArea">
				<dht:postList posts="${viewperson.posts.list}" maxPosts="${viewperson.maxPostsShown}" posterId="${personId}" posterName="${personName}"/>
			</div>
			<div style="position:absolute;bottom:60px;right:30px;"><input style="width:5em;text-align:center;border:1px solid black;padding:0;"type="text" value="Search"> More Shares</div>
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
			<h5 class="dh-title">Groups They're In</h5>
			<div class="dh-groups">
			<c:choose>
				<c:when test="${viewperson.groups.size > 0}">
					<dh:entityList value="${viewperson.groups.list}" photos="true"/>
				</c:when>
				<c:otherwise>
					<!-- FIXME: need class definition for this -->
					<div class="dh-groups-none">Invite <c:out value="${personName}"/> To Your Groups!!</div>
				</c:otherwise>
			</c:choose>
			</div>
		</div>
		<div class="dh-right-box dh-right-box-last">
			<h5 class="dh-title">People You Know</h5>
			<p class="dh-invites">
			<c:if test="${home.invitations > 0}">
			You can <a class="dh-invites-left" href="/invite">invite</a> ${home.invitations} more people to join DumbHippo.
			</c:if>
			</p>
			<div class="dh-people">
			<c:choose>
				<c:when test="${home.contacts.size > 0}">
					<dh:entityList value="${home.contacts.list}" showInviteLinks="${home.invitations > 0}" photos="true"/>
				</c:when>
				<c:otherwise>
					<!-- FIXME: need class definition for this -->
					<div class="dh-friends-none">You Need Peeps!!</div>
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

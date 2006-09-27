<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<head>
	<title>Mugshot Home</title>
	<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/home.css"/>
	<dht:scriptIncludes/>
	<dht:faviconIncludes/>
</head>

<dht:requirePersonBean asOthersWouldSee="false" needExternalAccounts="true"/>
<jsp:setProperty name="person" property="viewedUserId" value="${signin.user.id}"/>

<body class="dh-gray-background-page dh-home-page">
	<div id="dhPage">
		<dht3:header/>
		<dht3:shinyBox color="grey">
			<dht3:personHeader who="${person.viewedPerson}"><a href="/account">Edit my Mugshot account</a></dht3:personHeader>
		</dht3:shinyBox>
		
		<c:choose>
			<c:when test="${person.contacts.size > 0}">
				<c:forEach items="${person.contacts.list}" end="2" var="person">
					<dht3:shinyBox color="grey">				
						<dht3:personHeader who="${person}"><a href="/">Remove from friends</a> | <a href="/">Invite to a group</a></dht3:personHeader>
					</dht3:shinyBox>
				</c:forEach>
				<c:if test="${person.contacts.size > 3}">
			       <dht:moreLink moreName="More (${person.contacts.size})" more="${url}"/>
				</c:if>  
			</c:when>
			<c:otherwise>
			    <c:choose>
				    <c:when test="${person.signin.user.account.invitations > 0}">
					    <p>Email <a href="/invitation">invites</a> to some friends</p>
				    </c:when>
				    <c:otherwise>
					    <p>A loner huh?</p>
				    </c:otherwise>
			    </c:choose>
			</c:otherwise>
		</c:choose>		
		<dht:footer/>
	</div>
</body>
</html>

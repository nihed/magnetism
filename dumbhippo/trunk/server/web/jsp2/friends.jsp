<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test='${empty param["who"] && !signin.valid}'>
	<dht:errorPage>I don't know whose friends you are looking for!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test='${empty param["who"]}'>
		<c:set var="selfView" value='true' scope="page"/>
		<c:set var="who" value='${signin.user.id}' scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="selfView" value='false' scope="page"/>
		<c:set var="who" value='${param["who"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>

<c:if test="${!person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s Friends</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<c:if test="${webVersion == 3}">
		<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/header.css"/>		
	</c:if>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${person.viewedUserId}"/>
	<dht:contentColumn>
		<dht:zoneBoxFriends back='true'>
			<dht:zoneBoxTitle>ALL <c:out value='${selfView ? "YOUR " : "" }'/>FRIENDS</dht:zoneBoxTitle>
			<c:choose>  
		        <c:when test="${person.contacts.size > 0}">             
			        <dht:twoColumnList>
				        <c:forEach items="${person.pageableContacts.results}" var="person">
					        <dht:personItem who="${person}" invited="true"/>
				        </c:forEach>
			        </dht:twoColumnList>
                </c:when>
                <c:when test="${selfView}">
                    <c:choose>
				        <c:when test="${person.signin.user.account.invitations > 0}">
					        Email <a href="/invitation">invites</a> to some friends
				        </c:when>
				        <c:otherwise>
					        A loner huh?
				        </c:otherwise>
			        </c:choose>
			    </c:when>
			    <c:otherwise>
			        <%-- Contacts can be 0 because there are no contacts or because the viewer can not see --%>
			        <%-- this person's friends. In either case, if this is not a self view, the statement below is valid. --%>
			        You cannot view this person's friends.
			    </c:otherwise>
			</c:choose>            
		    <dht:expandablePager pageable="${person.pageableContacts}" anchor="dhFriends"/>
		    <c:if test="${selfView && (person.followers.size > 0)}">
		        <dht:zoneBoxSeparator/>
			    <dht:zoneBoxTitle title="People who have added you as a friend, but who you did not add as your friends." >ALL YOUR FOLLOWERS</dht:zoneBoxTitle>
			    <dht:twoColumnList>
				    <c:forEach items="${person.pageableFollowers.results}" var="person">
					    <dht:personFollowerItem who="${person}"/>
				    </c:forEach>
			    </dht:twoColumnList>
		        <dht:expandablePager pageable="${person.pageableFollowers}" anchor="dhFollowers"/>
		    </c:if>        
		</dht:zoneBoxFriends>		
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

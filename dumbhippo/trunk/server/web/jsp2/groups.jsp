<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test='${!empty param["publiconly"]}'>
	<c:set var="publiconly" value='${param["publiconly"]}' scope="page"/>
</c:if>

<c:choose>
	<c:when test='${empty param["who"]}'>
	    <c:choose>
	        <c:when test='${signin.valid}'>
	            <c:set var="who" value='${signin.user.id}' scope="page"/>
            </c:when>
            <c:otherwise>
 	            <c:set var="publiconly" value='true' scope="page"/>
            </c:otherwise>
		</c:choose>
	</c:when>
	<c:otherwise>
		<c:set var="who" value='${param["who"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<dh:bean id="person" class="com.dumbhippo.web.pages.GroupsPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>

<c:if test="${empty publiconly} && ${!person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test="${!empty publiconly}">
	    <c:set var="title" value="All Public Groups" scope="page"/>
	</c:when>
	<c:when test="${person.self}">
	    <c:set var="title" value="Your Groups" scope="page"/>
	</c:when>
	<c:otherwise>
	    <c:set var="title" value="${person.viewedPerson.name}'s Groups" scope="page"/>
	</c:otherwise>
</c:choose> 

<head>
	<title><c:out value="${title}"/></title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript" src="/javascript/${buildStamp}/dh/groups.js"></script>	
</head>
<dht:twoColumnPage>
    <c:if test="${!empty who}">
	    <dht:sidebarPerson who="${person.viewedUserId}"/>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxGroups back='true'>
			<dht:zoneBoxTitle>
			    <c:out value="${fn:toUpperCase(title)}"/>
			</dht:zoneBoxTitle>
			<dht:twoColumnList>
			    <c:choose>
			    	<c:when test="${!empty publiconly}">
			    		<c:forEach items="${person.allPublicGroups.list}" var="group">
					        <dht:groupItem group="${group}"/>
				        </c:forEach>
			    	</c:when>
			    	<c:otherwise>
				        <c:forEach items="${person.groups.list}" var="group">
					        <dht:groupItem group="${group}"/>
				        </c:forEach>
				    </c:otherwise>
				</c:choose>
			</dht:twoColumnList>
			<c:if test="${empty publiconly} && ${person.self}">
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle>INVITED GROUPS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${person.invitedGroups.list}" var="group">
						<dht:groupItem group="${group}" controls="true"/>
					</c:forEach>	
				</dht:twoColumnList>
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle>FOLLOWED GROUPS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${person.followedGroups.list}" var="group">
						<dht:groupItem group="${group}"/>
					</c:forEach>	
				</dht:twoColumnList>				
			</c:if>
		</dht:zoneBoxGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
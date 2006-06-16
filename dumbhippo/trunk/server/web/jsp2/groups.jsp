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

<c:if test="${empty publiconly && !person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test="${!empty publiconly}">
	    <c:set var="pagetitle" value="All Public Groups" scope="page"/>
	    <c:set var="intitle" value="All Public Groups" scope="page"/>
	</c:when>
	<c:when test="${person.self}">
	    <c:set var="pagetitle" value="Your Groups" scope="page"/>
	    <c:set var="intitle" value="Groups You're In" scope="page"/>
	    <c:set var="followtitle" value="Groups You Follow" scope="page"/>
	</c:when>
	<c:otherwise>
	    <c:set var="pagetitle" value="${person.viewedPerson.name}'s Groups" scope="page"/>
	    <c:set var="intitle" value="Groups ${person.viewedPerson.name} Is In" scope="page"/>
	    <c:set var="followtitle" value="Groups ${person.viewedPerson.name} Follows" scope="page"/>
	</c:otherwise>
</c:choose> 

<head>
	<title><c:out value="${pagetitle}"/></title>
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
			    <c:out value="${fn:toUpperCase(intitle)}"/>
			</dht:zoneBoxTitle>
			<dht:twoColumnList>
			    <c:choose>
			    	<c:when test="${!empty publiconly}">
			    		<c:forEach items="${person.allPublicGroups.list}" var="group">
					        <dht:groupItem group="${group}"/>
				        </c:forEach>
			    	</c:when>
			    	<c:otherwise>
			    	    <c:choose>
			    	        <c:when test="${person.groups.size > 0}">
				                <c:forEach items="${person.groups.list}" var="group">
					                <dht:groupItem group="${group}"/>
				                </c:forEach>
				            </c:when>
				            <c:otherwise>
				                Not in any groups.
				            </c:otherwise>
				        </c:choose>		
				    </c:otherwise>
				</c:choose>
			</dht:twoColumnList>
			<c:if test="${empty publiconly && person.self && person.invitedGroups.size > 0}">
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle>GROUPS YOU'VE BEEN INVITED TO JOIN</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${person.invitedGroups.list}" var="group">
						<dht:groupItem group="${group}" controls="true"/>
					</c:forEach>	
				</dht:twoColumnList>
			</c:if>
			<c:if test="${empty publiconly}">
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle>${fn:toUpperCase(followtitle)}</dht:zoneBoxTitle>
				<c:choose>
				    <c:when test="${person.followedGroups.size > 0}">
				        <dht:twoColumnList>
					        <c:forEach items="${person.followedGroups.list}" var="group">
						        <dht:groupItem group="${group}"/>
					        </c:forEach>	
				        </dht:twoColumnList>
				    </c:when>
				    <c:otherwise>
				        Not following any groups.
				    </c:otherwise>
				</c:choose>			
			</c:if>
			<c:if test="${empty publiconly && person.self && person.invitedToFollowGroups.size > 0}">
				<dht:zoneBoxSeparator/>			
				<dht:zoneBoxTitle>GROUPS YOU'VE BEEN INVITED TO FOLLOW</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${person.invitedToFollowGroups.list}" var="group">
						<dht:groupItem group="${group}" controls="true"/>
					</c:forEach>	
				</dht:twoColumnList>
			</c:if>
		</dht:zoneBoxGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
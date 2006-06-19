<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:choose>
	<c:when test='${empty param["who"]}'>
	    <c:if test='${signin.valid}'>
	        <c:set var="who" value='${signin.user.id}' scope="page"/>
        </c:if>
	</c:when>
	<c:otherwise>
		<c:set var="who" value='${param["who"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<c:choose>
    <c:when test='${empty param["mode"]}'>
        <c:choose>
            <c:when test="${empty who}">
                <c:set var="mode" value='public' scope="page"/>
            </c:when>
            <c:otherwise>
                <c:set var="mode" value='member' scope="page"/>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:otherwise>
	    <c:set var="mode" value='${param["mode"]}' scope="page"/>
	</c:otherwise>
</c:choose>

<dh:bean id="person" class="com.dumbhippo.web.pages.GroupsPage" scope="page"/>
<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>
<jsp:setProperty name="person" property="mode" value="${mode}"/>

<c:if test="${mode != 'public' && !person.valid}">
	<dht:errorPage>There's nobody here!</dht:errorPage>
</c:if>

<c:choose>
	<c:when test="${mode == 'public'}">
	    <c:set var="pagetitle" value="All Public Groups" scope="page"/>
	</c:when>
	<c:when test="${mode == 'member'}">
	    <c:choose>
	        <c:when test="${person.self}">
	            <c:set var="pagetitle" value="Your Groups" scope="page"/>
	        </c:when>
	        <c:otherwise>
	            <c:set var="pagetitle" value="${person.viewedPerson.name}'s Groups" scope="page"/>
	        </c:otherwise>
	    </c:choose>
	</c:when>
	<c:when test="${mode == 'invited'}">
	    <c:set var="pagetitle" value="Groups You've Been Invited To" scope="page"/>
	</c:when>
	<c:when test="${mode == 'followed'}">
	    <c:choose>
	        <c:when test="${person.self}">
	            <c:set var="pagetitle" value="Groups You Follow" scope="page"/>
	        </c:when>
	        <c:otherwise>
	            <c:set var="pagetitle" value="Groups ${person.viewedPerson.name} Follows" scope="page"/>
	        </c:otherwise>
	    </c:choose>	</c:when>
	<c:when test="${mode == 'invitedtofollow'}">
	    <c:set var="pagetitle" value="Suggested Groups To Follow" scope="page"/>
	</c:when>
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
			    <c:out value="${fn:toUpperCase(pagetitle)}"/>
			</dht:zoneBoxTitle>
			<c:if test="${mode == 'public'}">
				<c:choose>
				    <c:when test="${person.allPublicGroups.size > 0}">
				        <dht:twoColumnList>
					    <c:forEach items="${person.allPublicGroups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>    
						</dht:twoColumnList>
					</c:when>
				    <c:otherwise>
				        No public groups.
				    </c:otherwise>
				</c:choose>
			</c:if>
			<c:if test="${mode == 'member'}">
				<c:choose>
				    <c:when test="${person.groups.size > 0}">
				        <dht:twoColumnList>
					    <c:forEach items="${person.groups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>
				        </dht:twoColumnList>
					</c:when>
				    <c:otherwise>
				        Not in any groups.
				    </c:otherwise>
				</c:choose>
			</c:if>
			<c:if test="${mode == 'invited' && person.self}">
				<c:choose>
				    <c:when test="${person.invitedGroups.size > 0}">
				        <dht:twoColumnList>
					    <c:forEach items="${person.invitedGroups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>
					    </dht:twoColumnList>
					</c:when>
				    <c:otherwise>
				        No outstanding group invitations.
				    </c:otherwise>
				</c:choose>
			</c:if>
			<c:if test="${mode == 'followed'}">
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
			<c:if test="${mode == 'invitedtofollow' && person.self}">
				<c:choose>
				    <c:when test="${person.invitedToFollowGroups.size > 0}">
				        <dht:twoColumnList>
					    <c:forEach items="${person.invitedToFollowGroups.list}" var="group">
						    <dht:groupItem group="${group}" controls="true"/>
					    </c:forEach>
					    </dht:twoColumnList>
					</c:when>
				    <c:otherwise>
				        No outstanding invitations to follow groups.
				    </c:otherwise>
				</c:choose>
			</c:if>
		</dht:zoneBoxGroups>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
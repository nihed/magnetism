<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>
<%-- TODO: should be able to remove this once the sidebar with only some members shown is not used anywhere --%>
<jsp:setProperty name="group" property="allMembers" value="true"/>

<c:if test="${empty group.viewedGroup}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<c:set var="pageName" value="${group.viewedGroup.name} Members" scope="page"/>

<head>
	<title><c:out value="${pageName}"/> - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="members">
	<dht3:pageSubHeader title="${group.viewedGroup.name} - ${group.private ? 'Private Group' : 'Public Group'}" privatePage="${group.private}">
		<dht3:groupRelatedPagesTabs group="${group.viewedGroup}" selected="members"/>
	</dht3:pageSubHeader>
	
	<dht3:shinyBox color="grey">
        <div class="dh-page-shinybox-title-large"><span>Group Members (<c:out value="${group.activeMembers.size}"/>)</span></div>
        <c:choose>  
            <c:when test="${group.activeMembers.size > 0}">
          	    <c:forEach items="${group.pageableActiveMembers.results}" var="person">
			        <dht3:personItem who="${person}"/>
		        </c:forEach>
		        <div class="dh-grow-div-around-floats"><div></div></div>
		        <dht:expandablePager pageable="${group.pageableActiveMembers}" anchor="dhActiveMembers"/>
            </c:when>
			<c:otherwise>
			    Nobody in this group.
			</c:otherwise>
	    </c:choose>            
    </dht3:shinyBox>
    
    <%-- Only public groups can have followers --%>
    <c:if test="${group.public}">
        <dht3:shinyBox color="grey">
            <div class="dh-page-shinybox-title-large"><span>Group Followers (<c:out value="${group.followers.size}"/>)</span></div>
            <c:choose>  
                <c:when test="${group.followers.size > 0}">
          	        <c:forEach items="${group.pageableFollowers.results}" var="person">
			            <dht3:personItem who="${person}"/>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableFollowers}" anchor="dhFollowers"/>
                </c:when>
			    <c:otherwise>
			        This group has no followers, everyone is already in!
			    </c:otherwise>
	        </c:choose>            
        </dht3:shinyBox>
    </c:if>
   
    <c:if test="${group.member}">
        <dht3:shinyBox color="grey">
            <div class="dh-page-shinybox-title-large">
                <span>People Invited to the Group (<c:out value="${group.invitedMembers.size}"/>)</span>
                <a class="dh-underlined-link dh-page-shinybox-subtitle" href="/group-invitation?group=${group.viewedGroupId}">Invite people!</a>
            </div>
            <c:choose>  
                <c:when test="${group.invitedMembers.size > 0}">
          	        <c:forEach items="${group.pageableInvitedMembers.results}" var="person">
			            <dht3:personItem who="${person}"/>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableInvitedMembers}" anchor="dhInvitedMembers"/>
                </c:when>
			    <c:otherwise>
			        This group has no outstanding invitations. <a href="/group-invitation?group=${group.viewedGroupId}">Invite people!</a>
			    </c:otherwise>
	        </c:choose>            
        </dht3:shinyBox>
    </c:if>

    <%-- Only public groups can have followers; both members and followers can see people who are invited to follow --%>
    <c:if test="${group.public && (group.member || group.follower)}">
        <dht3:shinyBox color="grey">
            <div class="dh-page-shinybox-title-large">
                <span>People Invited to Follow the Group (<c:out value="${group.invitedFollowers.size}"/>)</span>
                <c:if test="${group.follower}">
                    <a class="dh-underlined-link dh-page-shinybox-subtitle" href="/group-invitation?group=${group.viewedGroupId}">Invite people to follow!</a>
                </c:if>    
            </div>
            <c:choose>  
                <c:when test="${group.invitedFollowers.size > 0}">
          	        <c:forEach items="${group.pageableInvitedFollowers.results}" var="person">
			            <dht3:personItem who="${person}"/>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableInvitedFollowers}" anchor="dhInvitedFollowers"/>
                </c:when>
			    <c:otherwise>
			        This group has no outstanding invitations to follow. <c:if test="${group.follower}"><a href="/group-invitation?group=${group.viewedGroupId}">Invite people to follow!</a></c:if>
			    </c:otherwise>
	        </c:choose>            
        </dht3:shinyBox>
    </c:if>
       
</dht3:page>

</html>
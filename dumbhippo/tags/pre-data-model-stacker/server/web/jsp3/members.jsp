<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>

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
	
	<c:if test="${!group.viewedGroup.canSeeContent && dh:enumIs(group.viewedGroup.status, 'REMOVED')}">
	    <dht3:shinyBox color="orange">
	        <div class="dh-page-shinybox-title-large"><span>Group Members</span></div>
	        <dh:script module="dh.actions"/>
			You need to <dht:actionLink oneLine="true" href="javascript:dh.actions.joinGroup('${group.viewedGroupId}')" title="Rejoin this group">rejoin this group</dht:actionLink> to see the group members.
	    </dht3:shinyBox>
	</c:if>
		  
	<c:if test="${group.viewedGroup.canSeeContent}">        
	    <dht3:shinyBox color="orange">
            <div class="dh-page-shinybox-title-large"><span>Group Members (<c:out value="${group.activeMembers.size}"/>)</span></div>
            <c:choose>  
                <c:when test="${group.activeMembers.size > 0}">
          	        <c:forEach items="${group.pageableActiveMembers.results}" var="person">
			            <dht3:personItem who="${person}" useSpecialControls="true">
			                <c:if test="${person.viewOfSelf}">
			        	        <dh:script module="dh.actions"/>
						        <dht:actionLink oneLine="true" href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')" title="Stop receiving stack activity from this group">Leave Group</dht:actionLink>
			                </c:if>
			            </dht3:personItem>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableActiveMembers}" anchor="dhActiveMembers"/>
                </c:when>
			    <c:otherwise>
			        Nobody in this group.
			    </c:otherwise>
	        </c:choose>            
        </dht3:shinyBox>
    </c:if>
    
    <%-- Only public "by invitation" groups can have followers --%>
    <c:if test="${group.publicInvite}">
        <dht3:shinyBox color="orange">
            <div class="dh-page-shinybox-title-large"><span>Group Followers (<c:out value="${group.followers.size}"/>)</span></div>
            <c:choose>  
                <c:when test="${group.followers.size > 0}">
          	        <c:forEach items="${group.pageableFollowers.results}" var="person">
			            <dht3:personItem who="${person}" useSpecialControls="true">
                            <c:if test="${person.viewOfSelf}">
                                <dh:script module="dh.actions"/>
						        <dht:actionLink oneLine="true" href="javascript:dh.actions.leaveGroup('${group.viewedGroupId}')" title="Stop following this group">Stop following</dht:actionLink>
				            </c:if>
				            <c:if test="${group.canAddMembers}">
				                <dh:script module="dh.actions"/>
				                <dht:actionLink oneLine="true" href="javascript:dh.actions.addMember('${group.viewedGroupId}', '${person.identifyingGuid}', function () { dh.util.refresh() })" title="Invite this person to be a member in this group">Invite to group</dht:actionLink>		  	    	
				            </c:if>
			            </dht3:personItem>			                 
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
   
    <c:if test="${group.viewedGroup.status.participant}">
        <dht3:invitedMembers/>
    </c:if>

    <%-- Only public "by invitation" groups can have followers; both members and followers can see people who are invited to follow --%>
    <c:if test="${group.publicInvite && (group.viewedGroup.status.participant || group.viewedGroup.status.followingParticipant)}">
        <dht3:invitedFollowers/>
    </c:if>
       
</dht3:page>

</html>
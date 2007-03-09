<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="forInvitationPage" required="false" type="java.lang.Boolean" %>

<c:set var="titleClass" value="dh-page-shinybox-title-large"/>
<c:if test="${forInvitationPage}">
    <c:set var="titleClass" value="dh-section-header"/>    
</c:if>
    
<dht3:shinyBox color="grey">
    <div class="${titleClass}">
                <span>People Invited to Follow the Group (<c:out value="${group.invitedFollowers.size}"/>)</span>
                <c:if test="${!forInvitationPage && group.follower}">
                    <a class="dh-underlined-link dh-page-shinybox-subtitle" href="/group-invitation?group=${group.viewedGroupId}">Invite people to follow!</a>
                </c:if>    
            </div>
            <c:choose>  
                <c:when test="${group.invitedFollowers.size > 0}">
          	        <c:forEach items="${group.pageableInvitedFollowers.results}" var="person">
			            <dht3:personItem who="${person}" useSpecialControls="true">
			                <c:choose>
			                    <c:when test="${group.canAddMembers}">
			                        <dh:script module="dh.actions"/>
				                    <dht:actionLink oneLine="true" href="javascript:dh.actions.addMember('${group.viewedGroupId}', '${person.identifyingGuid}', function () { dh.util.refresh() })" title="Invite this person to be a member in this group">Invite to group</dht:actionLink>		  	    			                   
			                    </c:when>			               
			                    <c:when test="${group.canAddFollowers}">
			                        <a href="javascript:window.open('/group-invitation?group=${group.viewedGroupId}&invitee=${person.identifyingGuid}', '_self');">
			                            Re-send invitation
			                        </a>
			                    </c:when>
			                </c:choose> 
			                <c:if test="${person.viewerCanRemoveInvitation}">
			                    <c:if test="${group.canAddMembers || group.canAddFollowers}">
			                        |
			                    </c:if>    
                                <dh:script module="dh.actions"/>
				                <dht:actionLink oneLine="true" href="javascript:dh.actions.removeGroupInvitee('${group.viewedGroupId}', ${person.email});" title="Remove invitation to follow this group for this person">Remove invitation</dht:actionLink>		  	    				                    
			                </c:if>			                
			            </dht3:personItem>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableInvitedFollowers}" anchor="dhInvitedFollowers"/>
                </c:when>
			    <c:otherwise>
			        This group has no outstanding invitations to follow. <c:if test="${group.follower}"><a href="/group-invitation?group=${group.viewedGroupId}">Invite people to follow!</a></c:if>
			    </c:otherwise>
	        </c:choose>            
</dht3:shinyBox>
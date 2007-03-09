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
                <span>People Invited to the Group (<c:out value="${group.invitedMembers.size}"/>)</span>
                <c:if test="${!forInvitationPage}">
                    <a class="dh-underlined-link dh-page-shinybox-subtitle" href="/group-invitation?group=${group.viewedGroupId}">Invite people!</a>
                </c:if>
            </div>
            <c:choose>  
                <c:when test="${group.invitedMembers.size > 0}">
          	        <c:forEach items="${group.pageableInvitedMembers.results}" var="person">
			            <dht3:personItem who="${person}" useSpecialControls="true">
			                <c:if test="${group.canAddMembers}">
			                    <a href="javascript:window.open('/group-invitation?group=${group.viewedGroupId}&invitee=${person.identifyingGuid}', '_self');">
			                        Re-send invitation
			                    </a>
			                </c:if>    
			                <c:if test="${person.viewerCanRemoveInvitation}">
			                    <c:if test="${group.canAddMembers}">
			                        |
			                    </c:if>    
                                <dh:script module="dh.actions"/>
				                <dht:actionLink oneLine="true" href="javascript:dh.actions.removeGroupInvitee('${group.viewedGroupId}', ${person.email});" title="Cancel group invitation for this person">Remove invitation</dht:actionLink>		  	    				                    
			                </c:if>
			            </dht3:personItem>
		            </c:forEach>
		            <div class="dh-grow-div-around-floats"><div></div></div>
		            <dht:expandablePager pageable="${group.pageableInvitedMembers}" anchor="dhInvitedMembers"/>
                </c:when>
			    <c:otherwise>
			        This group has no outstanding invitations. <a href="/group-invitation?group=${group.viewedGroupId}">Invite people!</a>
			    </c:otherwise>
	        </c:choose>            
        </dht3:shinyBox>
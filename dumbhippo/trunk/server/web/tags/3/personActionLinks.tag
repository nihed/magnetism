<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="showHomeUrl" required="false" type="java.lang.Boolean" %>

<c:if test="${empty showHomeUrl}">
	<c:set var="showHomeUrl" value="true"/>
</c:if> 

<c:choose>
    <c:when test="${signin.active}">
        <c:choose>
            <c:when test="${who.liveUser == null}">
            	<a href="javascript:window.open('/invitation?invitee=' + encodeURIComponent('${who.name}'), '_self');">
        	        <c:choose>
        	            <c:when test="${who.invitationView == null}">
        	                Send invitation
        	            </c:when>
        	            <c:when test="${who.invitationView != null && who.invitationView.invite.valid}">
        	                Re-send invitation
        	                <c:set var="invitationText" value=" and cancel their invitation"/>
        	            </c:when>
        	            <c:otherwise>
        	                Send a new invitation
        	            </c:otherwise>
        	        </c:choose>                
        	    </a> 
        	    <c:if test="${who.invitationView != null}">
        	        | 
        	        <dh:script module="dh.actions"/>
  				    <dht:actionLink oneLine="true" href="javascript:dh.actions.removeInvitedContact('${who.identifyingGuid}')" title="Remove contact${invitationText}">Remove contact</dht:actionLink>   
  				</c:if>         	    
        	</c:when>
        	<c:when test="${who.viewOfSelf}">
        	    <div>
					<a href="/account">Edit my Mugshot account</a>        		
			    </div>
        	</c:when>
        	<c:otherwise>
	            <c:choose>
  			        <c:when test="${who.contactOfViewer}">
  			            <dh:script module="dh.actions"/>
  				        <dht:actionLink oneLine="true" href="javascript:dh.actions.removeContact('${who.viewPersonPageId}')" title="Remove this person from your network">Remove from network</dht:actionLink>
	   	            </c:when>
	        	    <c:otherwise>
	        	        <dh:script module="dh.actions"/>
					    <dht:actionLink oneLine="true" href="javascript:dh.actions.addContact('${who.viewPersonPageId}')" title="Add this person to your network">Add to network</dht:actionLink>
				    </c:otherwise>
				</c:choose>
				<%-- A page where you can select a group you want to invite someone to is not yet implemented. --%>
				<%--| <a href="/group-invitation">Invite to a group</a> --%>
			</c:otherwise>
		</c:choose>
	</c:when>
    <c:when test="${showHomeUrl}">
		<a href="${who.homeUrl}">Mugshot page</a>
    </c:when>
</c:choose> 
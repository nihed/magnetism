<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="privatePage" required="false" type="java.lang.Boolean" %>
<%@ attribute name="titleLink" required="false" type="java.lang.String" %>
<%@ attribute name="titleLinkText" required="false" type="java.lang.String" %>
<%@ attribute name="offerInviteFriendsLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="offerCreateGroupLink" required="false" type="java.lang.Boolean" %>

<c:if test="${signin.active}">
    <c:choose> 
        <c:when test="${offerInviteFriendsLink}">     
	        <c:set var="invitations" value="${signin.user.account.invitations}" scope="page"/>
	        <c:if test="${invitations > 0}">
		        <c:set var="titleLink" value="/invitation"/>
		        <c:if test="${invitations > 1}">
	                <c:set var="plural" value="s"/>
		        </c:if>
	            <c:set var="titleLinkText" value="Invite friends (${invitations} invitation${plural} left)"/>
	        </c:if>
	    </c:when>
        <c:when test="${offerCreateGroupLink}">
            <c:set var="titleLink" value="/create-group"/>
	        <c:set var="titleLinkText" value="Create a Group"/>  
        </c:when>
    </c:choose>       	    
</c:if>

<div class="dh-page-title-container">
	<c:if test="${!empty privatePage && privatePage}">
		<dh:png src="/images3/${buildStamp}/private_icon.png" style="width: 12; height: 14; overflow: hidden;"/>
	</c:if>
	<span class="dh-page-title"><c:out value="${title}"/></span>
	<c:if test="${!empty titleLink}">
		<span class="dh-page-title-link"><a href="${titleLink}"><c:out value="${titleLinkText}"/></a></span>
	</c:if>
	<div class="dh-page-options-container">	
		<div class="dh-page-options"><jsp:doBody/></div>
	</div>
</div>
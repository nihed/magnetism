<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.Integer" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>
<%@ attribute name="showHomeUrl" required="false" type="java.lang.Boolean" %>

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<c:if test="${empty showHomeUrl}">
	<c:set var="showHomeUrl" value="true"/>
</c:if>

<dht3:shinyBox color="orange" width="${width}" floatSide="${floatSide}">
    <dht3:groupHeader who="${who}" embedVersion="${embedVersion}">
        <c:choose>
            <c:when test="${signin.valid}">
	            <c:choose>
	        	    <c:when test="${who.active}">
						 <dht:actionLink href="javascript:dh.actions.leaveGroup('${who.identifyingGuid}')" title="Stop receiving stack activity from this group">Leave Group</dht:actionLink>
					</c:when>
					<c:when test="${who.invited}">
						 <dht:actionLink href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="You were invited to join this group">Join group (invited)</dht:actionLink>					
					</c:when>
					<c:when test="${dh:enumIs(who.status, 'REMOVED')}">
						 <dht:actionLink href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="Rejoin this group">Join group</dht:actionLink>
					</c:when>	
					<c:when test="${dh:enumIs(who.status, 'NONMEMBER') && who.canJoin}">
						<dht:actionLink href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="Follow stack activity in this group">Follow group</dht:actionLink>
					</c:when>				
	        	    <c:otherwise>
				    </c:otherwise>
			    </c:choose>				
            </c:when>
            <c:otherwise>
            	<c:if test="${showHomeUrl}">
			        <a href="${who.homeUrl}">Group page</a>
			    </c:if>
		    </c:otherwise>
		</c:choose>        
	</dht3:groupHeader>
    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}"/>    
</dht3:shinyBox>		
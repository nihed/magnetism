<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.GroupView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.String" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="disableLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>
<%@ attribute name="showHomeUrl" required="false" type="java.lang.Boolean" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>


<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<c:if test="${empty showHomeUrl}">
	<c:set var="showHomeUrl" value="true"/>
</c:if>

<dht3:shinyBox color="orange" width="${width}" floatSide="${floatSide}">
    <dht3:groupHeader who="${who}" embedVersion="${embedVersion}" anchor="${stackType}" disableLink="${disableLink}">
        <c:choose>
            <c:when test="${signin.valid}">
	            <c:choose>
	        	    <c:when test="${who.active}">
	        	    	<dh:script module="dh.actions"/>
						<dht:actionLink oneLine="true" href="javascript:dh.actions.leaveGroup('${who.identifyingGuid}')" title="Stop receiving stack activity from this group">Leave Group</dht:actionLink>
					</c:when>
					<c:when test="${who.invited}">
						<dh:script module="dh.actions"/>
						 <dht:actionLink oneLine="true" href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="You were invited to join this group">Join group (invited)</dht:actionLink>					
					</c:when>
					<c:when test="${dh:enumIs(who.status, 'REMOVED')}">
						<dh:script module="dh.actions"/>
						 <dht:actionLink oneLine="true" href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="Rejoin this group">Join group</dht:actionLink>
					</c:when>
					<c:when test="${dh:enumIs(who.status, 'NONMEMBER') && who.canJoin}">
						<dh:script module="dh.actions"/>
						<dht:actionLink oneLine="true" href="javascript:dh.actions.joinGroup('${who.identifyingGuid}')" title="Follow stack activity in this group">Follow group</dht:actionLink>
					</c:when>
					<c:when test="${dh:enumIs(who.status, 'FOLLOWER')}">
						<dh:script module="dh.actions"/>
						<dht:actionLink oneLine="true" href="javascript:dh.actions.leaveGroup('${who.identifyingGuid}')" title="Stop following this group">Stop Following</dht:actionLink>
					</c:when>
	        	    <c:otherwise>
				    </c:otherwise>
			    </c:choose>	
			<c:if test="${who.status.canShare}">
				| <dht:actionLink oneLine="true" href="/group-invitation?group=${who.identifyingGuid}" title="Invite other people to this group">Invite People</dht:actionLink>
			</c:if>	
			<c:if test="${who.active}">
				<dht:actionLinkChat oneLine="true" chatId="${who.identifyingGuid}" kind="group" prefix="| " />
			</c:if>
            </c:when>
            <c:otherwise>
            	<c:if test="${showHomeUrl}">
			        <a href="${who.homeUrl}">Group page</a>
			    </c:if>
		    </c:otherwise>
		</c:choose>        
	</dht3:groupHeader>
	<c:choose>
		<c:when test="${!shortVersion}">
		    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}"/>
    	</c:when> 
    	<c:otherwise>
		    <div class="dh-back">
	    	    <a href="/group?who=${group.viewedGroupId}">Back to <c:out value="${group.viewedGroup.name}"/>'s Home</a>
		    </div>
    	</c:otherwise>
	</c:choose>
</dht3:shinyBox>		

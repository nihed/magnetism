<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.blocks.FacebookBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}">
	<dht3:blockHeader block="${block}" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			<table cellspacing="0" cellpadding="0">
			<tr>
			<td class="dh-stacker-block-title">
			    <c:if test="${!oneLine}">  
			        <span class="dh-stacker-block-title-type">Facebook:</span>
			    </c:if>     
			</td>    
			<td>
			<div class="dh-stacker-block-title-facebook-events">
				<c:forEach items="${block.facebookEvents}" var="event" varStatus="eventIdx" end="3">
					<c:choose>
						<c:when test="${dh:enumIs(event.eventType, 'NEW_TAGGED_PHOTOS_EVENT')}">
							<div><a class="dh-underlined-link" href="http://www.facebook.com"><dht3:plural n="${event.count}" s="tagged picture"/></a></div>
						</c:when>
						<c:when test="${dh:enumIs(event.eventType, 'UNSEEN_POKES_EVENT')}">
							<div><a class="dh-underlined-link" href="http://www.facebook.com"><dht3:plural n="${event.count}" s="unseen poke"/></a></div>
						</c:when>
						<c:when test="${dh:enumIs(event.eventType, 'UNREAD_MESSAGES_UPDATE')}">
							<div><a class="dh-underlined-link" href="http://www.facebook.com"><dht3:plural n="${event.count}" s="unread message"/></a></div>
						</c:when>						
					</c:choose>
				</c:forEach>
			</div>
			</td>
			</tr>
			</table>
		<dht3:blockHeaderDescription blockId="${blockId}">
		</dht3:blockHeaderDescription>			
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
			<dht3:blockTimeAgo block="${block}"/>
		</dht3:blockHeaderRight>
	</dht3:blockHeader>	
	<dht3:blockContent blockId="${blockId}">
	</dht3:blockContent>
</dht3:blockContainer>

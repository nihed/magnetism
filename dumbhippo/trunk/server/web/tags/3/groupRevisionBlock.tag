<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.GroupRevisionBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-orange2' : 'dh-box-orange1'}" blockId="${blockId}" expandable="false">
	<dht3:blockLeft block="${block}">
		<dht3:blockTitle>
		    <c:choose>
		        <c:when test="${block.revisorView.mugshotCharacter && dh:enumIs(block.revision.type, 'GROUP_MEMBERSHIP_POLICY_CHANGED')}">
		             <dht3:entityLink who="${block.groupView}"/> was changed <c:out value="${block.membershipRevisionInfo}"/> 
		        </c:when>
		        <c:otherwise>     
			        <span class="dh-stacker-block-title-generic"><dht3:entityLink who="${block.revisorView}"/></span>
			        <c:choose>
				        <c:when test="${dh:enumIs(block.revision.type, 'GROUP_NAME_CHANGED')}"> changed the group's name to '<c:out value="${block.revision.newName}"/>'</c:when>
				        <c:when test="${dh:enumIs(block.revision.type, 'GROUP_DESCRIPTION_CHANGED')}"> changed the description of <dht3:entityLink who="${block.groupView}"/></c:when>
				        <c:when test="${dh:enumIs(block.revision.type, 'GROUP_FEED_ADDED')}"> added the feed '<c:out value="${block.revision.feed.title}"/>' to <dht3:entityLink who="${block.groupView}"/> </c:when>
				        <c:when test="${dh:enumIs(block.revision.type, 'GROUP_FEED_REMOVED')}"> removed the feed '<c:out value="${block.revision.feed.title}"/>' from <dht3:entityLink who="${block.groupView}"/></c:when>
                        <c:when test="${dh:enumIs(block.revision.type, 'GROUP_MEMBERSHIP_POLICY_CHANGED')}"> changed <dht3:entityLink who="${block.groupView}"/> <c:out value="${block.membershipRevisionInfo}"/></c:when>
			        </c:choose>
			    </c:otherwise>
			</c:choose>    
		</dht3:blockTitle>
		<div class="dh-stacker-block-header-description" style="margin-top: 3px;">
		    <c:if test="${block.groupView.status.canChat}">
				<dht:actionLinkChat oneLine="true" chatId="${block.groupView.group.id}" kind="group" linkText="Chat about this"/> 
			</c:if>
			<c:if test="${block.groupView.status.canChat && block.groupView.status.canModify}">
				or 
			</c:if>
			<c:if test="${block.groupView.status.canModify}">
				<dht:actionLink oneLine="true" href="/group-account?group=${block.groupView.group.id}" title="Edit the group">edit the group</dht:actionLink>
			</c:if>
		</div>
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.entitySource}" showFrom="${showFrom}">
        <c:if test="${!oneLine}">  	
		    <div class="dh-stacker-block-sent-to">
			    change to <dht3:entityLink who="${block.groupView}"/>
		    </div>
		</c:if>    
		<div class="dh-stacker-block-time">
			<c:out value="${block.timeAgo}"/>
		</div>
	</dht3:blockRight>
</dht3:blockContainer>

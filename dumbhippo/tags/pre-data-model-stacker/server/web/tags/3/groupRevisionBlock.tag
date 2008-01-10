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
<%@ attribute name="chatHeader" required="true" type="java.lang.Boolean" %>

<c:set var="cssClass" value="dh-box-${chatHeader ? 'grey' : 'orange'}${offset ? 2 : 1}"/>

<dht3:blockContainer cssClass="${cssClass}" blockId="${blockId}" title="Change to ${block.groupView.name}" expandable="${!oneLine && !chatHeader && (!empty block.chatId || block.messageCount > 0)}">
	<dht3:blockLeft block="${block}" chatHeader="${chatHeader}" oneLine="${oneLine}">
		<dht3:blockTitle>
		    <c:choose>
		        <c:when test="${block.revisorView.specialCharacter && dh:enumIs(block.revision.type, 'GROUP_MEMBERSHIP_POLICY_CHANGED')}">
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
			<c:if test="${block.groupView.status.canModify}">
				<dht:actionLink oneLine="true" href="/group-account?group=${block.groupView.group.id}" title="Edit the group">edit the group</dht:actionLink>
			</c:if>
		</div>
        <c:if test="${!oneLine && !chatHeader}">
			<dht3:stackReason block="${block}" blockId="${blockId}"/>
			<dht3:blockContent blockId="${blockId}">
				<dht3:quipper block="${block}" blockId="${blockId}"/>
				<dht3:chatPreview block="${block}" blockId="${blockId}"/>
			</dht3:blockContent>		    
		</c:if>
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.entitySource}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}">
        <c:if test="${!oneLine}">  	
		    <div class="dh-stacker-block-sent-to">
			    change to <dht3:entityLink who="${block.groupView}"/>
		    </div>
		</c:if>    
		<c:choose>
			<c:when test="${chatHeader}">
				<dht3:blockSentTimeAgo chatHeader="true">${block.sentTimeAgo}</dht3:blockSentTimeAgo>
			</c:when>
			<c:otherwise>
				<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
			</c:otherwise>
		</c:choose>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>

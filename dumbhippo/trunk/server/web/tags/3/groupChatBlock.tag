<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.GroupChatBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-orange2' : 'dh-box-orange1'}"  block="${block}" blockId="${blockId}" expandable="true">
	<dht3:blockLeft>
		Group Chat: <dht:actionLinkChat linkText="New chat activity" oneLine="true" chatId="${block.groupView.group.id}" kind="${chatKind}"/>
		<dht3:blockContent blockId="${blockId}">
			<dht3:chatPreview block="${block}" chatId="${block.groupView.group.id}" chatKind="group" chattingCount="${block.groupView.chattingUserCount}"/>
		</dht3:blockContent>			
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.groupView}" showFrom="${showFrom}">
		<dht3:blockTimeAgo block="${block}"/>		
	</dht3:blockRight>
</dht3:blockContainer>


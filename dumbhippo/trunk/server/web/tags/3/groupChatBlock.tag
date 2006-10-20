<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.GroupChatBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-orange2' : 'dh-box-orange1'}" blockId="${blockId}" expandable="true">
	<dht3:blockHeader icon="/images3/${buildStamp}/mugshot_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			Group Chat: <dht:actionLinkChat linkText="New chat activity" oneLine="true" chatId="${block.groupView.group.id}" kind="${chatKind}"/>
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}" from="${block.groupView}" showFrom="${showFrom}">
			<dht3:blockTimeAgo block="${block}"/>		
		</dht3:blockHeaderRight>
	</dht3:blockHeader>	
	<dht3:blockContent blockId="${blockId}">
		<dht3:chatPreview block="${block}" chatId="${block.groupView.group.id}" chatKind="group" chattingCount="${block.groupView.chattingUserCount}"/>
	</dht3:blockContent>
</dht3:blockContainer>


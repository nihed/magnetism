<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.MusicPersonBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" block="${block}" blockId="${blockId}">
	<dht3:blockLeft>
	    <c:if test="${!oneLine}"> 
		    <span class="dh-stacker-block-title-type">Music Radar:</span>	
		</c:if>    	   
		<span class="dh-stacker-block-title-music-person">
			<c:forEach items="${block.trackViews}" end="2" var="track" varStatus="trackIdx">
				<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
			</c:forEach>
		</span>
		<dht3:blockDescription blockId="${blockId}">
		</dht3:blockDescription>			
		<dht3:blockContent blockId="${blockId}">
			<dht3:chatPreview block="${block}" chatId="${block.groupView.group.id}" chatKind="group" chattingCount="${block.groupView.chattingUserCount}"/>
		</dht3:blockContent>		
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
		<dht3:blockTimeAgo block="${block}"/>
	</dht3:blockRight>
</dht3:blockContainer>

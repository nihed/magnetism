<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.MusicPersonBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}">
	<dht3:blockHeader icon="/images3/${buildStamp}/musicradar_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
			<span class="dh-stacker-block-title-type">Music Radar</span>:		
			<span class="dh-stacker-block-title-music-person">
				<c:forEach items="${block.trackViews}" end="2" var="track" varStatus="trackIdx">
					<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
				</c:forEach>
			</span>
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}">
			<dht3:blockTimeAgo block="${block}"/>
		</dht3:blockHeaderRight>
	</dht3:blockHeader>
	<dht3:blockDescription>
	</dht3:blockDescription>	
	<dht3:blockContent blockId="${blockId}">
	<div class="dh-stacker-block-content-music-person">	
		More: 
		<c:forEach items="${block.trackViews}" begin="2" end="5" var="track" varStatus="trackIdx">
			<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
		</c:forEach>
	</div>
	</dht3:blockContent>
</dht3:blockContainer>

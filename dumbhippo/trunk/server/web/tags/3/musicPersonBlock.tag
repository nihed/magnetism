<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.blocks.MusicPersonBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}">
	<dht3:blockHeader block="${block}" blockId="${blockId}">
		<dht3:blockHeaderLeft>
		    <c:if test="${!oneLine}"> 
			    <span class="dh-stacker-block-title-type">Music Radar:</span>	
			</c:if>    	   
			<span class="dh-stacker-block-title-music-person">
				<c:forEach items="${block.trackViews}" end="2" var="track" varStatus="trackIdx">
					<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
				</c:forEach>
			</span>
		<dht3:blockHeaderDescription blockId="${blockId}">
		</dht3:blockHeaderDescription>			
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
			<dht3:blockTimeAgo block="${block}"/>
		</dht3:blockHeaderRight>
	</dht3:blockHeader>	
	<dht3:blockContent blockId="${blockId}">
	<div class="dh-stacker-block-content-music-person">	
		More: 
		<c:forEach items="${block.trackViews}" begin="2" end="5" var="track" varStatus="trackIdx">
			<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
		</c:forEach>
	</div>
	</dht3:blockContent>
</dht3:blockContainer>

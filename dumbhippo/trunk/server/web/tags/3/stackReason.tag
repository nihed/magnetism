<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<c:if test="${dh:enumIs(block.stackReason, 'VIEWER_COUNT')}">
	<div class="dh-stacker-stack-reason" id="dhStackerStackReason-${blockId}">
		<c:choose>
			<c:when test="${block.significantClickedCount == 1}">
				1 person has now viewed this post.
			</c:when>
			<c:otherwise>
				<c:out value="${block.significantClickedCount}"/> people have now viewed this post.
			</c:otherwise>
		</c:choose>
	</div>
</c:if>   	

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="who" required="false" type="java.util.List" %>
<%@ attribute name="singleWho" required="false" type="com.dumbhippo.server.views.EntityView" %>

<div class="dh-stacker-block-sent-to" id="dhStackerBlockRightSentTo-${blockId}">
	<c:if test="${fn:length(who) > 0 || !empty singleWho}">
		Sent to: 
		<c:choose>
			<c:when test="${!empty singleWho}">
				<dht3:entityLink who="${singleWho}"/>
			</c:when>
			<c:otherwise>
				<c:forEach end="9" items="${who}" var="ent" varStatus="entIdx">
					<dht3:entityLink who="${ent}"/><c:if test="${!entIdx.last}">, </c:if>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</c:if>
</div>

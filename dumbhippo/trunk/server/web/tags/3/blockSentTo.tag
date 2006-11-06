<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="who" required="true" type="java.util.List" %>

<div class="dh-stacker-block-sent-to" id="dhStackerBlockRightSentTo-${blockId}">
	<c:if test="${fn:length(who) > 0}">
		Sent to: 
		<c:forEach end="9" items="${who}" var="ent" varStatus="entIdx">
			<dht3:entityLink who="${ent}"/><c:if test="${!entIdx.last}">, </c:if>
		</c:forEach>
	</c:if>
</div>
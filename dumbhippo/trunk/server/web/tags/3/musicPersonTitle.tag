<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.MusicPersonBlockView" %>

<span class="dh-stacker-block-title-music-person">
	<c:forEach items="${block.trackViews}" end="2" var="track" varStatus="trackIdx">
		<dht3:track track="${track}"/><c:if test="${!trackIdx.last}">, </c:if>
	</c:forEach>
</span>

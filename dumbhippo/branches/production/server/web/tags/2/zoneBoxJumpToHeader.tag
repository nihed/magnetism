<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="skip" required="false" type="java.lang.String" %>

<%-- if adding more items, you need a | after all but the last two; the next to last 
	needs a pipe if the last one won't be added; the last never needs a pipe --%>

Jump to:
<c:if test="${skip != 'web'}">
	<a href="/links">Web Swarm</a> |
</c:if>
<c:if test="${skip != 'music'}">
	<a href="/music">Music Radar</a> <c:if test="${skip != 'tv'}">|</c:if>
</c:if>
<c:if test="${skip != 'tv'}">
	<a href="/tv">TV Party</a>
</c:if>

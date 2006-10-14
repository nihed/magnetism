<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.GroupMemberBlockView" %>

<span class="dh-stacker-block-title-group-member-name"><dht3:personLink who="${block.memberView}"/></span> is a new 
<c:choose>
	<c:when test="${dh:enumIs(block.status, 'FOLLOWER')}">follower.</c:when>
	<c:otherwise>member.</c:otherwise>
</c:choose>


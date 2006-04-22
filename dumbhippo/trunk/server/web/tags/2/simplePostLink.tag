<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.PostView" %>

<div class="dh-shared-link">
	<a href="${post.url}" onClick="return dh.util.openFrameSet(window,event,this,'${post.post.id}');"
		title="${post.url}">
		<c:out value="${post.titleAsHtml}" escapeXml="false"/>
	</a>
</div>

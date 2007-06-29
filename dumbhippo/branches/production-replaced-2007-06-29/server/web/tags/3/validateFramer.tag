<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="page" required="true" type="java.lang.String" %>
<%@ attribute name="framer" required="true" type="com.dumbhippo.web.pages.FramerPage" %>

<c:if test="${empty framer.post}">
	<c:choose>
		<c:when test="${!signin.valid && !empty framer.postId}">
			<dh:login next="${page}?post=${framer.postId}"/>
		</c:when>
		<c:otherwise>
			<dht:errorPage>${framer.errorText}</dht:errorPage>
		</c:otherwise>
	</c:choose>
</c:if>

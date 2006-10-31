<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="src" required="false" type="java.lang.String"%>

<c:choose>
	<c:when test="${empty src}">
		<img src="/images/${buildStamp}/samplead.png" width="150" height="69"/>
	</c:when>
	<c:otherwise>
		<iframe src="${src}" width="150" height="69" style="border: 1px solid #bababa;" frameborder="0" scrolling="no"></iframe>
	</c:otherwise>
</c:choose>

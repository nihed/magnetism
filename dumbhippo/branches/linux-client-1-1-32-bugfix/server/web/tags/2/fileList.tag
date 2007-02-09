<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="files" required="true" type="java.util.List" %>

<c:forEach items="${files}" var="file">
	<div>
		<a href="/files/user/${file.relativeUriAsHtml}"><c:out value="${file.name}"/></a>
	</div>
	<div>
		<span style="width: 10px;"></span> <input type="button" value="Delete"
			onclick="dh.files.deleteFile('${file.id}');"/>
	</div>
</c:forEach>

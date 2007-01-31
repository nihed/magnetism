<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%-- We keep /music around because old music radar embeds link to it. --%>
<c:redirect url="/person">
	<c:param name="who" value="${param.who}"/>
</c:redirect>


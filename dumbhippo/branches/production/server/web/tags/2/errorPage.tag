<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:doBody scope="request" var="errorText"/>
<c:redirect url="/error">
	<c:param name="text" value="${errorText}"/>
</c:redirect>


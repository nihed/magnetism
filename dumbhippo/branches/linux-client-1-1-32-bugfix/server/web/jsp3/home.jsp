<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:redirect url="/person">
	<c:param name="who" value="${signin.userId}"/>
</c:redirect>

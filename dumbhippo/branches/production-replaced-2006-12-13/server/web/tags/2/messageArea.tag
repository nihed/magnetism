<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="dh-message" id="dhMessageDiv" style='display: ${empty param["message"] ? "none" : "block"};'>
    <c:out value='${param["message"]}'/>
</div>
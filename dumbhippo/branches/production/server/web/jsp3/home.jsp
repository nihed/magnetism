<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%-- this redirects to /person, adds a who parameter if necessary, and preserves existing parameters --%>
<dht3:requireWhoParameter page="/person"/>
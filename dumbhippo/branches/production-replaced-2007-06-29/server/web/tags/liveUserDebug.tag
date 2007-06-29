<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="user" required="true" type="com.dumbhippo.server.views.PersonView"%>
<a href="/person?who=${user.liveUser.guid}">${user.liveUser.guid}</a> (<c:out value="${user.name}"/>)  
<br/>
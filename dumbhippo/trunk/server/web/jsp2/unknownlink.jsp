<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%-- Right now, GNOME Online is also using this page. We should copy it over to jsp-gnome --%>
<%-- and update the code in RewriteServlet, if this page will include any Mugshot-specific --%>
<%-- links in the future.  --%>
<jsp:forward page="/error">
	<jsp:param name="text" value="That page doesn't exist."/>
</jsp:forward>
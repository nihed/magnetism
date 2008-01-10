<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<jsp:forward page="/error">
	<jsp:param name="text" value="More people are trying to use Mugshot than we can handle right now; try back later!"/>
</jsp:forward>
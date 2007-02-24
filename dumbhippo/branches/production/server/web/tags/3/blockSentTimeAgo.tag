<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>

<div class="dh-stacker-block-time">
	${chatHeader ? 'sent' : 'first sent'} <jsp:doBody/>
</div>

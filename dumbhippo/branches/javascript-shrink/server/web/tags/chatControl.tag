<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="userId" required="true" type="java.lang.String"%>
<%@ attribute name="chatId" required="true" type="java.lang.String"%>

<object classid="clsid:2D40665F-8139-4cb5-BA39-A6E25A147F5D" id="dhChatControl">
	<param name="UserID" value="${userId}"/>
	<param name="ChatID" value="${chatId}"/>
</object>

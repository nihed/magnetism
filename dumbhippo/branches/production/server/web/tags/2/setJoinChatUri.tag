<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="chatId" required="true" type="java.lang.String" %>

<%-- The test here is whether there's any hope of joining the chat working;
     e.g., we don't want to show a link at all on OS X currently, but we want
     to show one on IE even if the user doesn't have the client installed
     to encourage them to install the client --%>
<c:if test="${signin.valid && ((browser.windows && (browser.firefox || browser.ie)) || browser.linux)}">
	<c:set scope="request" var="joinChatUri" value="javascript:dh.actions.joinChat('${chatId}')"/>
</c:if>

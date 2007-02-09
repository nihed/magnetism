<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="ownSection" required="false" type="java.lang.Boolean" %>

<c:set var="disclaimerClass" value="dh-disclaimer"/>
<c:if test="${ownSection}">
    <c:set var="disclaimerClass" value="dh-disclaimer-own-section"/>
</c:if>

<div class="${disclaimerClass}">We promise not to sell or share your info, spam or scam you,
	install spyware, or other annoying stuff. <a href="/privacy">See our Privacy Policy.</a></div>
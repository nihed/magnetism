<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="number" required="true" type="java.lang.Integer" %>

<c:if test="${(number % 4) == 0}">
	<c:set var="rightSideClass" value="dh-image-right-side" scope="page"/>
</c:if>

<c:if test="${number > 12}">
	<c:set var="bottomSideClass" value="dh-image-bottom-side" scope="page"/>
</c:if>

<div class="dh-image ${rightSideClass} ${bottomSideClass}">
	<a  style="visibility: hidden;"><img id="dhPhotoChooserImage${number}"/></a>
</div>

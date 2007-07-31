<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<div id="gnomeHeader">
	<c:choose>
		<c:when test="${dogfoodMode}">
			<a class="gnome-header" href="">GNOME Online <span style="color: red;">DOGFOOD</span></a>
		</c:when>
		<c:otherwise>
			<a class="gnome-header" href="">GNOME Online</a>
		</c:otherwise>
	</c:choose>
</div>
<gnome:fixedLogo/>

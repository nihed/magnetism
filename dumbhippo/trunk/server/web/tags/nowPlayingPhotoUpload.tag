<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="mode" required="true" type="java.lang.String"%>
<%@ attribute name="themeId" required="true" type="java.lang.String"%>
<%@ attribute name="linkText" required="true" type="java.lang.String"%>
<%@ attribute name="reloadTo" required="true" type="java.lang.String" %>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhNowPlayingPhotoCount}">
	<c:set var="dhNowPlayingPhotoCount" value="0" scope="request"/>
</c:if>
<c:set var="dhNowPlayingPhotoCount" value="${dhNowPlayingPhotoCount + 1}" scope="request"/>
<c:set var="N" value="${dhNowPlayingPhotoCount}" scope="page"/>

<c:url value="/upload/nowplaying-themes" var="posturl"/>
<form id="dhNowPlayingPhotoForm${N}" enctype="multipart/form-data" action="${posturl}" method="post"
	 style="font-size: smaller; text-align: right;">
	<img id="dhNowPlayingPhotoProgress${N}" class="dhInvisible" width="192" height="192"/>
	<a id="dhNowPlayingChangePhotoLink${N}"
		href="javascript:void(0);" 
		onClick="dh.nowplaying.showChangePhoto('${N}');return true">${linkText}</a>
	<input class="dh-upload-photo dhInvisible" 
		onChange="dh.nowplaying.doChangePhoto('${N}')"
		id="dhNowPlayingPhotoFileEntry${N}"
		type="file" name="photo"/>

	<input type="hidden" name="theme" value="${themeId}"/>
	<input type="hidden" name="mode" value="${mode}"/>
	<input type="hidden" name="reloadTo" value="${reloadTo}"/>
</form>

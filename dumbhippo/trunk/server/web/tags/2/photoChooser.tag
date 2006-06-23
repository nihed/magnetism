<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dht:popup id="dhPhotoChooser" extraClass="dh-photo-chooser">
	<div class="dh-title">SELECT A PROFILE PICTURE</div>
	<div class="dh-image-grid">
		<c:forEach begin="1" end="16" var="number">
			<dht:photoChooserCell number="${number}"/>
		</c:forEach>
	</div>
	<div id="dhPhotoChooserControls" class="dh-controls">
		<div id="dhPhotoChooserBack" class="dh-left">
			<a href="javascript:void(0);" title="Back to last page of photos">BACK</a>
		</div>
		<div id="dhPhotoChooserMore" class="dh-right">
			<a href="javascript:void(0);" title="See more photos on next page">MORE</a>
		</div>
		<div class="dh-grow-div-around-floats"><div></div></div>
	</div>
</dht:popup>

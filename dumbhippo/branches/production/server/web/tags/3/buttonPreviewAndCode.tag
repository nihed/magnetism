<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="image" required="true" type="java.lang.String" %>
<%@ attribute name="imageWidth" required="true" type="java.lang.String" %>
<%@ attribute name="imageHeight" required="true" type="java.lang.String" %>
<%@ attribute name="description" required="true" type="java.lang.String" %>

<div class="dh-buttons-choice-background">
	<div class="dh-buttons-choice-preview">
		<div class="dh-buttons-choice-preview-image">
			<%-- no dh:png since the buttons are all gifs, if someone embeds a button they don't want to go through IE5 png hoops --%>
			<img src="/images/buttons/${image}" style="width: ${imageWidth}px; height: ${imageHeight}px;"/>
		</div>
		<div class="dh-buttons-choice-preview-title">
			<c:out value="${description}"/>
		</div>
	</div>
	<div class="dh-buttons-choice-code-area">
		<textarea class="dh-buttons-code" readonly="readonly" rows="3" wrap="off">&lt;a href=&quot;${buttonLink}&quot; title=&quot;Mugshot&quot;&gt;
&lt;img src=&quot;${baseUrl}/images/buttons/${image}&quot; alt=&quot;Mugshot&quot;&gt;
&lt;/a&gt;</textarea>
	</div>
	<div class="dh-grow-div-around-floats"></div>
</div>

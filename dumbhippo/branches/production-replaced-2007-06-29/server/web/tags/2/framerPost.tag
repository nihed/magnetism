<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<dh:script module="dh.util"/>

<%@ attribute name="post" required="true" type="com.dumbhippo.server.views.PostView"%>

<div class="dh-framer-share">
	<div class="dh-framer-from-container">
		<div class="dh-framer-from-area">
			<div class="dh-framer-headshot">
				<dht:headshot person="${post.poster}" size="60"/>
			</div>
			<div>
				<a class="dh-framer-from" href="${post.poster.homeUrl}" target="_top"><c:out value="${post.poster.name}"/></a>
			</div>
		</div>
	</div>
	<div class="dh-framer-content-container">
		<div class="dh-framer-content">
			<div class="dh-framer-title">
				<jsp:element name="a">
					<jsp:attribute name="href"><c:out value="${post.url}"/></jsp:attribute>
					<jsp:attribute name="target">_top</jsp:attribute>
					<jsp:attribute name="onMouseDown">dh.util.useFrameSet(window,event,this,'${post.post.id}');</jsp:attribute>
					<jsp:body><c:out value="${post.titleAsHtml}" escapeXml="false"/></jsp:body>
				</jsp:element>
			</div>
			<div id="dhFramerDescription" class="dh-framer-description">
				<c:out value="${post.textAsHtmlShort}" escapeXml="false"/>			
			</div>	
			<div class="dh-framer-sent-to">
				<c:choose>
					<c:when test="${post.toWorld}">
						<c:set var="recipientsPrefix" value="The World" scope="page"/>
					</c:when>
					<c:otherwise>
						<c:set var="recipientsPrefix" value="" scope="page"/>					
					</c:otherwise>
				</c:choose>
				Sent to <dh:entityList prefixValue="${recipientsPrefix}" value="${post.recipients}" separator=", "/>
			</div>				
		</div>	
	</div>
</div>

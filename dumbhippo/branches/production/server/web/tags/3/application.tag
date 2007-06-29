<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="application" required="true" type="com.dumbhippo.server.applications.ApplicationView" %>
<%@ attribute name="linkify" required="false" type="java.lang.Boolean" %>
<%@ attribute name="includeStats" required="false" type="java.lang.Boolean" %>

<dh:default var="linkify" value="true"/>
<dh:default var="includeStats" value="true"/>

<div class="dh-applications-application dh-applications-application-big">
	<c:if test="${includeStats}">
		<div class="dh-applications-application-stats-outer">
			<dht3:applicationStats application="${application}"/>
		</div>
	</c:if>
	<div class="dh-applications-application-icon">
		<dh:png src="${application.icon.url}" 
				style="width: ${application.icon.displayWidth}; height: ${application.icon.displayHeight}; overflow: hidden;"/>
	</div>
	<div class="dh-applications-application-details">
		<div class="dh-applications-application-name">
			<c:choose>
				<c:when test="${linkify}">
					<a href="/application?id=${application.application.id}">
						<c:out value="${application.application.name}"/>
					</a>
				</c:when>
				<c:otherwise>
					<c:out value="${application.application.name}"/>
				</c:otherwise>
			</c:choose>
		</div>
		<div class="dh-applications-application-generic-name">
			<c:out value="${application.application.genericName}"/>
		</div>
		<div class="dh-applications-application-category">
			<a href="/applications?category=${application.application.category.name}">
				<c:out value="${application.application.category.displayName}"/>
			</a>
		</div>
	</div>
</div>

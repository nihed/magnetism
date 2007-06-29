<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="apps" required="true" type="com.dumbhippo.server.Pageable" %>

<div class="dh-mini-applications">
    <c:forEach items="${apps.results}" var="app">
   		<div class="dh-applications-application-separator"></div>
    	<div class="dh-applications-application">
    		<div class="dh-applications-application-stats-outer">
	    		<div class="dh-applications-application-stats">
	    			<div class="dh-applications-rank"><c:out value="${app.rank}"/></div>
	    		</div>
    		</div>
    		<div class="dh-applications-application-icon">
				<dh:png src="${app.icon.url}" 
					style="width: ${app.icon.displayWidth}; height: ${app.icon.displayHeight}; overflow: hidden;"/>
    		</div>
    		<div class="dh-applications-application-details">
    			<div class="dh-applications-application-name">
	    			<a href="/application?id=${app.application.id}">
	    				<c:out value="${app.application.name}"/>
    				</a>
	    		</div>
    			<div class="dh-applications-application-category">
	    			<a href="/applications?category=${app.application.category.name}">
   						<c:out value="${app.application.category.displayName}"/>
   					</a>
	    		</div>
    		</div>
    		<div class="dh-applications-application-separator"></div>
  			</div>
    </c:forEach>
    <div class="dh-applications-more">
	    <dht:expandablePager pageable="${apps}"/>
    </div>
</div>
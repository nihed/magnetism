<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView" %>
<%@ attribute name="stackOrder" required="true" type="java.lang.String" %>
<%@ attribute name="stackType" required="false" type="java.lang.String" %>
<%@ attribute name="pageable" required="false" type="com.dumbhippo.server.Pageable" %>
<%@ attribute name="blocks" required="false" type="java.util.List" %>
<%@ attribute name="showFrom" required="true" type="java.lang.Boolean" %>
<%@ attribute name="shortVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="embedVersion" required="false" type="java.lang.Boolean" %>
<%@ attribute name="width" required="false" type="java.lang.String" %>
<%@ attribute name="floatSide" required="false" type="java.lang.String" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showHomeUrl" required="false" type="java.lang.Boolean" %>

<c:if test="${empty shortVersion}">
	<c:set var="shortVersion" value="false"/>
</c:if> 

<c:if test="${empty embedVersion}">
	<c:set var="embedVersion" value="false"/>
</c:if> 

<c:if test="${empty showHomeUrl}">
	<c:set var="showHomeUrl" value="true"/>
</c:if> 

<dht3:shinyBox color="grey" width="${width}" floatSide="${floatSide}">				
	<dht3:personHeader who="${person}" disableLink="${disableLink || embedVersion}" embedVersion="${embedVersion}" shortVersion="${shortVersion}">
 	    <dht3:personActionLinks who="${person}" showHomeUrl="${showHomeUrl}"/> 	    
	</dht3:personHeader>
	<c:choose>
		<c:when test="${pageable.totalCount == 0 && person.viewOfSelf && !signin.user.account.hasAcceptedTerms}">
			<div class="dh-empty-stacker-text">
				Once your Mugshot account is active, updates from sites you belong to
				will show up here. Show updates from your Myspace, Flickr, Facebook and
				other pages all in one place. <span class="dh-empty-stacker-secondary">(This page
				is not visible to anybody but you until you accept the Mugshot Terms of Use.)</span>
			</div>
	    </c:when>
		<c:when test="${pageable.totalCount == 0 && person.viewOfSelf}">
			<div class="dh-empty-stacker-text">
				<a href="/account">List your accounts</a> to have updates from sites
				you belong to show up here. Show updates from your Myspace, Flickr, Facebook and
				other pages all in one place.
			</div>
	    </c:when>
	    <c:otherwise>
			<c:if test="${!shortVersion}">
			    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}" homeStack="${homeStack}"/>
		    </c:if>
	    </c:otherwise>
    </c:choose>
</dht3:shinyBox>

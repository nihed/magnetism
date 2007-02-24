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
	<dht3:personHeader>
		<dht3:personHeaderLeft who="${person}" disableLink="${disableLink || embedVersion}" embedVersion="${embedVersion}" shortVersion="${shortVersion}">
	 	    <dht3:personActionLinks who="${person}" showHomeUrl="${showHomeUrl}"/> 	    
	 	</dht3:personHeaderLeft>
	 	<dht3:personHeaderRight who="${person}" align="${pageable.totalCount == 0 && person.viewOfSelf ? 'middle' : 'belowAccounts'}" includeWhereAt="${!embedVersion}">
		    <c:if test="${!embedVersion}">
		  		<c:choose>
					<c:when test="${pageable.totalCount == 0 && person.viewOfSelf && !signin.user.account.hasAcceptedTerms}">
						<dht3:tip>
							Here is where you will see updates from your MySpace, Flickr, Facebook and
							<a href="/features">other sites</a> you belong to when you activate your 
							Mugshot account.
							<div class="dh-tip-secondary">(This page is not visible to anybody but you until you 
						      accept the Mugshot Terms of Use above.)</div>
						</dht3:tip>
					</c:when>
					<c:when test="${pageable.totalCount == 0 && person.viewOfSelf}">
						<dht3:tip>
							<a href="/account">Add your Web accounts</a> to show updates from your
							MySpace, Flickr, Facebook, and <a href="/features">other sites</a> you belong to.
						</dht3:tip>
				    </c:when>				  		
				    <c:otherwise>
	                	<%-- Accounts with thumbnail boxes --%>
		            	<c:forEach var="account" items="${person.lovedAccounts.list}">
	    	       		<c:if test="${account.hasThumbnails}">
            	       		<dht:whereAtThumbnailBox account="${account}" />
		        	   	</c:if>
           				</c:forEach>
           			</c:otherwise>
           		</c:choose>
		      	<c:if test="${shortVersion}">
          	    	<div class="dh-back">
                   		<a href="/person?who=${person.viewPersonPageId}">Back to <c:out value="${person.name}"/>'s Home</a>
					</div>
		        </c:if>    
		    </c:if>
	 	</dht3:personHeaderRight>
	</dht3:personHeader>
	<c:if test="${!shortVersion}">
	    <dht3:stacker stackOrder="${stackOrder}" stackType="${stackType}" pageable="${pageable}" blocks="${blocks}" showFrom="${showFrom}" oneLine="${embedVersion}" homeStack="${homeStack}"/>
    </c:if>
</dht3:shinyBox>

<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requireStackedPersonBean/>

<%-- This is a Facebook authetication token, when people re-login we land them on their person page and get the token --%>
<jsp:setProperty name="person" property="facebookAuthToken" param="auth_token"/>

<c:set var="pageName" value="Home" scope="page"/>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="person"/>
	<dht:faviconIncludes/>
</head>


<dht3:page currentPageLink="person">
	<dht3:pageSubHeader title="${person.viewedPerson.name}'s ${pageName}">
		<dht3:randomTip tipIndex="${person.randomTipIndex}" isSelf="${person.self}"/>
		<dht3:standardStackedPersonPageOptions selected="${pageName}"/> 
	</dht3:pageSubHeader>
	<%-- this will go away soon, so it's not worth it creating a tag for it --%>
	<c:if test="${person.facebookErrorMessage != null}">
        <div id="dhFacebookNote">
            <c:out value="${person.facebookErrorMessage}"/>
            <a href="http://facebook.com">Log out from Facebook first</a> to re-login here.
       </div>                     
    </c:if> 
	<dht3:personStack person="${person.viewedPerson}" stackOrder="1" pageable="${person.pageableMugshot}" shortVersion="${person.pageableStack.position > 0}" showFrom="true" homeStack="${person.self}"/>
	
	<dht3:shinyBox color="grey">
	    <div class="dh-person-stacker-header">
		    <span class="dh-person-header-name"><c:out value="${person.viewedPerson.name}"/>'s Stacker</span>
			    <c:choose>
				    <c:when test="${person.viewedPerson.viewOfSelf}">
					    <span class="dh-person-header-description">What I'm watching on the web</span>							
				    </c:when>
				    <c:otherwise>
					    <span class="dh-person-header-description">What <c:out value="${person.viewedPerson.name}"/> is watching on the web</span>							
				    </c:otherwise>
			    </c:choose>
		</div>    
		<dht3:stacker stackOrder="2" stackType="dhStacker" pageable="${person.pageableStack}" showFrom="true" homeStack="${person.self}"/>
		<c:if test="${person.pageableMugshot.position != 0}">
		    <div class="dh-back">
		        <a href="/person?who=${person.viewedPerson.viewPersonPageId}">Back to <c:out value="${person.viewedPerson.name}"/>'s Home</a>
		    </div>
		</c:if>    		    
	</dht3:shinyBox>	
</dht3:page>

</html>

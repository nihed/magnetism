<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="kind" required="false" type="java.lang.String" %>
<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableSignupLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<c:choose>
<c:when test="${webVersion == 3}">
	<dht3:header disableHomeLink="${disableHomeLink}" disableSignupLink="${disableSignupLink}" searchText="${searchText}"/>
</c:when>
<c:otherwise>

<c:if test="${empty kind}">
	<c:choose>
		<c:when test="${showSidebar}">
			<c:set var="kind" scope="page" value="withSidebar"/>
		</c:when>
		<c:otherwise>
			<c:set var="kind" scope="page" value="withoutSidebar"/>
		</c:otherwise>
	</c:choose>
</c:if>

<c:choose>
	<c:when test="${kind == 'main'}">
		<c:set var="headerMap" value="header710x125" scope="page" />
		<c:set var="headerImage" value="/images2/${buildStamp}/mughdr710x125.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-tall" scope="page"/>
	</c:when>
	<c:when test="${kind == 'withSidebar'}">
		<c:set var="headerMap" value="header710x80" scope="page" />
		<c:set var="headerImage" value="/images2/${buildStamp}/mughdr710x80.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-regular" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="headerMap" value="header500x65" scope="page" />
		<c:set var="headerImage" value="/images2/${buildStamp}/mughdr500x65.gif" scope="page"/>
		<c:set var="headerHeightClass" value="dh-header-short" scope="page"/>
	</c:otherwise>
</c:choose>

<map name="header710x125">
<area shape="rect" coords="0,0,400,125" href="/" />
</map>

<map name="header710x80">
<area shape="rect" coords="0,0,258,80" href="/" />
</map>

<map name="header500x65">
<area shape="rect" coords="0,0,258,65" href="/" />
</map>

<div id="dhPageHeader" class="${headerHeightClass}">
	<img id="dhPageHeaderImage" src="${headerImage}" usemap="#${headerMap}" />
	<div id="dhHeaderControls">
		<div id="dhHeaderOptions">
			<c:choose>
				<c:when test="${signin.valid}">
					<c:if test="${!disableHomeLink}">
					    <a href="/">HOME</a> | 
					 </c:if>
					<dht:actionLinkLogout/>
				</c:when>
				<c:otherwise>
				    <c:if test="${!disableSignupLink}">				
				        <a href="/signup">Sign up</a> | 
				    </c:if>    
				    <a href="/who-are-you">Log in</a>
				</c:otherwise>
			</c:choose>
		</div>
		<div id="dhSearchBox">
			<form action="/search" method="get">
				Search: 
				<jsp:element name="input">
					<jsp:attribute name="type">text</jsp:attribute>
					<jsp:attribute name="id">dhGlobalSearchEntry</jsp:attribute>
					<jsp:attribute name="class">dh-text-input</jsp:attribute>
					<jsp:attribute name="name">q</jsp:attribute>
					<jsp:attribute name="value">${searchText}</jsp:attribute>
				</jsp:element>
				<input type="submit"  value="Go"/>
			</form>
			<script type="text/javascript">
				dhGlobalSearchEntryInit = function () {
					var searchBox = document.getElementById('dhGlobalSearchEntry');
					var entry = new dh.textinput.Entry(searchBox, "topics, music, people", "");
				}
				dhGlobalSearchEntryInit()
			</script>			
		</div>		
	</div>
    <c:if test="${kind == 'main' || kind == 'withSidebar'}">
        <div id="dhHeaderLinks">
            <dht:requireLinksGlobalBean/>
            <a href="http://blog.mugshot.org/?page_id=213" title="Learn about various Mugshot features" target="_blank">Mugshot Features</a>
            <c:if test="${linksGlobal.newFeatures}">
                &nbsp;<img src="/images2/${buildStamp}/newpink.gif"/>
            </c:if>
            &nbsp;&nbsp;|&nbsp;&nbsp;
		    <a href="http://blog.mugshot.org" title="Read our blog" target="_blank">Mugshot Blog</a>
		</div>
    </c:if>    		
</div>
</c:otherwise>
</c:choose> <!-- webVersion test -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<div id="dhPageHeader">
	<table cellspacing="0" cellpadding="0">
	<tr>
	<td><img id="dhPageHeaderLeft" src="/images3/${buildStamp}/header_left.png" /></td>
	<td id="dhPageHeaderInner">
	<div id="dhPageHeaderInnerBox">
	<div id="dhSearchBox">
		<form action="/search" method="get" name="dhSearchForm">
			Search: 
			<jsp:element name="input">
				<jsp:attribute name="type">text</jsp:attribute>
				<jsp:attribute name="id">dhGlobalSearchEntry</jsp:attribute>
				<jsp:attribute name="name">q</jsp:attribute>
				<jsp:attribute name="value">${searchText}</jsp:attribute>
			</jsp:element>
			<img src="/images3/${buildStamp}/find.gif" onclick="document.forms['dhSearchForm'].submit()"/>
		</form>
		<script type="text/javascript">
			dhGlobalSearchEntryInit = function () {
				var searchBox = document.getElementById('dhGlobalSearchEntry');
				var entry = new dh.textinput.Entry(searchBox, "Search for topics, people, music", "");
			}
			dhGlobalSearchEntryInit()
		</script>			
	</div>		
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
	</div>  
	</div>
	</td>
	<td><img id="dhPageHeaderRight" src="/images3/${buildStamp}/header_right.png"/></td>
	</tr>
	</table>
</div>

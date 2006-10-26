<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="disableHomeLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disableSignupLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<div id="dhPageHeader3">
	<table cellspacing="0" cellpadding="0" width="100%">
	<tr valign="top">
	<td width="248px"><a href="/"><dh:png id="dhPageHeaderLeft" src="/images3/${buildStamp}/header_left.png" style="width: 248px; height: 64px"/></a></td>
	<td id="dhPageHeaderLeftControls" align="left" valign="bottom" height="56px">
		<div id="dhPageHeaderLeftControlsArea">
		<c:choose>
			<c:when test="${signin.valid}">
				<span id="dhPageHeaderWelcome">Hello, <c:out value="${signin.viewedUserFromSystem.name}"/>!</span> 
				<div id="dhPageHeaderWelcomeOptions" class="dh-underlined-link"><dht:actionLinkLogout oneLine="true"/></div>
			</c:when>
			<c:otherwise>	
			    <span id="dhPageHeaderWelcomeOptions"><a class="dh-underlined-link" href="/who-are-you">Log in</a><c:if test="${!disableSignupLink}"> | <a class="dh-underlined-link" href="/signup">Sign up</a></c:if></span>
			</c:otherwise>
		</c:choose>
		</div>
	</td>
	<td id="dhPageHeaderInner" align="right" height="56px">
		<table cellspacing="0" cellpadding="0" height="56px">
		<tr valign="top" align="right"><td>
		<div id="dhSearchBox">
			<form action="/search" method="get" name="dhSearchForm" id="dhSearchForm">
				<table cellspacing="0" cellpadding="0">
				<tr valign="top" align="right"><td valign="top">
				<jsp:element name="input">
					<jsp:attribute name="type">text</jsp:attribute>
					<jsp:attribute name="id">dhSearchBoxEntry</jsp:attribute>
					<jsp:attribute name="name">q</jsp:attribute>
					<jsp:attribute name="value">${searchText}</jsp:attribute>
				</jsp:element>
				</td><td valign="top"><img id="dhSearchBoxFind" src="/images3/${buildStamp}/find.gif" onclick="dhSearchSubmit()"/></td>
				</tr>
				</table>
			</form>
			<script type="text/javascript">
				var dhSearchBoxEntry = null;
				dhSearchInit = function () {
					var searchBox = document.getElementById('dhSearchBoxEntry');
					dhSearchBoxEntry = new dh.textinput.Entry(searchBox, "Search for topics, people, music", <dh:jsString value="${searchText}"/>);
				}
				dhSearchSubmit = function() {
					dhSearchBoxEntry.prepareToSubmit();
					document.forms['dhSearchForm'].submit();
				}
				dhSearchInit();
			</script>
		</div>
		</td>
		</tr>
		<tr><td></td></tr>
		<tr valign="bottom" align="right">
		<td>
		<div id="dhHeaderControls3">
			<div id="dhHeaderOptions3">
			    <c:if test="${signin.valid}">
					<a class="dh-underlined-link" href="/">My Home</a> | 
				    <a class="dh-underlined-link" href="/account">My Account</a> |
			    </c:if>
				<a class="dh-underlined-link" href="/active-people">Active People</a> | 
				<a class="dh-underlined-link" href="/active-groups">Active Groups</a> | 
				<a class="dh-underlined-link" href="/features">Features</a> |				
				<a class="dh-underlined-link" href="http://blog.mugshot.org/?page_id=245a">Help</a>
			</div>	
		</div>  
		</td>
		</tr>
		</table>
	</td>
	<td align="right" width="8px"><dh:png id="dhPageHeaderRight" src="/images3/${buildStamp}/header_right.png" style="width: 8px; height: 64px;"/></td>
	</tr>
	</table>
</div>

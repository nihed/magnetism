<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<div id="dhPageHeader">
	<table cellspacing="0" cellpadding="0">
	<tr valign="top">
	<td><img id="dhPageHeaderLeft" src="/images3/${buildStamp}/header_left.png" /></td>
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
				</td><td valign="top"><img class="dhSearchBoxFind" src="/images3/${buildStamp}/find.gif" onclick="document.forms['dhSearchForm'].submit()"/></td>
				</tr>
				</table>
			</form>
			<script type="text/javascript">
				dhGlobalSearchEntryInit = function () {
					var searchBox = document.getElementById('dhGlobalSearchEntry');
					var entry = new dh.textinput.Entry(searchBox, "Search for topics, people, music", "");
				}
				dhGlobalSearchEntryInit()
			</script>			
		</div>
		</td>
		</tr>
		<tr><td></td></tr>
		<tr valign="bottom" align="right">
		<td>
		<div id="dhHeaderControls">
			<div id="dhHeaderOptions">
				<c:if test="${signin.valid && !disableHomeLink}">
				    <a href="/">Home</a> | 
				</c:if>
				<a href="/features">Features</a> | 
				<a href="http://blog.mugshot.org/">Blog</a> | 
				<a href="/friends">People</a> | 
				<a href="/groups">Groups</a> | 
				<a href="http://blog.mugshot.org/?page_id=245a">Help</a>
				<c:if test="${signin.valid}">
					| <dht:actionLinkLogout/>
				</c:if>			
			</div>	
		</div>  
		</td>
		</tr>
		</table>
	</td>
	<td><img id="dhPageHeaderRight" src="/images3/${buildStamp}/header_right.png"/></td>
	</tr>
	</table>
</div>

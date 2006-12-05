<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="currentPageLink" required="true" type="java.lang.String" %>
<%@ attribute name="searchText" required="false" type="java.lang.String" %>

<c:choose>
	<%-- we know that the "person" variable is available on /person though using it here is 
		 a little fishy I suppose --%>
	<c:when test="${currentPageLink == 'person' && signin.valid && person.self}">
		<c:set var="disableHomeLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'main' && !signin.valid}">
		<c:set var="disableHomeLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'signup'}">
		<c:set var="disableSignupLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'badges'}">
		<c:set var="disableMiniLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'active-people'}">
		<c:set var="disableActivePeopleLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'active-groups'}">
		<c:set var="disableActiveGroupsLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'features'}">
		<c:set var="disableFeaturesLink" value="true"/>
	</c:when>
	<c:when test="${currentPageLink == 'account'}">
		<c:set var="disableAccountLink" value="true"/>
	</c:when>	
</c:choose>

<div id="dhPageHeader3">
	<table cellspacing="0" cellpadding="0" width="100%">
	<tr valign="top">
	<td width="248px"><a href="/" id="dhPageHeaderLeftLink"><dh:png id="dhPageHeaderLeft" src="/images3/${buildStamp}/header_left.png" style="width: 248px; height: 64px"/></a></td>
	<td id="dhPageHeaderLeftControls" align="left" valign="bottom">
		<div id="dhPageHeaderLeftControlsArea">
		<c:choose>
			<c:when test="${signin.valid}">
				<span id="dhPageHeaderWelcome">Hello, <c:out value="${signin.viewedUserFromSystem.name}"/>!</span> 
				<div id="dhPageHeaderWelcomeOptions"><dht:actionLinkLogout oneLine="true" underline="true"/></div>
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
			<dh:script module="dh.textinput"/>
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
				<c:choose>
					<c:when test="${signin.valid}">
						<c:if test="${!disableHomeLink}">
						    <a class="dh-underlined-link" href="/">My Home</a> | 
						</c:if>
						<c:if test="${!disableAccountLink}">
					        <a class="dh-underlined-link" href="/account">My Account</a> |
					    </c:if>
					</c:when>
					<c:otherwise>
						<c:if test="${!disableHomeLink}">
							<a class="dh-underlined-link" href="/">Home</a> | 
						</c:if>
					</c:otherwise>
				</c:choose>
				<c:if test="${!disableActivePeopleLink}">
					<a class="dh-underlined-link" href="/active-people">Active People</a> | 
				</c:if>
				<c:if test="${!disableActiveGroupsLink}">
					<a class="dh-underlined-link" href="/active-groups">Active Groups</a> | 
				</c:if>
				<c:if test="${!disableMiniLink}">
					<dh:png src="/images3/${buildStamp}/mini_icon.png" style="width: 28px; height: 11px;"/> <a class="dh-underlined-link" href="/badges">Get Mini</a> |
				</c:if>
				<c:if test="${!disableFeaturesLink}">
					<a class="dh-underlined-link" href="/features">Features</a> |				
				</c:if>
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
<!-- Semi-cracktastic thing to show our javascript debug log -->
<dh:script module="dh.logger"/>
<dh:script module="dh.util"/>
<script type="text/javascript">
	var link = document.getElementById("dhPageHeaderLeftLink");
	link.onclick = function (e) {
		if (!e) e = window.event;
		if (e.ctrlKey && e.altKey) {
			dh.logger.show();
			dh.util.cancelEvent(e);
			return false;
		}
		return true;
	}
	dh.log("header", "Registered header event handler")
</script>

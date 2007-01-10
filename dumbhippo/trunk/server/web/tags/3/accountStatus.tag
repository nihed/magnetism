<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="includeDownload" required="false" type="java.lang.Boolean" %>

<c:if test="${empty includeDownload}">
	<c:set var="includeDownload" value="true"/>
</c:if> 

<dh:script modules="dh.actions,dh.event"/>
<c:if test="${!signin.active || (includeDownload && signin.user.account.needsDownload)}">
	<c:set scope="request" var="accountStatusShowing" value="true"/>
	<div id="dhAccountStatus">
		<c:choose>
			<c:when test="${signin.user.account.adminDisabled}">
				<div id="dhAccountDisabled">
					Your account has been disabled by the Mugshot site adminstrators. If you believe it
					was disabled in error, please contact <a href="mailto:feedback@mugshot.org">feedback@mugshot.org</a> 
					so we can straighten the situation out.
				</div>
			</c:when>
			<c:when test="${!signin.user.account.hasAcceptedTerms}">
				<div>
					<span class="dh-account-status-primary">Welcome to Mugshot! Activate your account to share updates with friends.</span>
					<a href="/features">Learn more about Mugshot.</a>					
				</div>
				<script type="text/javascript">
					dh.event.addPageLoadListener(dh.actions.updateGetStarted);
				</script>
				<table>
				<tr>
				<td valign="center" class="dh-account-status-secondary">
			        <input type="checkbox" id="dhAcceptTerms" onclick="dh.actions.updateGetStarted();">
		                <label for="dhAcceptTerms">I agree to the Mugshot</label> <a href="javascript:window.open('/terms', 'dhTermsOfUse', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a>.
			        </input>
		        </td>
		        <td valign="center">
					<a id="dhGetStarted"><img id="dhGetStartedButton" src="/images3/${buildStamp}/get_started_disabled.gif"/></a>
				</td>
				</tr>
				</table>
			</c:when>
			<c:when test="${signin.user.account.disabled}">
				<div id="dhAccountDisabled">
					You have disabled your account. The information on these page is not
					visible to anybody else. Reenable your account to use all of Mugshot's 
					features. 
					<p>
						<a href="javascript:dh.actions.enableAccount()">Reenable my account</a>
					</p>
				</div>
			</c:when>
			<c:when test="${includeDownload && signin.user.account.needsDownload}">
				<div id="dhDownloadMessageClose">
					<a href="javascript:dh.actions.removeDownloadMessage()"><img src="/images3/${buildStamp}/alert_x_box.gif" width="10" height="10"/></a>
				</div>
				<div id="dhDownloadMessage">
					<dh:png src="/images3/${buildStamp}/star.png" style="width: 13px; height: 13px;"/>
					Make the most of Mugshot by downloading our desktop software. <a href="/download">Download</a>
				</div>
			</c:when>
		</c:choose>
	</div> 
</c:if>
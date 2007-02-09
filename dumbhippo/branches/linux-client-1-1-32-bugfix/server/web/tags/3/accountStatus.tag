<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="includeDownload" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableControl" required="false" type="java.lang.Boolean" %>

<c:if test="${empty includeDownload}">
	<c:set var="includeDownload" value="true"/>
</c:if> 
<c:if test="${empty enableControl}">
	<c:set var="enableControl" value="false"/>
</c:if> 

<dh:script modules="dh.actions,dh.event"/>
<c:choose>
	<c:when test="${signin.user.account.adminDisabled}">
		<div id="dhAccountDisabled">
			<span class="dh-account-disabled-header">Your account is currently disabled.</span>
			Your account has been disabled by the Mugshot site adminstrators. If you believe it
			was disabled in error, please contact <a href="mailto:feedback@mugshot.org">feedback@mugshot.org</a> 
			so we can straighten the situation out.
		</div>
	</c:when>
	<c:when test="${signin.user.account.disabled}">
		<div id="dhAccountDisabled">
			<span class="dh-account-disabled-header">Your account is currently disabled.</span>  
			<c:choose>
				<c:when test="${!enableControl}">
					Go to <a href="/account">My Account</a> to reenable it.
				</c:when>
				<c:otherwise>
					The information on your Mugshot pages is not visible to anybody else. 
					Reenable your account to use all of Mugshot's features. 
                    <p>
                    	<a href="javascript:dh.actions.enableAccount()">Reenable my account</a>
                    </p>
                </c:otherwise>
			</c:choose>
		</div>
	</c:when>	
	<c:when test="${!signin.active || (includeDownload && signin.user.account.needsDownload)}">
	<c:set scope="request" var="accountStatusShowing" value="true"/>
		<div id="dhAccountStatus">
			<c:choose>
			<c:when test="${!signin.user.account.hasAcceptedTerms}">
				<table cellspacing="0" cellpadding="0">
				<tr>
				<td valign="center">			
					<div>
						<div class="dh-account-status-primary">Welcome to Mugshot!</div>
						Activate your account to share updates with friends.
					</div>
					<div>
						<a href="/features">Learn more about Mugshot.</a>					
					</div>
					<script type="text/javascript">
						dh.event.addPageLoadListener(dh.actions.updateGetStarted);
					</script>
				</td>
				<td align="left" valign="center">
					<div class="dh-account-status-secondary">
						<div>
			        	<input type="checkbox" id="dhAcceptTerms" onclick="dh.actions.updateGetStarted();">
		                	<label for="dhAcceptTerms">I agree to the Mugshot</label> <a href="javascript:window.open('/terms', 'dhTermsOfUse', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a>.
				        </input>
				        </div>
				        <div>
							<a id="dhGetStarted"><img id="dhGetStartedButton" src="/images3/${buildStamp}/get_started_disabled.gif"/></a>
						</div>
					</div>
				</td>
				</tr>
				</table>
			</c:when>
			<c:when test="${includeDownload && signin.user.account.needsDownload}">
				<div id="dhDownloadMessageClose">
					<a href="javascript:dh.actions.removeDownloadMessage()"><img src="/images3/${buildStamp}/alert_x_box.gif" width="10" height="10"/></a>
				</div>
				<div id="dhDownloadMessage">
					<dh:png src="/images3/${buildStamp}/star.png" style="width: 13px; height: 13px;"/>
					Make the most of Mugshot by downloading our desktop software. <a href="/download">Download now</a>.
				</div>
			</c:when>
		</c:choose>
	</div> 
	</c:when>
</c:choose>
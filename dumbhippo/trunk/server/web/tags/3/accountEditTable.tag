<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="account" required="true" type="com.dumbhippo.web.pages.AccountPage" %>
<%@ attribute name="termsOfUseNote" required="false" type="java.lang.Boolean" %>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="page"/>

<c:choose>
    <c:when test="${browser.ie}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_ie.gif"/>
        <c:set var="browseInputSize" value="0"/>
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <%-- This is ok here, but don't use browser.firefox on the anonymous pages --%>
    <%-- for which we use a single cache for all gecko browsers. --%>
    <c:when test="${browser.firefox && browser.windows}">
        <c:set var="browseButton" value="/${siteImageDir}/${buildStamp}/browse_ff.gif"/>   
        <c:set var="browseInputSize" value="1"/>
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:when test="${browser.firefox && browser.linux}">
        <c:set var="browseButton" value="/${siteImageDir}/${buildStamp}/browse_lff.gif"/>   
        <c:set var="browseInputSize" value="1"/>  
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:otherwise>
        <c:set var="browseButton" value=""/>     
        <c:set var="browseInputSize" value="24"/>
        <c:set var="browseInputClass" value="dh-file-upload"/>
    </c:otherwise>
</c:choose>

<dht2:formTable tableId="dhAccountInfoForm">
    <c:if test="${dh:enumIs(site, 'MUGSHOT')}">	
	    <dht3:formTableRowSeparator>
	        <div class="dh-section-header">Public Info</div>
	        <div class="dh-section-explanation">
	            This information will be visible on your <a href="/person">Home</a> page.
	            <c:if test="${dh:enumIs(signin.user.account.accountType, 'GNOME')}">
	                You can modify your name, picture, and other information on your <a href="${signin.baseUrlGnome}/account">GNOME Online</a> account page.
	            </c:if>
	        </div>
        </dht3:formTableRowSeparator>    
    </c:if>
    <!-- we have name, picture, e-mail and IM inputs on the GNOME site if the person has GNOME account type,
         but have those inputs on the Mugshot site if their account is of type Mugshot -->
    <c:if test="${dh:enumIs(site, 'GNOME') || !dh:enumIs(signin.user.account.accountType, 'GNOME')}">
	    <dht2:formTableRow label="My name" controlId='dhUsernameEntry'>
		    <dht2:textInput id="dhUsernameEntry" extraClass="dh-name-input"/>
		    <div id="dhUsernameEntryDescription" style="display: none"></div>
	    </dht2:formTableRow>
	</c:if>    
	<c:if test="${dh:enumIs(site, 'MUGSHOT')}">	
		<dht2:formTableRow label="About me" altRow="${dh:enumIs(signin.user.account.accountType, 'MUGSHOT')}" controlId="dhBioEntry">
			<div>
				<dht2:textInput id="dhBioEntry" multiline="true"/>
				<div id="dhBioEntryDescription" style="display: none"></div>
			</div>
		</dht2:formTableRow>
	</c:if>
	<!-- music bio currently disabled
	<dht2:formTableRowStatus controlId='dhMusicBioEntry'></dht2:formTableRowStatus>
	<dht2:formTableRow label="Your music bio">
		<div>
			<dht2:textInput id="dhMusicBioEntry" multiline="true"/>
		</div>
	</dht2:formTableRow>
	-->
	<c:if test="${dh:enumIs(site, 'GNOME') || dh:enumIs(signin.user.account.accountType, 'MUGSHOT')}">
	    <dht2:formTableRow label="My picture" altRow="${dh:enumIs(site, 'GNOME')}">
		    <div id="dhHeadshotImageContainer" class="dh-image">
			    <dht2:headshot person="${account.person}" size="60" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" />
		    </div>
		    <div class="dh-next-to-image">
			    <div class="dh-picture-instructions">Upload new picture:</div>
			    <c:set var="location" value="/headshots" scope="page"/>
			    <c:url value="/upload${location}" var="posturl"/>
			    <form id='dhPictureForm' enctype="multipart/form-data" action="${posturl}" method="post">
				    <input id='dhPictureEntry' class="${browseInputClass}" type="file" name="photo" size="${browseInputSize}"/>
				    <c:if test="${browseInputClass == 'dh-hidden-file-upload'}">
				        <div id='dhStyledPictureEntry' class="dh-styled-file-upload">
				            <img src="${browseButton}">
				        </div>
				    </c:if>
				    <input type="hidden" name="groupId" value=""/>
				    <input type="hidden" name="reloadTo" value="/account"/>
			    </form>
			    <div class="dh-picture-more-instructions">
			        or <a href="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" title="Choose from a library of pictures">choose a stock picture</a>						
			    </div>
			    <div id="dhChooseStockLinkContainer">
			    </div>
		    </div>
		    <div class="dh-grow-div-around-floats"><div></div></div>
	    </dht2:formTableRow>
	</c:if>    
	<dht2:formTableRow label="Accounts" altRow="${dh:enumIs(site, 'MUGSHOT')}">
		<dht3:accountEditTableExternals account="${account}"/>
	</dht2:formTableRow>
	<c:if test="${dh:enumIs(site, 'MUGSHOT')}">	
		<dht2:formTableRow label="Music Radar preferences">
			<div id="dhMusicRadarPreferences" class="dh-account-preferences-row">
		    Music sharing: 
		    <c:choose>
				<c:when test="${signin.musicSharingEnabled}">
					<dh:script module="dh.actions" />
					<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled"
						checked="true" onclick="dh.actions.setMusicSharingEnabled(true);">
					<label for="dhMusicOn">On</label>
					<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled"
						onclick="dh.actions.setMusicSharingEnabled(false);">
					<label for="dhMusicOff">Off</label>
				</c:when>
				<c:otherwise>
					<dh:script module="dh.actions" />
					<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled"
						onclick="dh.actions.setMusicSharingEnabled(true);">
					<label for="dhMusicOn">On</label>
					<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled"
						checked="true" onclick="dh.actions.setMusicSharingEnabled(false);">
					<label for="dhMusicOff">Off</label>
				</c:otherwise>
			</c:choose>
			<div>
			<a href="/radar-themes">Edit my Music Radar theme</a> | <a
				href="/getradar">Get Music Radar HTML</a>
			</div>
			</div>
		</dht2:formTableRow>
	</c:if><%-- End of MUGSHOT site --%>
	<c:if test="${dh:enumIs(site, 'GNOME')}">	
  	    <dht2:formTableRow label="GNOME Online services" altRow="true">
		<div id="dhApplicationUsagePreferences"
			class="dh-account-preferences-row">
		    Save application usage statistics (<a href="/applications">Learn more</a>)
		    <c:choose>
			<c:when
				test="${signin.user.account.applicationUsageEnabledWithDefault}">
				<dh:script module="dh.actions" />
				<input type="radio" name="dhApplicationUsageEnabled"
					id="dhApplicationUsageOn" checked="true"
					onclick="dh.actions.setApplicationUsageEnabled(true);">
				<label for="dhApplicationUsageOn">On</label>
				<input type="radio" name="dhApplicationUsageEnabled"
					id="dhApplicationUsageOff"
					onclick="dh.actions.setApplicationUsageEnabled(false);">
				<label for="dhApplicationUsageOff">Off</label>
			</c:when>
			<c:otherwise>
				<dh:script module="dh.actions" />
				<input type="radio" name="dhApplicationUsageEnabled"
					id="dhApplicationUsageOn"
					onclick="dh.actions.setApplicationUsageEnabled(true);">
				<label for="dhApplicationUsageOn">On</label>
				<input type="radio" name="dhApplicationUsageEnabled"
					id="dhApplicationUsageOff" checked="true"
					onclick="dh.actions.setApplicationUsageEnabled(false);">
				<label for="dhApplicationUsageOff">Off</label>
			</c:otherwise>
		</c:choose>
		</div>
	    </dht2:formTableRow>
	    <dht2:formTableRow label="Google services">
		<div id="dhGoogleServices"
			class="dh-account-preferences-row">
			<table>
			 	<c:forEach items="${account.person.allEmails}" var="email">
				<tr>
					<td><c:out value="${email.email}" /></td>
					<td>
						<c:choose>
							<c:when test="${dh:containerHas(account.person.dmo.googleEnabledEmails, email.email)}">
								<jsp:element name="input">
									<jsp:attribute name="type">checkbox</jsp:attribute>
									<jsp:attribute name="onclick">dh.actions.setGoogleServicedEmail(<dh:jsString value="${email.email}" />, false)</jsp:attribute>
									<jsp:attribute name="checked">true</jsp:attribute>
								</jsp:element>
							</c:when>
							<c:otherwise>
								<jsp:element name="input">
									<jsp:attribute name="type">checkbox</jsp:attribute>
									<jsp:attribute name="onclick">dh.actions.setGoogleServicedEmail(<dh:jsString value="${email.email}" />, true)</jsp:attribute>
								</jsp:element>								
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
				</c:forEach>
			</table>
		</div>
	    </dht2:formTableRow>	
	    <dht2:formTableRow label="Mugshot" icon="/images3/${buildStamp}/mugshot_icon.png" altRow="true">
	    <div class="dh-account-preferences-row">
			<c:choose>
			    <c:when test="${signin.user.account.disabled && signin.user.account.publicPage}">
			        Disabled because your GNOME Online account is disabled
			    </c:when>
			    <c:when test="${signin.user.account.publicPage}">
			        Enabled <a href="${signin.baseUrlMugshot}/account">Visit Your Account</a>    
			    </c:when>
			    <c:otherwise>
			        Disabled <a href="${signin.baseUrlMugshot}/account">Log in to Enable</a>  
			    </c:otherwise>
			</c:choose>    
			(<a href="${signin.baseUrlMugshot}/features">Learn more</a>)
		</div>	
	    </dht2:formTableRow>
	</c:if>
    <c:if test="${dh:enumIs(site, 'MUGSHOT')}">		
	    <dht3:formTableRowSeparator>
	        <div class="dh-section-header">Private Info</div>
	        <div class="dh-section-explanation">Nobody sees this stuff but you.</div>
        </dht3:formTableRowSeparator>
    </c:if>    
	<dht2:formTableRowStatus controlId='dhPasswordEntry'></dht2:formTableRowStatus>
	<dht2:formTableRow label="Set a password">
	    <div class="dh-explanation">
		A password is optional. You can also log into ${dh:xmlEscape(site.siteName)} by having a log in link sent to any of your
		email addresses or screen names from the <i>Log In</i> screen.
		</div>
		Enter password:
		<br/>
		<dht2:textInput id="dhPasswordEntry" type="password" extraClass="dh-password-input"/>
		<br/>
	    Re-type password:
		<br/>
		<dht2:textInput id="dhPasswordAgainEntry" type="password" extraClass="dh-password-input"/>
		<br/>
		<c:if test="${!account.hasPassword}">
			<c:set var="removePasswordLinkStyle" value="display: none;" scope="page"/>
		</c:if>
		<img id="dhSetPasswordButton" src="/${siteImageDir}/${buildStamp}/setpassword_disabled.gif"/><a id="dhRemovePasswordLink" style="${removePasswordLinkStyle}" href="javascript:dh.password.unsetPassword();" title="Delete my password">Delete my current password.</a>
	</dht2:formTableRow>
	<dht2:formTableRow label="Disable account" altRow="true">
	    <div class="dh-explanation">
	    <c:choose>	    
	        <c:when test="${dh:enumIs(site, 'GNOME')}">
	            Disabling your account means that we will not use any information about you
	            for any of the GNOME Online services, and we will never send you email for any reason.
	            <c:if test="${signin.user.account.publicPage}">	            
		            This will also disable your <b>Mugshot</b> account.
		        </c:if>	     	            	        
	        </c:when>
	        <c:otherwise>
	            Disabling your account means we won't show any information on your
		        public Home page, and we will never send you email for any reason.
		        You can enable your account again at any time.		            	
	            <c:if test="${dh:enumIs(signin.user.account.accountType, 'GNOME')}">	            
		            This will <b>not</b> disable your <b>GNOME Online</b> account.
		        </c:if>	        
	        </c:otherwise>
        </c:choose>     
		</div>
		<a name="accountStatus"></a>
		<c:if test="${termsOfUseNote=='true'}">
	        <div id="dhTermsOfUseNote">If you no longer agree with <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>, disable your account here.</div>
        </c:if>    
	    <div>
			<dh:script module="dh.actions"/>						
			<img id="dhDisableAccountButton" src="/${siteImageDir}/${buildStamp}/disable.gif" onclick="javascript:dh.actions.disableAccount();"/>
		</div>
		<div>
		</div>
	</dht2:formTableRow>
</dht2:formTable>

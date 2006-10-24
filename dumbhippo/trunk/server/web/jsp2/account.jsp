<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dh:bean id="account" class="com.dumbhippo.web.pages.AccountPage" scope="page"/>
<%-- This is a Facebook authetication token, we can create a seperate post-facebook-login landing page --%>
<%-- or land people on their account page, get a token, and display a message --%>
<jsp:setProperty name="account" property="facebookAuthToken" param="auth_token"/>

<c:set var="termsOfUseNote" value='false'/>
<c:if test='${!empty param["termsOfUseNote"]}'>
    <c:set var="termsOfUseNote" value='${param["termsOfUseNote"]}'/> 
</c:if>

<head>
	<title>Your Account</title>
	<dht:siteStyle/>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/account.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.account");
		dojo.require("dh.password");
		dh.formtable.currentValues = {
			'dhUsernameEntry' : <dh:jsString value="${signin.user.nickname}"/>,
			'dhBioEntry' : <dh:jsString value="${signin.user.account.bio}"/>,
			'dhMusicBioEntry' : <dh:jsString value="${signin.user.account.musicBio}"/>,
			'dhRhapsodyListeningHistoryEntry' : <dh:jsString value="${account.rhapsodyListeningHistoryFeedUrl}"/>,
			'dhWebsiteEntry' : <dh:jsString value="${account.websiteUrl}"/>,
			'dhBlogEntry' : <dh:jsString value="${account.blogUrl}"/>
		};
		dh.account.userId = <dh:jsString value="${signin.user.id}"/>
		dh.account.reloadPhoto = function() {
			dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer')], 60);
		}
		dh.account.initialMyspaceName = <dh:jsString value="${account.mySpaceName}"/>;
		dh.account.initialMyspaceHateQuip = <dh:jsString value="${account.mySpaceHateQuip}"/>;
		dh.account.initialYouTubeName = <dh:jsString value="${account.youTubeName}"/>;
		dh.account.initialYouTubeHateQuip = <dh:jsString value="${account.youTubeHateQuip}"/>;
		dh.account.initialFlickrEmail = <dh:jsString value="${account.flickrEmail}"/>;
		dh.account.initialFlickrHateQuip = <dh:jsString value="${account.flickrHateQuip}"/>;
		dh.account.initialLinkedInName = <dh:jsString value="${account.linkedInName}"/>;
		dh.account.initialLinkedInHateQuip = <dh:jsString value="${account.linkedInHateQuip}"/>;	
	</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxAccount>
			<c:choose>
			<c:when test="${!signin.user.account.disabled}">
				<dht:zoneBoxTitle>PUBLIC INFO</dht:zoneBoxTitle>
				<dht:zoneBoxSubtitle>This information will be visible on your <a href="/person?who=${signin.user.id}">profile page</a>.</dht:zoneBoxSubtitle>
				<dht:formTable>
				<dht:formTableRowStatus controlId='dhUsernameEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="User name">
					<dht:textInput id="dhUsernameEntry" extraClass="dh-username-input"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhBioEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="About me">
					<%--
					<div>
						<input type="button" value="Generate a random bio!" onclick="dh.account.generateRandomBio();"/>
					</div>
					--%>
					<div>
						<dht:textInput id="dhBioEntry" multiline="true"/>
					</div>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhMusicBioEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Your music bio">
				    <%--
					<div>
						<input type="button" value="Generate a random music bio!" onclick="dh.account.generateRandomBio();"/>
					</div>
					--%>
					<div>
						<dht:textInput id="dhMusicBioEntry" multiline="true"/>
					</div>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhPictureEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Picture">
					<div id="dhHeadshotImageContainer" class="dh-image">
						<dht:headshot person="${account.person}" size="60" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" />
					</div>
					<div class="dh-next-to-image">
						<div>Upload new picture:</div>
						<c:set var="location" value="/headshots" scope="page"/>
						<c:url value="/upload${location}" var="posturl"/>
						<div>
							<form enctype="multipart/form-data" action="${posturl}" method="post">
								<input id='dhPictureEntry' type="file" name="photo"/>
								<input type="hidden" name="groupId" value=""/>
								<input type="hidden" name="reloadTo" value="/account"/>
							</form>
						</div>
						<div id="dhChooseStockLinkContainer">
							or <a href="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" title="Choose from a library of pictures">choose stock picture</a>
						</div>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</dht:formTableRow>
				<dht:formTableRow label="Flickr">
					<dht:loveHateEntry baseId="dhFlickr" mode="${account.flickrSentiment}"/>
				</dht:formTableRow>				
				<dht:formTableRow label="LinkedIn profile">
					<dht:loveHateEntry baseId="dhLinkedIn" mode="${account.linkedInSentiment}"/>
				</dht:formTableRow>
				<dht:formTableRow label="MySpace name">
					<dht:loveHateEntry baseId="dhMyspace" mode="${account.mySpaceSentiment}"/>
				</dht:formTableRow>
				<dht:formTableRow label="YouTube name">
					<dht:loveHateEntry baseId="dhYouTube" mode="${account.youTubeSentiment}"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhRhapsodyListeningHistoryEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Rhapsody feed">
					<dht:textInput id="dhRhapsodyListeningHistoryEntry" maxlength="255"/>
					<a href="http://www.rhapsody.com/myrhapsody/rss.html" target="_blank" class="dh-text-input-help">help me find it</a>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhWebsiteEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="My website">
					<dht:textInput id="dhWebsiteEntry" maxlength="255"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhBlogEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="My blog">
					<dht:textInput id="dhBlogEntry" maxlength="255"/>
				</dht:formTableRow>				
				<tr valign="top">
	                <td colspan="2">
	                    <c:if test="${account.facebookAuthToken != null}">
                            <div id="dhFacebookNote">Thank you for logging in to Facebook! You and your friends will now be getting Facebook updates.</div>                     
                        </c:if>   	                
                    </td>
                </tr>     
				<dht:formTableRow label="Login to Facebook">
					<a href="http://api.facebook.com/login.php?api_key=${account.facebookApiKey}&next=/account" target="_blank"><img src="http://static.facebook.com/images/devsite/facebook_login.gif"></a>
				</dht:formTableRow>
			</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>FRIENDS ONLY INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>This information will only be seen by friends.</dht:zoneBoxSubtitle>
			<dht:formTable>
				<dht:formTableRowStatus controlId='dhEmailEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Email addresses">
					<table cellpadding="0" cellspacing="0" class="dh-address-table">
						<tbody>
							<c:forEach items="${account.person.allEmails}" var="email" varStatus="status">
								<tr>
									<td><c:out value="${email.email}"/></td>
									<td>
										<c:if test="${account.canRemoveEmails}">
											<c:set var="emailJs" scope="page">
												<jsp:attribute name="value">
													<dh:jsString value="${email.email}"/>
												</jsp:attribute>
											</c:set>
											<a href="javascript:dh.account.removeClaimEmail(${emailJs});">remove</a>
										</c:if>
									</td>
								</tr>
								<tr class="dh-email-address-spacer">
									<td></td>
									<td></td>
								</tr>
							</c:forEach>
							<tr><td><dht:textInput id='dhEmailEntry'/></td><td><input id='dhEmailVerifyButton' type="button" value="Verify" onclick="dh.account.verifyEmail();"/></td></tr>
						</tbody>
					</table>
				</dht:formTableRow>
				<dht:formTableRow label="AIM screen names">
					<c:forEach items="${account.person.allAims}" var="aim">
						<div class="dh-aim-address">
							<c:set var="aimJs" scope="page">
								<jsp:attribute name="value">
									<dh:jsString value="${aim.screenName}"/>
								</jsp:attribute>
							</c:set>
							<c:out value="${aim.screenName}"/>
							<c:if test="${!empty account.aimPresenceKey}">
								<a href="aim:GoIM?screenname=${aim.screenName}"><img src="http://api.oscar.aol.com/SOA/key=${account.aimPresenceKey}/presence/${aim.screenName}" border="0"/></a>
							</c:if>
							<a href="javascript:dh.account.removeClaimAim(${aimJs});">remove</a>
						</div>
					</c:forEach>
					<div>
						<a href="${account.addAimLink}">IM our friendly bot to add a new screen name</a>
					</div>
				</dht:formTableRow>
			</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>MUSIC RADAR</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>Control the Mugshot Music Radar feature.</dht:zoneBoxSubtitle>
				<dht:formTable>
					<dht:formTableRow label="Music Sharing">
					<c:choose>
					<c:when test="${signin.musicSharingEnabled}">
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>			
					</c:when>
					<c:otherwise>
						<input type="radio" id="dhMusicOn" name="dhMusicEmbedEnabled" onclick="dh.actions.setMusicSharingEnabled(true);"> <label for="dhMusicOn">On</label>
						<input type="radio" id="dhMusicOff" name="dhMusicEmbedEnabled" checked="true" onclick="dh.actions.setMusicSharingEnabled(false);">	<label for="dhMusicOff">Off</label>
					</c:otherwise>
					</c:choose>
					<div><a href="radar-learnmore">Music Radar</a> shows on your Mugshot what music you're listening to.
					<a href="/radar-themes">Edit Your Theme</a> - <a href="/getradar">Get Music Radar HTML</a>
					</div>
					</dht:formTableRow>
				</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SECURITY INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>Nobody sees this stuff but you.</dht:zoneBoxSubtitle>
				<dht:formTable>
					<dht:formTableRowStatus controlId='dhPasswordEntry'></dht:formTableRowStatus>
					<dht:formTableRow label="Set a password">
						<dht:textInput id="dhPasswordEntry" type="password" extraClass="dh-password-input"/>
					</dht:formTableRow>
					<dht:formTableRow label="Re-type password">
						<dht:textInput  id="dhPasswordAgainEntry" type="password" extraClass="dh-password-input"/><span style="width: 10px; height: 5px;"></span><input id="dhSetPasswordButton" type="button" value="Set password"/>
					</dht:formTableRow>
					<dht:formTableRow label="">
						A password is optional; you can also log in by sending a login link to any of your
						email addresses or screen names.
						<c:if test="${!account.hasPassword}">
							<c:set var="removePasswordLinkStyle" value="display: none;" scope="page"/>
						</c:if>
						<a id="dhRemovePasswordLink" style="${removePasswordLinkStyle}" href="javascript:dh.password.unsetPassword();" title="Remove my password">Remove my current password.</a>
					</dht:formTableRow>
					<dht:formTableRow label="Disable account">
					    <a name="accountStatus"></a>
					    <c:if test="${termsOfUseNote=='true'}">
                            <div id="dhTermsOfUseNote">If you no longer agree with <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>, disable your account here.</div>
                        </c:if>    
						<div>
							<input type="button" value="Disable account" onclick="javascript:dh.actions.disableAccount();"/>
						</div>
						<div>
							Disabling your account means we don't show any information on your
							public profile page, and we will never send you email for any reason.
							You can enable your account again at any time.
						</div>
					</dht:formTableRow>
				</dht:formTable>
				</c:when>
				<c:otherwise>
				    <%-- This code is never hit, because we redirect to /we-miss-you in this case. --%>
					<dht:formTable>				
					<dht:formTableRow label="Enable account">
					    <a name="accountStatus"></a>
					    <c:if test="${termsOfUseNote=='true'}">
                            <div id="dhTermsOfUseNote">Your account is already disabled, which means you no longer agree with <a href="javascript:window.open('/terms', 'dhTermsOfUs', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Terms of Use</a> and <a href="javascript:window.open('/privacy', 'dhPrivacy', 'menubar=no,scrollbars=yes,width=600,height=600');void(0);">Privacy Policy</a>.</div>
                        </c:if>     
					    <div class="dh-account-disabled-message">
						    <c:out value="${signin.user.nickname}" />, your account is disabled.
						</div>
						<div>
						<p>
							Enable your account to interact with friends and groups, share 
							links, and use Music Radar to display your playlists.
						</p>
						</div>
						<div>
							<input type="button" value="Enable account" onclick="javascript:dh.actions.enableAccount();"/>
						</div>
					</dht:formTableRow>						
					</dht:formTable>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxAccount>
	</dht:contentColumn>
	<dht:photoChooser/>
</dht:twoColumnPage>
</html>

<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dh:bean id="account" class="com.dumbhippo.web.AccountPage" scope="page"/>

<head>
	<title>Your Account</title>
	<link rel="stylesheet" type="text/css" href="/css2/account.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.account");
		dojo.require("dh.password");
		dh.formtable.currentValues = {
			'dhUsernameEntry' : <dh:jsString value="${signin.user.nickname}"/>,
			'dhBioEntry' : <dh:jsString value="${signin.user.account.bio}"/>,
			'dhMusicBioEntry' : <dh:jsString value="${signin.user.account.musicBio}"/>,
			'dhMyspaceEntry' : <dh:jsString value="${signin.user.account.mySpaceName}"/>
		};
		dh.account.userId = <dh:jsString value="${signin.user.id}"/>
		dh.account.reloadPhoto = function() {
			dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer')], 60);
		}
	</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxAccount>
			<c:choose>
			<c:when test="${!signin.user.account.disabled}">
				<dht:zoneBoxTitle>PUBLIC INFO</dht:zoneBoxTitle>
				<dht:zoneBoxSubtitle>This information will be visible on your profile page.</dht:zoneBoxSubtitle>
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
				<dht:formTableRow label="My music bio">
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
						<dht:headshot user="${signin.user}" size="60" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.account.reloadPhoto);" />
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
				<dht:formTableRowStatus controlId='dhMyspaceEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="MySpace name">
					<dht:textInput id="dhMyspaceEntry"/>
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
							<c:out value="${aim.screenName}"/><a href="javascript:dh.account.removeClaimAim(${aimJs});">remove</a>
						</div>
					</c:forEach>
					<div>
						<a href="${account.addAimLink}">IM our friendly bot to add a new screen name</a>
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
						<div>
							<input type="button" value="Disable account" onclick="javascript:dh.actions.setAccountDisabled(true);"/>
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
					<dht:formTable>
					<dht:formTableRow label="Enable account">
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
							<input type="button" value="Enable account" onclick="javascript:dh.actions.setAccountDisabled(false);"/>
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

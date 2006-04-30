<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Your Account</title>
	<link rel="stylesheet" type="text/css" href="/css2/account.css"/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.account");
		dh.account.currentValues = {
			'dhUsernameEntry' : <dh:jsString value="${signin.user.nickname}"/>,
			'dhBioEntry' : <dh:jsString value="${signin.user.account.bio}"/>,
			'dhMyspaceEntry' : <dh:jsString value="${signin.user.account.mySpaceName}"/>
		};
	</script>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxAccount>
			<dht:zoneBoxTitle>PUBLIC INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>This information will be visible on your profile page.</dht:zoneBoxSubtitle>
			<dht:formTable>
				<dht:formTableRowStatus controlId='dhUsernameEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="User name">
					<dht:textInput id="dhUsernameEntry" extraClass="dh-username-input"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhBioEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="About me">
					<div>
						<input type="button" value="Generate a random bio!" onclick="dh.account.generateRandomBio();"/>
					</div>
					<div>
						<dht:textInput id="dhBioEntry" multiline="true"/>
					</div>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhPictureEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Picture">
					<div class="dh-image">
						<dht:headshot user="${signin.user}" size="60"/>
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
						<div>
							or <a href="FIXME">choose stock picture</a>
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
				<dht:formTableRow label="Email addresses">
					<table>
						<tbody>
							<tr><td>foo@bar.com</td><td><a href="FIXME">remove</a></td><td></td></tr>
							<tr><td>foo@bar.com</td><td><a href="FIXME">remove</a></td><td></td></tr>
							<tr><td><dht:textInput/></td><td><input type="button" value="Verify"/></td><td></td></tr>
						</tbody>
					</table>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhEmailEntry'>Verification mail sent to foo@bar.com.</dht:formTableRowStatus>
				<dht:formTableRow label="AIM screen names">
					<table>
						<tbody>
							<tr><td>foobar2000</td><td></td><td></td></tr>
							<tr><td><dht:textInput/></td><td><input type="button" value="Verify"/></td><td></td></tr>
						</tbody>
					</table>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhAimEntry'>Verification IM sent to foobar123.</dht:formTableRowStatus>
			</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SECURITY INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>Nobody sees this stuff but you.</dht:zoneBoxSubtitle>
				<dht:formTable>
					<dht:formTableRow label="Set a password">
						<dht:textInput id="dhPasswordEntry" type="password" extraClass="dh-password-input"/>
					</dht:formTableRow>
					<dht:formTableRow label="Re-type password">
						<dht:textInput  id="dhPasswordAgainEntry" type="password" extraClass="dh-password-input"/><span style="width: 10px;"></span><input id="dhSetPasswordButton" type="button" value="Set password"/>
					</dht:formTableRow>				
					<dht:formTableRow label="Disable account">
						<div>
							<input type="button" value="Disable account"/>
						</div>
						<div>
							Disabling your account means you have no public profile page, and we will 
							never send you email for any reason. You can enable your account again at 
							any time.
						</div>
					</dht:formTableRow>
				</dht:formTable>
		</dht:zoneBoxAccount>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>

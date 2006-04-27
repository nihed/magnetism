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
	<link rel="stylesheet" type="text/css" href="/css2/site.css"/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage neverShowSidebar="true">
	<dht:contentColumn>
		<dht:zoneBoxAccount>
			<dht:zoneBoxTitle>PUBLIC INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>This information will be visible on your profile page.</dht:zoneBoxSubtitle>
			<dht:formTable hasSaveCancelButtons="true">
				<dht:formTableRow label="User name">
					<dht:textInput/>
				</dht:formTableRow>
				<dht:formTableRow label="About me">
					<div>
						<input type="button" value="Generate a random bio!"/>
					</div>
					<div>
						<dht:textInput multiline="true"/>
					</div>
				</dht:formTableRow>				
			</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>FRIENDS ONLY INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>This information will only be seen by friends.</dht:zoneBoxSubtitle>
			<dht:formTable>
				<dht:formTableRow label="Email addresses">
					<div>foo@bar.com</div>
					<div><a href="">Add an additional email address</a></div>
				</dht:formTableRow>
			</dht:formTable>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SECURITY INFO</dht:zoneBoxTitle>
			<dht:zoneBoxSubtitle>Nobody sees this stuff but you.</dht:zoneBoxSubtitle>
				<dht:formTable>
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

<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:set var="pageName" value="Buttons" scope="page"/>

<c:choose>
	<%-- this is set in request scope since some of the tags used only on this page use it --%>
	<c:when test="${signin.valid}">
		<c:set var="buttonLink" value="${baseUrl}/person?who=${signin.user.id}" scope="request"/>
	</c:when>
	<c:otherwise>
		<c:set var="buttonLink" value="${baseUrl}/" scope="request"/>
	</c:otherwise>
</c:choose>

<head>
	<title><c:out value="${pageName} - Mugshot"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="buttons"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="buttons">
	<dht3:shinyBox color="grey">
		<div class="dh-buttons-header-box">
			<div class="dh-buttons-header-image" style="width: 250px; height: 113px;">
				<dh:png src="/images3/${buildStamp}/love_yr_mugshot_logo.png" style="width: 250px; height: 113px;"/>
			</div>
			<div class="dh-buttons-header-text">
				<table cellspacing="0" cellpadding="0" height="113">
					<tbody>
						<tr valign="center">
							<td>
								<div class="dh-buttons-header-text-large">
									Can't get enough Mugshot? Tell the world and build your network. (And ours!)
								</div>
								<div class="dh-buttons-header-text-small">
									Add our badges to your site or blog, linking to your Mugshot page. Instructions are below.
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
			<div class="dh-grow-div-around-floats"></div>
		</div>

		<div class="dh-buttons-main-box">
			<div class="dh-buttons-choices-box">
				<dht3:buttonPreviewAndCode image="mugshot_50x50.gif"      description="50x50 AIM icon" imageWidth="50" imageHeight="50"/>
				<dht3:buttonPreviewAndCode image="mugshot_80x15.gif"      description="80x15" 		   imageWidth="80" imageHeight="15"/>
				<dht3:buttonPreviewAndCode image="mugshot_88x31.gif"      description="88x31"          imageWidth="88" imageHeight="31"/>
				<dht3:buttonPreviewAndCode image="mugshotlove_88x31.gif"  description="88x31"          imageWidth="88" imageHeight="31"/>
				<dht3:buttonPreviewAndCode image="mugshot_94x20.gif"      description="94x20"          imageWidth="94" imageHeight="20"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x60.gif"     description="120x60"         imageWidth="120" imageHeight="60"/>
				<dht3:buttonPreviewAndCode image="mugshotlove_120x60.gif" description="120x60"         imageWidth="120" imageHeight="60"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x90.gif"     description="120x90"         imageWidth="120" imageHeight="90"/>				
				<dht3:buttonPreviewAndCode image="mugshotlove_120x90.gif" description="120x90"         imageWidth="120" imageHeight="90"/>
				<dht3:buttonPreviewAndCode image="mugshot_120x240.gif"    description="120x240"        imageWidth="120" imageHeight="240"/>				
			</div>
			
			<div class="dh-buttons-instructions-box">
				<div class="dh-buttons-instructions-headline">How to add buttons in...</div>
				<%-- No one is willing to bite the bullet on installing the AOL AIM
				<dht3:buttonInstructions title="AIM">
					<ol>
						<li>Install AIM Triton</li>
						<li>Weed through hundreds of advertisements scattered on your desktop and web browsers</li>
						<li>Go to add / remove applications and remove AIM</li>
						<li>Install Trillian or Gaim</li>
					</ol>
				</dht3:buttonInstructions>
				--%>
				<dht3:buttonInstructions title="Blogger">
					<ol>
						<li>Log into Blogger</li>
						<li>Click "Layout"</li>
						<li>On the sidebar of your layout, click "Add a Page Element"</li>
						<li>Under "HTML/Javascript" click "Add to Blog"</li>
						<li>Add a title like "Mugshot Rocks!"</li>
						<li>Copy the code of the badge and past into the "Content" box</li>
						<li>Click "Save Changes"</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="Blogger (beta)">
					<ol>
						<li>Log into Blogger with your Google Account</li>
						<li>Click "Template"</li>
						<li>Scroll down to the "Archive Links" section</li>
						<li>Copy the code of the badge into the template</li>
						<li>Click "Save Template Changes"</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="LiveJournal"><%-- Haven't verified these instructions are valid --%>
					<ol>
						<li>Log into LiveJournal</li>
						<li>Hover over "Manage" and click "Info"</li>
						<li>Copy the code of the badge and past into the "Bio" box</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="MySpace">
					<ol>
						<li>Log into MySpace and click "Edit Profile"</li>
						<li>Copy the code of a badge and past into a section like "Interests" or "About Me"</li>
						<li>Click "Preview Section" and then "Save All Changes"</li>
					</ol>
				</dht3:buttonInstructions>	
				<dht3:buttonInstructions title="TypePad"><%-- Haven't verified these instructions are valid --%>
					<ol>
						<li>Log into TypePad</li>
						<li>Click on "TypeLists"</li>
						<li>Select List type "Notes", give the List a name and click "Create new list"</li>
						<li>Click "Add this list to your weblog(s) or About Page"</li>
						<li>Check the box next to your weblog and click "Save Changes"</li>
						<li>Click "Add a new item"</li>
						<li>Copy the code of a badge and paste into the "Note" box</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="WordPress">
					<ol>
						<li>Log into your WordPress Dashboard</li>
						<li>Click on "Links" or "Blogroll" and then "Add Link"</li>
						<li>Type "Mugshot" into "Name" or "Link Name"</li>
						<li>Copy and paste the "href" URL of the badge code into the "URI" or "Address"</li>
						<li>Copy and past the "src" URL of the badge code into the "Image URI" down below</li>
						<li>Click "Add Link"</li>
					</ol>
				</dht3:buttonInstructions>
				<dht3:buttonInstructions title="Yahoo! Mail"><%-- Haven't verified these instructions are valid --%>
					<ol>
						<li>Log into Yahoo! Mail</li>
						<li>Click "Options" and then "Mail Options"</li>
						<li>Click the "Compose" section and select "Show a signature on all outgoing messages"</li>
						<li>Copy the code of a badge and paste into the "Signature" box</li>
						<li>Click "Rich Text" and then click "Save Changes"</li>
					</ol>
				</dht3:buttonInstructions>
			</div>
		
			<div class="dh-grow-div-around-floats"></div>
		</div>

		<div class="dh-grow-div-around-floats"></div>
	</dht3:shinyBox>
</dht3:page>

</html>

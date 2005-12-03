<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sharing a Link</title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dht:scriptIncludes/>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<script type="text/javascript">
		dojo.require("dh.sharelink");
	</script>
</head>
<body>
	<dht:header>
		Sharing a Link
	</dht:header>

	<div id="dhMain">
		<!--  invisible at first to avoid flicker while we set up dojo widgets -->
		<div id="dhShareLinkForm" class="dhInvisible">
			<div class="dhVerticalPadding"></div>
			
			<table cols="1" class="url">
			<tbody>
			<tr>
			<td>
				<div dojoType="InlineEditBox" id="dhUrlTitleToShare">(untitled)</div>
				<div id="dhUrlToShareDiv" class="dhLabel dhInvisible">
					Link: <input id="dhUrlToShare" type="text" class="dhText"/>
				</div>
			</td>
			</tr>
			<tr>
			<td>
				<div id="dhTitleError" class="dhValidityError"></div>
			</td>
			</tr>
<!-- Getting rid of this item for the short term as it's useless
			<tr>
			<td style="text-align: right;">
				<input type="checkbox" id="dhSecretCheckbox"/>Secret
			</td>
			</tr>
-->
			</tbody>
			</table>
			
			<div class="dhVerticalPadding"></div>
			<div class="dhLabel">Share <u>W</u>ith:</div>
			
			<table>
			<tr>
			<td>
				<input autocomplete="off" accesskey="w"
						type="text" id="dhRecipientComboBox" class="dhText"/>
				<div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div><!-- this could be anywhere in the document really -->
			</td>
			<td>
				<input type="button" value="Add" accesskey="a" class="dhButton" 
						onclick="dh.share.doAddRecipientFromCombo();"/>
			</td>
			</tr>
			</table>
			
			<div id="dhRecipientsError" class="dhValidityError"></div>
	
			<div class="dhVerticalPadding"></div>
			
			<div id="dhRecipientListArea" class="dhBackgroundBox">		
				<div id="dhRecipientList"> </div>
				
				<div id="dhCreateGroupPopup" class="dhItemBox dhInvisible">
					<div class="dhLabel">New <u>G</u>roup Name</div>
					<div>
						<input id="dhCreateGroupName"/>
						<input type="button" accesskey="g" class="dhButton" value="Create" 
							onclick="dh.sharelink.doCreateGroup();"/>
					</div>
					<div id="dhCreateGroupAccessButtons">
						<input type="radio" id="dhCreateGroupPrivateRadio" name="groupAccess" />
						<a href="javascript:dh.util.toggleCheckBox('dhCreateGroupPrivateRadio');dh.sharelink.updateAccessTip();">Private</a>
						<input type="radio" id="dhCreateGroupPublicRadio" name="groupAccess" checked="checked"/>
						<a href="javascript:dh.util.toggleCheckBox('dhCreateGroupPublicRadio');dh.sharelink.updateAccessTip();">Public</a>
			
						<div id="dhPrivateGroupAccessTip" class="dh-help-bubble">
							<dh:png style="left:27px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
							<div class="dh-help-bubble-message">
								Private groups are for <strong>families</strong> and super secret CIA agents
								<a href="/privacy" target="_blank">privacy</a>
							</div><!-- help bubble message -->
						</div><!-- help bubble -->
						<div id="dhPublicGroupAccessTip" class="dh-help-bubble">
							<dh:png style="left:110px;" klass="dh-help-bubble-triangle" src="/images/${buildStamp}/triangle.png"/>
							<div class="dh-help-bubble-message">
								Public groups are for <strong>friends</strong>, <strong>co-workers</strong>, and others who don't
								mind people seeing the links they are sharing.
								<a href="/privacy" target="_blank">privacy</a>
							</div><!-- help bubble message -->
						</div><!-- help bubble -->
					</div>
					<p id="dhCreateGroupStatus" class="dhStatusLabel dhInvisible"></p>
				</div>
				<a id="dhCreateGroupLink" class="dhActionLink dhInvisible"
					 	onmouseover="dh.sharelink.highlightPossibleGroup();"
				 		onmouseout="dh.sharelink.unhighlightPossibleGroup();"
					 	href="javascript:dh.sharelink.toggleCreateGroup();">
				 	Create Group from These
				</a>
				<a id="dhAddMemberLink" class="dhActionLink dhInvisible"
		 				onmouseover="dh.sharelink.highlightPossibleGroup();"
					 	onmouseout="dh.sharelink.unhighlightPossibleGroup();"
		 				href="javascript:dh.sharelink.doAddMembers();">
					 Add <span id="dhAddMemberDescription"></span> to <span id="dhAddMemberGroup"></span>
				</a>
			</div><!-- end of div containing create group link and recipient list -->
	
			<div class="dhVerticalPadding"></div>
	
			<div class="dhLabel"><u>D</u>escription:</div>
			<div class="dhTextArea" id="dhShareLinkDescription">
			</div>
	
			<div class="dhVerticalPadding"></div>
	
			<input type="button" value="Share" accesskey="s"
					class="dhButton share" onclick="dh.sharelink.submitButtonClicked();"/>
	
			<div class="dhVerticalPadding"></div>
	
			<div id="dojoDebug"/> <!-- where to put dojo debug spew -->
		</div><!-- end dhShareLinkForm -->

	</div>
</body>
</html>

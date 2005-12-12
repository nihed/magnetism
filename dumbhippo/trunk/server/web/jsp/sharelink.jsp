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
<body scroll="no">
	<dht:header>
		Sharing a Link
	</dht:header>

	<div id="dhMain">
		<!--  invisible at first to avoid flicker while we set up dojo widgets -->
		<div id="dhShareForm" class="dhInvisible">
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
			<tbody>
			<tr>
			<td>
				<input autocomplete="off" accesskey="w"
						type="text" id="dhRecipientComboBox" class="dhText"/>
				<!-- dh:png id="dhRecipientComboBoxButton" src="/images/${buildStamp}/triangle.png" style="width: 27; height: 21;"/> -->
				<img id="dhRecipientComboBoxButton" src="/images/${buildStamp}/arrow.gif" />
				<div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div>
			</td>
			<td>
				<input type="button" value="Add" accesskey="a" class="dhButton" 
						onclick="dh.share.autoSuggest.activate();"/>
			</td>
			</tr>
			</tbody>
			</table>
			
			<div id="dhRecipientsError" class="dhValidityError"></div>
	
			<div class="dhVerticalPadding"></div>
			
			<dht:recipientList/>
	
			<div class="dhVerticalPadding"></div>
	
			<div class="dhLabel"><u>D</u>escription:</div>
			<div class="dhTextArea" id="dhShareDescription">
			</div>
	
			<div class="dhVerticalPadding"></div>
	
			<input type="button" value="Share" accesskey="s"
					class="dhButton share" onclick="dh.sharelink.submitButtonClicked();"/>
	
			<div class="dhVerticalPadding"></div>
			
			<div id="dhFlickrPhotoUpload"></div>
	
			<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
		</div><!-- end dhShareForm -->

	</div>
</body>
</html>

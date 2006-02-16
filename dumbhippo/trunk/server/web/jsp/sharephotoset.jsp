<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<%-- not used, client specifies title --%>
	<title></title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.sharephotoset");	
	</script>
</head>
<body>
	<dht:header>
		Sharing Photos
	</dht:header>

	<div id="dhMain">
		<!--  invisible at first to avoid flicker while we set up dojo widgets -->
		<div id="dhShareForm" class="dhInvisible">
			<div class="dhVerticalPadding"></div>
					
			<div id="dhFlickrError" class="dhValidityError"></div>	
			<div id="dhFlickrNotice" class="dhFlickrNotice"></div>						
			<table cols="2">
			<tbody>
			<tr>
			<td>
			<table cols="1">
			<tbody>
			<tr>
			<td>
				<div dojoType="InlineEditBox" id="dhUrlTitleToShare"></div>
			</td>
			</tr>
			<tr>
			<td>
				<div id="dhTitleError" class="dhValidityError"></div>
			</td>
			</tr>
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
	
			<dht:shareDescription/>
				
			<div class="dhVerticalPadding"></div>
	
			<input type="button" value="Share" accesskey="s"
					class="dhButton share" onclick="dh.sharephotoset.submitButtonClicked();"/>
			</td>
			<td>
			<div id="dhFlickrPhotoUpload"></div>
			</td>
			</tr>
			</table>
				
			<div class="dhVerticalPadding"></div>
			
			<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
		</div><!-- end dhShareForm -->

	</div>
</body>
</html>

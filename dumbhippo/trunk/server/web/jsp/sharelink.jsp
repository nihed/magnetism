<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sharing a Link</title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript" src="javascript/dh/share.js"></script>
	<script type="text/javascript" src="javascript/dh/sharelink.js"></script>
</head>
<body scroll="no">
<div id="dhShareOuterDiv">
<table id="dhShareOuter" cellspacing="0" cellpadding="0">
    <tr><td id="dhShareTL"><img src="/images/${buildStamp}/shareLinkTL.png"/></td>
    <td id="dhShareTop">&nbsp;</td>
    <td id="dhShareTR"><img src="/images/${buildStamp}/shareLinkTR.png"/></td>
    </tr>
    <tr>
    <td id="dhShareLeftBorder">&nbsp;</td>    
    <td id="dhShareMain">
    <table id="dhShareContainer" class="dhInvisible" cellspacing="0" cellpadding="0">
        <tr>
        <td id="dhShareLeft" width="46%">
        <table id="dhShareLeftContainer" cellspacing="0" cellpadding="0">
        <tr><td valign="top" width="100%">
        <img src="/images/${buildStamp}/dumbhippo_logo.gif"/><br/>
		<div dojoType="InlineEditBox" id="dhUrlTitleToShare">(untitled)</div>
     	<div id="dhUrlToShareDiv" class="dhLabel dhInvisible">
			Link: <input id="dhUrlToShare" type="text" class="dhText"/>
		</div>
		<div id="dhTitleError" class="dhValidityError"></div>		
        </td>
        <tr valign="bottom" height="80%">
        <td>
        <div id="dhShareSendTo">Send to:</div>
        <table id="dhShareRecipientsContainer" cellspacing="0" cellpadding="0" height="22px">
            <tr width="100%">
            <td valign="center"><input autocomplete="off" accesskey="w"
						type="text" id="dhShareRecipientComboBox" class="dhText" tabindex="1"/></td>
            <td id="dhShareDownArrow"><img id="dhShareRecipientComboBoxButton" src="/images/${buildStamp}/downarrow.gif"/></td>       
            <td><img onclick="dh.share.autoSuggest.activate();" accesskey="a" id="dhShareAddButton" src="/images/${buildStamp}/but_add.gif"/></td>
            <td><div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div></td>
            </tr>
            <tr height="2px"><td></td></tr>
        </table>
		<div id="dhShareRecipientsError" class="dhValidityError"></div>          
        <table id="dhShareRecipientsBox" cellspacing="0" cellpadding="0" width="100%" height="120px">
			<tr height="5px">		
    	    <td valign="top" align="left"><img src="/images/${buildStamp}/darkblue_tl.gif" class="dhShareTopCornerImg"/></td>
    	    <td>&nbsp;</td>
    	    <td valign="top" align="right"><img src="/images/${buildStamp}/darkblue_tr.gif" class="dhShareTopCornerImg"/></td>
    		</tr><tr>
    		<td width="5px">&nbsp;</td>
			<td valign="top"><dht:recipientList/></td>
			<td width="5px">&nbsp;</td>
			<tr height="5px">
			<td valign="bottom" align="left"><img src="/images/${buildStamp}/darkblue_bl.gif" class="dhShareBottomCornerImg"/></td>
			<td>&nbsp;</td>
			<td valign="bottom" align="right"><img src="/images/${buildStamp}/darkblue_br.gif" class="dhShareBottomCornerImg"/></td>
			</tr>
        </table> <!-- end dhShareRecipientsBox -->
        </td></tr>        
        </table> <!-- end dhShareLeftContainer -->
        </td> <!-- ends dhShareLeft -->

        <td style="width: 2px">&nbsp;</td>

        <td id="dhShareRight">
        <table id="dhShareRightOuterContainer" cellspacing="0" cellpadding="0" width="100%">
            <tr height="5px">
            <td align="left" valign="top"><img src="/images/${buildStamp}/blue_tl.gif" class="dhShareTopCornerImg"/></td>
            <td>&nbsp;</td>
            <td align="right" valign="top"><img src="/images/${buildStamp}/blue_tr.gif" class="dhShareTopCornerImg"/></td>
            </tr>
            <tr>
            <td></td><td><div id="dhShareDescriptionLabel">Description:</div>
            </td><td></td>
            </tr>
            <tr>
            <td></td><td id="dhShareDescriptionArea">
            <table id="dhShareDescriptionContainer" cellspacing="0" cellpadding="0" width="100%">
                <tr height="5px">
                <td align="left" valign="top"><img src="/images/${buildStamp}/whiteinner_tl.gif" class="dhShareTopCornerImg"/></td>
                <td>&nbsp;</td>
                <td align="right" valign="top"><img src="/images/${buildStamp}/whiteinner_tr.gif" class="dhShareTopCornerImg"/></td>
                </tr>
                <tr height="100%">
                <td style="width: 5px;">&nbsp;</td>
                <td align="center" valign="top" width="100%"><textarea id="dhShareDescriptionTextArea" name="dhShareDescriptionTextArea"></textarea></td>
                <td></td>
                <tr height="5px">
                <td align="left" valign="bottom"><img src="/images/${buildStamp}/whiteinner_bl.gif" class="dhShareBottomCornerImg"/></td>
                <td><div>&nbsp;</div></td>
                <td align="right" valign="bottom"><img src="/images/${buildStamp}/whiteinner_br.gif" class="dhShareBottomCornerImg"/></td>
                </tr>
            </table>
            </td>
            <td></td>
            </tr>
            <tr>
            <td align="left" valign="bottom"><img src="/images/${buildStamp}/blue_bl.gif" class="dhShareBottomCornerImg"/></td>
            <td valign="middle" align="left"><div id="dhShareDescriptionBottomFill"><dht:createGroupDialog/></div></td>
            <td align="right" valign="bottom"><div style="position: relative;"><img id="dhShareShareButton" src="/images/${buildStamp}/but_share.gif" onclick="dh.sharelink.submitButtonClicked();" tabindex="3"/></div></td>
            </tr>
        </table>    
        <div></div>

        </td> <!-- ends dhShareRight -->
        </tr>
    </table> <!-- ends dhShareContainer -->

    <td id="dhShareRightBorder">&nbsp;</td>
    </tr>
    <tr>
    <td valign="bottom" id="dhShareBL"><img src="/images/${buildStamp}/shareLinkBL.png"/></td>
    <td valign="bottom" id="dhShareBottom">&nbsp;</td>
    <td valign="bottom" id="dhShareBR"><img src="/images/${buildStamp}/shareLinkBR.png"></td>
    </tr>
</table>  
</div>    
<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->    
</body>
</html>

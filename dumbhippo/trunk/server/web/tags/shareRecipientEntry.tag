<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- This tag is used by sharelink.jsp and sharephotoset.jsp.  It
     is the HTML container for a recipient list; requires the
     JavaScript dh.sharelink to be loaded too --%>
       <table id="dhShareRecipientsContainer" cellspacing="0" cellpadding="0">
            <tr>
            <td align="left" valign="center"><input autocomplete="off" accesskey="w"
						type="text" id="dhShareRecipientComboBox" class="dhText" tabindex="1"/></td>
            <td align="left" id="dhShareDownArrow"><img id="dhShareRecipientComboBoxButton" src="/images/${buildStamp}/downarrow.gif"/></td>     
            <td align="right"><img onclick="dh.share.autoSuggest.activate();" accesskey="a" id="dhShareAddButton" src="/images/${buildStamp}/but_add.gif"/></td>
            <td align="left"><div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div></td>
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
			<td valign="top">
<div id="dhRecipientListAreaContainer">  
	<div id="dhRecipientListArea">		
		<div id="dhRecipientList">
			<table id="dhRecipientListTable" cellspacing="5px" cellpadding="0">
				<tbody><tr valign="top" id="dhRecipientListTableRow"></tr></tbody>
			</table>
		</div>
	</div>
</div>			
			</td>
			<td width="5px">&nbsp;</td>
			<tr height="5px">
			<td valign="bottom" align="left"><img src="/images/${buildStamp}/darkblue_bl.gif" class="dhShareBottomCornerImg"/></td>
			<td>&nbsp;</td>
			<td valign="bottom" align="right"><img src="/images/${buildStamp}/darkblue_br.gif" class="dhShareBottomCornerImg"/></td>
			</tr>
        </table> <!-- end dhShareRecipientsBox -->     

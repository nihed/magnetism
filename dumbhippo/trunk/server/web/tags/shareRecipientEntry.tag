<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- This tag is used by sharelink.jsp and sharephotoset.jsp.  It
     is the HTML container for a recipient list; requires the
     JavaScript dh.sharelink to be loaded too --%>
       <table id="dhShareRecipientsContainer" cellspacing="2" cellpadding="0">
            <tr>
            <td align="left" id="dhShareComboBoxCell" valign="center">
             	<input autocomplete="off" accesskey="w"
					type="text" id="dhShareRecipientComboBox" class="dhText" tabindex="1"/>
				<div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div>
			</td>
            <td align="left" id="dhShareDownArrow"><img id="dhShareRecipientComboBoxButton" src="/images2/${buildStamp}/orangedown.gif"/></td>     
            <td align="right" id="dhShareAddCell"><img onclick="dh.share.autoSuggest.activate();" accesskey="a" id="dhShareAddButton" src="/images2/${buildStamp}/addbutton.gif"/></td>
            </tr>
            <tr height="2px"><td></td></tr>
        </table>
		<div id="dhShareRecipientsError" class="dhValidityError"></div>          
        <table id="dhShareRecipientsBox" cellspacing="0" cellpadding="0" width="100%" height="120px">
			<tr height="8">		
    	    <td width="8" valign="top" align="left"><img src="/images2/${buildStamp}/lsorange_tl.png" class="dhShareTopCornerImg"/></td>
    	    <td></td>
    	    <td width="8" valign="top" align="right"><img src="/images2/${buildStamp}/lsorange_tr.png" class="dhShareTopCornerImg"/></td>
    		</tr>
    		<tr id="dhShareRecipientsBoxMainRow">
    		<td></td>
    		<td>
	 			<div id="dhRecipientList">
					<table id="dhRecipientListTable" cellspacing="0" cellpadding="0">
						<tbody><tr valign="top" id="dhRecipientListTableRow"></tr></tbody>
					</table>
				</div>		
			</td>
			<td></td>
			</tr>
			<tr height="8">
			<td valign="bottom" align="left"><img src="/images2/${buildStamp}/lsorange_bl.png" class="dhShareBottomCornerImg"/></td>
			<td></td>
			<td valign="bottom" align="right"><img src="/images2/${buildStamp}/lsorange_br.png" class="dhShareBottomCornerImg"/></td>
			</tr>
        </table> <!-- end dhShareRecipientsBox -->     

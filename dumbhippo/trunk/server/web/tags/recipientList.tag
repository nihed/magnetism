<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%-- This tag is used by sharelink.jsp and sharephotoset.jsp.  It
     is the HTML container for a recipient list; requires the
     JavaScript dh.sharelink to be loaded too --%>
<div id="dhRecipientListAreaContainer">  
	<div id="dhRecipientListArea">		
		<div id="dhRecipientList">
			<table id="dhRecipientListTable" cellspacing="5px" cellpadding="0">
				<tbody><tr valign="top" id="dhRecipientListTableRow"></tr></tbody>
			</table>
		</div>
	</div>
</div>
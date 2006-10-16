<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="blockId" required="true" type="java.lang.String" %>

<table class="dh-stacker-block-content" id="dhStackerBlockContent-${blockId}">
	<tr>
    	<td class="dh-stacker-block-content-left">&nbsp;</td>
        <td width="100%">
			<div class="dh-stacker-block-content-main">        
				<jsp:doBody/>
			</div>
        </td>
    </tr>
    <tr><td><div class="dh-stacker-block-content-padding">&nbsp;</div></td></tr>
</table>

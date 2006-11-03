<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="cssClass" required="true" type="java.lang.String" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="expandable" required="false" type="java.lang.Boolean" %>

<jsp:element name="table">
	<jsp:attribute name="class">dh-stacker-block <c:if test="${expandable}">dh-stacker-block-expandable </c:if>${cssClass}</jsp:attribute>
	<jsp:attribute name="id">dhStackerBlock-${blockId}</jsp:attribute>
	<jsp:attribute name="cellspacing">0</jsp:attribute>
	<jsp:attribute name="cellpadding">0</jsp:attribute>	
	<jsp:body>
	<tr>
		<td valign="top" align="left" width="20px"><dh:png klass="dh-stacker-block-icon" src="${block.icon}" style="width: 16; height: 16; border: none;"/></td>
		<jsp:doBody/>
	</tr>
	</jsp:body>
</jsp:element>
<c:if test="${expandable}">
<script type="text/javascript">
	var block = document.getElementById("dhStackerBlock-${blockId}");
	block.dhBlockId = "${blockId}";
	block.onclick = dh.stacker.onBlockClick;
	block.onmousemove = dh.stacker.onBlockMouseMove;	
	block.onmouseover = dh.stacker.onBlockMouseOver;
	block.onmouseout = dh.stacker.onBlockMouseOut;	
	dh.stacker.hookLinkChildren(block, block);
</script>
</c:if>


<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="isEditing" required="true" type="java.lang.Boolean" %>
<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="link" required="true" type="java.lang.String" %>
<%@ attribute name="baseId" required="true" type="java.lang.String" %>

<div>
	<div dhId="DescriptionNormal">
		<c:if test="${isEditing}">
			<div class="dh-love-hate-instruction-editing">
				Editing <c:out value="${name}"/>.<br/>	
			</div>
		</c:if>
		<div>
			<dh:png src="/images3/${buildStamp}/quiplove_icon.png" style="width: 12; height: 11; overflow: hidden;"/> 
			<a style="font-weight: bold;" href="javascript:dh.lovehate.setMode('${baseId}', 'loveEdit')" title="Click to change">Love it</a>: Enter your 
				<jsp:element name="a">
					<jsp:attribute name="href"><c:out value="${link}"/></jsp:attribute>
					<jsp:attribute name="target">_blank</jsp:attribute>
					<jsp:body><c:out value="${name}"/></jsp:body>
				</jsp:element> user info<span dhId="AccountHelpId"></span>.
					<%-- We use dhId because this tag is used in multiple places. --%>
			  		<span dhId="LoveTipId"></span>
	 	</div>
		<div>
			<dh:png klass="dh-love-hate-icon" src="/images3/${buildStamp}/quiphate_icon.png" style="width: 11; height: 11; overflow: hidden;"/>
			<a style="font-weight: bold;" href="javascript:dh.lovehate.setMode('${baseId}', 'hateEdit')" title="Click to change">Hate it</a>: 
				Let people see why you don't use 
				<jsp:element name="a">
					<jsp:attribute name="href"><c:out value="${link}"/></jsp:attribute>
					<jsp:attribute name="target">_blank</jsp:attribute>
					<jsp:body><c:out value="${name}"/></jsp:body>
				</jsp:element>.
			</span>
		</div>
	</div>
	<div dhId="DescriptionError" style="display: none">
		<span class="dh-love-hate-error-prefix">Error:</span> <span class="dh-love-hate-error-text" dhId="DescriptionErrorText"></span>
	</div>
</div>

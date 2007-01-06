<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="value" required="true" type="java.lang.String"%>

<%-- We need to uniquify ids across the generated output --%>
<c:if test="${empty dhNameEntryCount}">
	<c:set var="dhNameEntryCount" value="0" scope="request"/>
</c:if>
<c:set var="dhNameEntryCount" value="${dhNameEntryCount + 1}" scope="request"/>
<c:set var="N" value="${dhNameEntryCount}" scope="page"/>

<dh:script modules="dh.actions,dh.event,dojo.event.*,dojo.widget.HtmlInlineEditBox"/>
<script type="text/javascript">    
	function dhNameEntryInit${N}() {
		var entry = dojo.widget.manager.getWidgetById("dhNameEntry${N}");
		entry.dhOnSaveHandler = dh.actions.renamePersonHandler;
		dojo.event.connect(entry, "onSave", entry, "dhOnSaveHandler");
    }
   dh.event.addPageLoadListener(dhNameEntryInit${N});
</script>
<span dojoType="InlineEditBox" id="dhNameEntry${N}">
	<c:out value="${value}"/>
</span>

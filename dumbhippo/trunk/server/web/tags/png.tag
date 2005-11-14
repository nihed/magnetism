<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ attribute name="src" required="true" type="java.lang.String"%>
<%@ attribute name="klass" required="false" type="java.lang.String"%>
<%@ attribute name="id" required="false" type="java.lang.String"%>

<!--[if lt IE 7]>
	<img id="${id}" class="${klass}" style="filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='${src}', sizingMethod='scale')" />
<![endif]-->

<!-- this is display:none if IE lt 7 -->
<img id="${id}" class="dh-non-ie-png ${klass}" src="${src}"/>

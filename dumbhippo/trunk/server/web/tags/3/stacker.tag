<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="userId" required="true" type="java.lang.String" %>

<div id="dhStacker-${userId}">
	<script>
		dojo.require('dh.stacker');
		var dhInitStacker${userId} = function() {
			var stacker = dh.stacker.getInstance();
			stacker.setContainer(document.getElementById('dhStacker-${userId}'));
			stacker.start();
		}
		dojo.event.connect(dojo, "loaded", dj_global, "dhInitStacker${userId}");		
	</script>
</div>

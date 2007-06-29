<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<dh:script module="dh.event"/>
<script type="text/javascript">
	var dhBeaconPosition = 1;
	var dhBeaconSampleCount = 5;
	function dhBeaconSampleIterate () {
		dhBeaconPosition = dhBeaconPosition + 1;
		if (dhBeaconPosition > dhBeaconSampleCount)
			dhBeaconPosition = 1;
		var img = document.getElementById("dhBeaconSample")
		img.src = "/images2/${buildStamp}/beacon_samples/beaconsample" + dhBeaconPosition + ".jpg"
		window.setTimeout(function() {
					dhBeaconSampleIterate();
				}, 3000);	
		return false;
	}
	dh.event.addPageLoadListener(dhBeaconSampleIterate);
</script>

<img id="dhBeaconSample" src="/images2/${buildStamp}/beacon_samples/beaconsample1.jpg"/>

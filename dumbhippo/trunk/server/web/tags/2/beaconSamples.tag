<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<script>
	var dhBeaconPosition = 1;
	var dhBeaconSampleCount = 5;
	function dhBeaconSampleIterate () {
		dhBeaconPosition = dhBeaconPosition + 1;
		if (dhBeaconPosition > dhBeaconSampleCount)
			dhBeaconPosition = 1;
		var img = document.getElementById("dhBeaconSample")
		img.src = "/images2/beacon_samples/beaconsample" + dhBeaconPosition + ".jpg"
		window.setTimeout(function() {
					dhBeaconSampleIterate();
				}, 3000);	
		return false;
	}
	dojo.event.connect(dojo, "loaded", dj_global, "dhBeaconSampleIterate");
</script>

<img id="dhBeaconSample" src="/images2/beacon_samples/beaconsample1.jpg"/>

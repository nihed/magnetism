dojo.hostenv.conditionalLoadModule({
	common: [
		["dojo.math", false, false],
		["dojo.math.curves", false, false],
		["dojo.math.points", false, false]
	]
});
dojo.hostenv.moduleLoaded("dojo.math.*");

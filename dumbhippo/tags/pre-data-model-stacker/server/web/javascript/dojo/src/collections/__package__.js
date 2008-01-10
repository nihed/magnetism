dojo.hostenv.conditionalLoadModule({
	common: [
		"dojo.collections.Collections",
		"dojo.collections.List",
		"dojo.collections.SortedList", 
		"dojo.collections.Dictionary", 
		"dojo.collections.Queue", 
		"dojo.collections.ArrayList", 
		"dojo.collections.Stack",
		"dojo.collections.BinaryTree",
		"dojo.collections.SkipList",
		"dojo.collections.Graph",
		"dojo.collections.Set"
	]
});
dojo.hostenv.moduleLoaded("dojo.collections.*");

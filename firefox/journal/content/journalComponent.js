function JournalComponent()
{
	// Add any initialisation for your component here.
}

JournalComponent.prototype = {
QueryInterface: function(iid)
{
	if (iid.equals(Components.interfaces.myInterface)
		|| iid.equals(Ci.nsISupports))
	{
		return this;
	}
	else
	{
		throw Components.results.NS_ERROR_NO_INTERFACE;
	}
},
HelloWorld: function() {
  return "Hello, world!";
}
};

var initModule =
{
	ServiceCID: Components.ID("{}"),
	ServiceContractID: "@redhat.com/journal;1",
	ServiceName: "HistoryJournal",
	
	registerSelf: function (compMgr, fileSpec, location, type)
	{
		compMgr = compMgr.QueryInterface(Ci.nsIComponentRegistrar);
		compMgr.registerFactoryLocation(this.ServiceCID,this.ServiceName,this.ServiceContractID,
			fileSpec,location,type);
	},

	unregisterSelf: function (compMgr, fileSpec, location)
	{
		compMgr = compMgr.QueryInterface(Ci.nsIComponentRegistrar);
		compMgr.unregisterFactoryLocation(this.ServiceCID,fileSpec);
	},

	getClassObject: function (compMgr, cid, iid)
	{
		if (!cid.equals(this.ServiceCID))
			throw Components.results.NS_ERROR_NO_INTERFACE
		if (!iid.equals(Components.interfaces.nsIFactory))
			throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
		return this.instanceFactory;
	},

	canUnload: function(compMgr)
	{
		return true;
	},

	instanceFactory:
	{
		createInstance: function (outer, iid)
		{
			if (outer != null)
				throw Components.results.NS_ERROR_NO_AGGREGATION;
			return new JournalComponent().QueryInterface(iid);
		}
	}
}; //Module

function NSGetModule(compMgr, fileSpec)
{
	return initModule;
}

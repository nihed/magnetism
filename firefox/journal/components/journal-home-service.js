const CLASS_ID = Components.ID("{38fbce87-8d59-40ad-84e7-9f0c0b3490b5}");
const CLASS_NAME = "Journal Home Service";
const CONTRACT_ID = "@redhat.com/journalhome;1";

var jhs;

// Suck.
var console = {
  log: function() {
  }
}

function JournalHomeService()
{
    jhs = this;
    this.historyInstance = null;
}

JournalHomeService.prototype = {
  greeting: function() { 
    return "Hello World!"; 
  },
  QueryInterface: function(iid) {
    if (!iid.equals(Components.interfaces["nsIJournalHome"]) && !iid.equals(Components.interfaces["nsISupports"]))
      throw NS_ERROR_NO_INTERFACE;

    return this;
  },
}

var JournalHomeFactory = {
  createInstance: function (outer, iid) {
    if (outer != null)
      throw NS_ERROR_NO_AGGREGATION;

    return (new JournalHomeService()).QueryInterface(iid);
  }
};

var JournalHomeModule =
{
    registerSelf: function (compMgr, fileSpec, location, type)
    {
        compMgr = compMgr.QueryInterface(Components.interfaces["nsIComponentRegistrar"]);
        compMgr.registerFactoryLocation(CLASS_ID, CLASS_NAME, CONTRACT_ID, fileSpec, location, type);
    },

    unregisterSelf: function(compMgr, fileSpec, location)
    {        
        compMgr = compMgr.QueryInterface(Components.interfaces["nsIComponentRegistrar"]);
        compMgr.unregisterFactoryLocation(CLASS_ID, location);
    },

    getClassObject: function (compMgr, cid, iid)
    {
        if (!iid.equals(Components.interfaces["nsIFactory"]))
            throw NS_ERROR_NOT_IMPLEMENTED;
    
        if (cid.equals(CLASS_ID))
            return JournalHomeFactory;

        throw NS_ERROR_NO_INTERFACE;
    },

    canUnload: function(compMgr)
    {
        return true;
    }
};

function NSGetModule(compMgr, fileSpec)
{
    return JournalHomeModule;
}


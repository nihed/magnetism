const CLASS_ID = Components.ID("{38fbce87-8d59-40ad-84e7-9f0c0b3490b5}");
const CLASS_NAME = "Journal Home Service";
const CONTRACT_ID = "@redhat.com/journalhome;1";
const ABOUT_CONTRACT_ID = "@mozilla.org/network/protocol/about;1?what=journal";

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

  newChannel: function(uri) {
    var ioService = Components.classes["@mozilla.org/network/io-service;1"]
                              .getService(Components.interfaces.nsIIOService);
    var childURI = ioService.newURI("chrome://firefoxjournal/content/journal.html",
                                    null, null);
    var channel = ioService.newChannelFromURI(childURI);
    channel.originalURI = uri;
    return channel;
  },

  getURIFlags: function(uri) {
    return Components.interfaces.nsIAboutModule.ALLOW_SCRIPT;
  },

  QueryInterface: function(iid) {
    if (!iid.equals(Components.interfaces.nsIJournalHome) && !iid.equals(Components.interfaces.nsISupports) && !iid.equals(Components.interfaces.nsIAboutModule))
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
        compMgr.registerFactoryLocation(CLASS_ID, CLASS_NAME, ABOUT_CONTRACT_ID, fileSpec, location, type);
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


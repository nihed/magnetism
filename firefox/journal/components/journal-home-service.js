const CLASS_ID = Components.ID("{38fbce87-8d59-40ad-84e7-9f0c0b3490b5}");
const CLASS_NAME = "Journal Home Service";
const CONTRACT_ID = "@redhat.com/journalhome;1";

var jhs;


/***** History wrapper API *****/

var HistoryItem = Class.create();
HistoryItem.prototype = {
  initialize: function(url, title, lastVisitDate, visitCount) {
    this.url = url;
    this.title = title;
    this.lastVisitDate = lastVisitDate;
    this.visitCount = visitCount;
  }
}
 
const RDF = Components.classes["@mozilla.org/rdf/rdf-service;1"].getService(Components.interfaces.nsIRDFService); 
const BOOKMARK_NAME = RDF.GetResource("http://home.netscape.com/NC-rdf#Name");
const BOOKMARK_DATE = RDF.GetResource("http://home.netscape.com/NC-rdf#Date");
const BOOKMARK_VISIT_COUNT = RDF.GetResource("http://home.netscape.com/NC-rdf#VisitCount");
const NC_HISTORY_ROOT = RDF.GetResource("NC:HistoryRoot");

function readRDFThingy(ds,res,prop,qi,def) {
  var val = ds.GetTarget(res, prop, true);
  if (val)
    return val.QueryInterface(qi).Value;
  else
    return def;
}

function readRDFString(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFLiteral,"")
}

function readRDFDate(ds,res,prop) {
  return new Date(readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFDate,null)/1000);
}

function readRDFInt(ds,res,prop) {
  return readRDFThingy(ds,res,prop,Components.interfaces.nsIRDFInt,-1);
}

var History = Class.create();
Object.extend(History.prototype, Enumerable);

Object.extend(History.prototype, {

  initialize: function() {
    this.history = Components.classes["@mozilla.org/browser/global-history;2"].getService(Components.interfaces.nsIRDFDataSource);
    this.cachedHistoryCount = -1;
    this.observers = [];
    this.timeout = null;

    this.historyConn = null;

    try {
      this.iStatement = this.getHistoryConn().createStatement("INSERT INTO history (url, title, tstamp, host, domain) VALUES (?1, ?2, ?3, ?4, ?5);");
      this.sStatement = this.getHistoryConn().createStatement("SELECT url, title, tstamp, count(url) as c FROM history GROUP BY url ORDER BY tstamp, c DESC;");
    } catch (e) { console.log("createStatement error: %s\n%s", e, this.historyConn.lastErrorString);}

    /* i wish AddObserver.bind(this) worked!!! */
    this.history.AddObserver(this);
  },
  getHistoryConn: function() {
    var historyConn = this.historyConn;
    if (historyConn == null) {
      try {
        var f = Components.classes["@mozilla.org/file/directory_service;1"].getService(Components.interfaces.nsIProperties).get("ProfD", Components.interfaces.nsIFile);
        f.append("journal.sqlite");
        var store = Components.classes["@mozilla.org/storage/service;1"].getService(Components.interfaces.mozIStorageService);
        this.historyConn = historyConn = store.openDatabase(f);
      } catch (e) { console.log("getHistoryConn error: " + e); }
    }
    return historyConn;
  },
  addURL: function(url,title) {
      console.log("adding url "+url + "\ntitle: " + title);
      try {
        this.iStatement.bindUTF8StringParameter(0, url);
        this.iStatement.bindUTF8StringParameter(1, title);
        var dNow = Date.now();
        this.iStatement.bindDoubleParameter(2, dNow);

        var pUrl = this.mozexParseUrl(url);
        this.iStatement.bindUTF8StringParameter(3, pUrl.host);
        this.iStatement.bindUTF8StringParameter(4, pUrl.domain);

        this.iStatement.execute();
        this.iStatement.reset();
      } catch (e) { console.log("error statement.execute: " + e); }
  },
  mozexParseUrl: function(url) {
    // adapted from - cvs :pserver:guest@mozdev.org:/cvs_mozex/src/package/content/mozex/
    var parser = Components.classes["@mozilla.org/network/url-parser;1?auth=maybe"].
        createInstance(Components.interfaces.nsIURLParser);
    try {
        var proto_pos = new Number();
        var proto_len = new Number();
        var auth_pos = new Number();
        var auth_len = new Number();
        var path_pos = new Number();
        var path_len = new Number();
        parser.parseURL(url, url.length, proto_pos, proto_len, auth_pos, auth_len, path_pos, path_len);

        var res = {
            proto: null, user: null, pass: null, fqdn: null, host: null, domain: null, port: null, path: null
        };

        /* protocol, path */
        res.proto = url.substr(proto_pos.value, proto_len.value);
        res.path = url.substr(path_pos.value, path_len.value);

        /* username, password, hostname, port */
        var user_pos = new Number();
        var user_len = new Number();
        var pass_pos = new Number();
        var pass_len = new Number();
        var host_pos = new Number();
        var host_len = new Number();
        var port = new Number();
        var auth = url.substr(auth_pos.value, auth_len.value);
        parser.parseAuthority(auth, auth.length, user_pos, user_len, pass_pos, pass_len, host_pos, host_len, port);
        res.user = auth.substr(user_pos.value, user_len.value);
        res.pass = auth.substr(pass_pos.value, pass_len.value);

        res.fqdn = auth.substr(host_pos.value, host_len.value);
        var dArray = res.fqdn.split(".");
        res.domain = dArray.splice(dArray.length - 2,2).join("."); /* take the last two elements of the array */
        res.host = dArray.join("."); /* splice modified our dArray to be everything but the last two elements */
        res.port = port.value;
        return res;
    }
    catch (e) {
        console.log("cannot parse URL: %s : %s'", url, e);
        return null;
    }
},

  onAssert: function(datasource, source, property, target) {
    if (source == NC_HISTORY_ROOT && property == RDF.GetResource("http://home.netscape.com/NC-rdf#child")) {
      /* A new page was added; it's URL is 'target.Value' or 'target.ValueUTF8' */
      try {
        var url = target.ValueUTF8;
        var title = readRDFString(this.history, target, BOOKMARK_NAME);
        this.addURL(url,title);
      } catch (e) { console.log("error in onAssert calling addURL: " + e); }
    }
  },
  onUnassert: function(datasource, source, property, target) {
    /* Page removed from history, etc. */
  },
  onChange: function(datasource, source, property, oldTarget, newTarget) {
    /* Visit date updated, etc. */
    try {
      var url = source.ValueUTF8;
      var title = readRDFString(this.history, source, BOOKMARK_NAME);
      this.addURL(url, title);
    } catch (e) { console.log("error in onChange calling addURL: " + e); }
  },
  onMove: function(datasource, oldsource, newsource, property, target) { },
  beginUpdateBatch: function(datasource) {  },
  endUpdateBatch: function(datasource) {  },
  _each: function(iterator) {

    while (this.sStatement.executeStep()) {
      var url = this.sStatement.getUTF8String(0);
      var title = this.sStatement.getUTF8String(1);
      var tstamp = this.sStatement.getDouble(2);
      var count = this.sStatement.getInt32(3);

      var histItem = new HistoryItem(url, title, new Date(tstamp), count);
      iterator(histItem);
    }
    this.sStatement.reset();
  },
  observeChange: function(observer) {
    this.observers.push(observer);
    if (!this.timeout) {
      var me = this;
      this.timeout = new PeriodicalExecuter(function(pe) { me.checkChanged(); }, 5);
    }
  },
  checkChanged: function(pe) {
    if (!this.observers) {
      pe.stop();  
      return;
    }
    if (this.cachedHistoryCount != this.history.count) {
      this.cachedHistoryCount = this.history.count;
      var me = this;
      this.observers.each(function (it) { it(me); });
    }
  }
});

function JournalHomeService()
{
    jhs = this;
}

JournalHomeService.prototype = {
  greeting: function() { 
    return "Hello World!"; 
  },
  getHistory: function() {
    
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


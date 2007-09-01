var domUtils = {
  createSpanText: function(text, className) {
    var span = document.createElement('span')
    if (className) span.className = className
    span.appendChild(document.createTextNode(text))
    return span;
  },
  createAText: function(text, href, className) {
    var a = document.createElement('a');
    if (className) a.setAttribute('class' , className);
    a.setAttribute('href', href);
    a.appendChild(document.createTextNode(text));
    return a;
  },
};

var formatUtils = {
  ellipsize: function(s, l) {
    var substr = s.substring(0, l);
    if (s.length > l) {
      substr += "...";
    }
    return substr;
  },
  pad: function(x) { return x < 10 ? "0" + x : "" + x },
  twelveHour:  function(x) { return (x > 12) ? (x % 12) : x },
  meridiem: function(x) { return (x > 12) ? "pm" : "am" },
  monthName: function(i) { return ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"][i]},
};

var dateTimeUtils = {
  getLocalDayOffset: function(date, tzoffset) {
    var tzoff = tzoffset || (new Date().getTimezoneOffset() * 60 * 1000);
    return Math.floor((date.getTime() - tzoff) / DAY_MS)  
  },
};

var urlUtils = {
// FIXME replace this stuff with the same rules Firefox uses internally
  domainSuffixes: [".com", ".org", ".net", ".uk", ".us", ".cn", ".fm"],
  prependHttp: function (text, def) {
    if (text.startsWith("http://") || text.startsWith("https://")) 
      return text;
    return "http://" + text;
  },
  parseWebLink: function(text) {
    for (var i = 0; i < this.domainSuffixes.length; i++) {
      var suffix = this.domainSuffixes[i];
      if (text.indexOf(suffix) > 0) {
        if (text.startsWith("http://") || text.startsWith("https://")) 
          return text;
        return "http://" + text;
      }   
    };
    if (text.startsWith("http://") || text.startsWith("https://")) 
      return text;
    return null;
  }
};


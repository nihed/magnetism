dh = {}
dh.core = {}
dh.util = {}
dh.util.dom = {}
dh.extensions = {}

dh.core.inherits = function(klass, superKlass) {
    klass.prototype = new superKlass();
    klass.prototype.constructor = klass;
    klass.superclass = superKlass.prototype;
}

dh.core.adaptExternalArray = function (arr) {
    // FIXME IE specific
    if (true)  {
        return new VBArray(arr).toArray()
    }
    return arr;
}

dh.core.getServerBaseUrl = function () {
    return window.external.application.GetServerBaseUrl()
}

// Parse query parameters, sucked from dh.util in server
dh.util.getParamsFromLocation = function() {
    var query = window.location.search.substring(1);
    var map = {};
    var params = query.split("&");
    for (var i = 0; i < params.length; i++) {
        var eqpos = params[i].indexOf('=')
        if (eqpos > 0) {
            var key = params[i].substring(0, eqpos);
            var val = params[i].substring(eqpos+1);
            map[key] = decodeURIComponent(val);
        }
    }
    return map;
}

dh.util.encodeQueryString = function (args) {
    var url = '?'
    var first = true;
    for (arg in args) {
        if (!first) {
            url = url + '&'
        } else { 
            first = false;
        }
        url = url + arg + '=' + encodeURIComponent(args[arg])
    }
    return url
}

dh.util.debug = function (msg) {
    window.external.DebugLog("javascript: " + msg)
}

// Takes a DOM event handler function and fixes
// the IE event object to have the standard DOM 2 functions etc.
// Also wraps it in a try/catch for debugging purposes.
dh.util.dom.stdEventHandler = function(f) {
    return function(e) {
        try {
            if (!e) e = window.event;
            if (!e.stopPropagation) {
                e.stopPropagation = function() { e.cancelBubble = true; }
            }
            e.returnValue = f(e);
            return e.returnValue;
        } catch (e) {
            dh.util.debug("exception in event handler: " + e.message);
            return false;
        }
    }
}

dh.util.dom.appendSpanText = function (elt, text, styleClass) {
    var span = document.createElement("span")
    span.setAttribute("className", styleClass)
    span.appendChild(document.createTextNode(text))
    elt.appendChild(span)   
}

dh.util.dom.joinSpannedText = function (elt, arr, styleClass, sep) {
    for (var i = 0; i < arr.length; i++) {
        dh.util.dom.appendSpanText(elt, arr[i], styleClass)
        if (i < arr.length - 1) {
            elt.appendChild(document.createTextNode(sep))
        }
    }   
}

dh.util.dom.createHrefImg = function (src, target) {
    var a = document.createElement("a")
    a.setAttribute("href", target)
    var img = document.createElement("img")
    a.appendChild(img)
    img.setAttribute("src", src)
    return a;
}

dh.util.dom.clearNode = function (elt) {
    while (elt.firstChild) { elt.removeChild(elt.firstChild); }
}

dh.util.dom.replaceContents = function (elt, child) {
    dh.util.dom.clearNode(elt)
    elt.appendChild(child)
}

dh.util.dom.getClearedElementById = function (id) {
    var elt = document.getElementById(id)
    if (elt) 
        dh.util.dom.clearNode(elt)
    return elt
}

dh.util.dom.selectNode = function (doc, path) {
    return doc.selectSingleNode(path)
}

dh.util.dom.selectNodes = function (doc, path) {
    return doc.selectNodes(path)
}

dh.util.prependCssClass = function (node, className) {
    if (node.className)
        node.className = className + " " + node.className
    else
        node.className = className
}

dh.util.swapLastCssClass = function (node, classPrefix, classSuffix) {
    var klasses = node.className.split(" ")
    if (klasses.length > 0 && klasses[klasses.length-1].indexOf(classPrefix) == 0) {
        klasses.pop()
    }
    klasses.push(classPrefix + classSuffix)
    node.className = klasses.join(" ")
}

dh.util.fillAlphaPng = function(image) {
    var span = image.parentNode
    var src = image.src
    span.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale');"
    span.style.background = "transparent";
}

dh.util.createPngElement = function(url, width, height) {
    // 5.5 <= IE < 7 specific
    var span = document.createElement("span")
      
    span.style.background = "#bbbbbb"
    span.style.width = width
    span.style.height = height
    
    var image = document.createElement("img")
    image.style.visibility = "hidden"
    span.appendChild(image)
    
    image.onload = function() { dh.util.fillAlphaPng(image) }
    image.src = url

    return span
}


dh.getXmlHttp = function () {
    var xmlhttp = false
    try {
        xmlhttp = window.external.getXmlHttp()
        dh.util.debug("instantiated WINHTTP")
    } catch (e) {
    try {
        xmlhttp = new ActiveXObject("Msxml2.XMLHTTP")
        dh.util.debug("instantiated Msxml2.XMLHTTP")        
    } catch (e) {
        try {
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP")
            dh.util.debug("instantiated Microsoft.XMLHTTP")              
        } catch (E) {
            xmlhttp = false
        }
    }
    }
    if (!xmlhttp && typeof XMLHttpRequest != 'undefined') {
        xmlhttp = new XMLHttpRequest()
    }
    return xmlhttp
}

// This function is adapted from the jsolait library
dh.parseXML = function(xml){
    var obj=null;
    var isMoz=false;
    var isIE=false;
        
    try {//to get the mozilla parser
        obj = new DOMParser();
        isMoz=true;
    }catch(e){
        try{//to get the MS XML parser
            obj = new ActiveXObject("Msxml2.DomDocument.4.0"); 
            obj.setProperty("SelectionLanguage", "XPath")            
            isIE=true;
        }catch(e){
            try{//to get the MS XML parser
                obj = new ActiveXObject("Msxml2.DomDocument"); 
                isIE=true;
            }catch(e){
                throw new Error("Couldn't get XML parser")
            }
        }
    }
    if(isMoz){
        obj = obj.parseFromString(xml, "text/xml");
    }else if(isIE){
        obj.loadXML(xml);
    }
    return obj;    
}

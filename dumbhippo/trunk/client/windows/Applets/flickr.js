dh.notification.extension.Flickr = function () {
    this._singlePhotoURL = /flickr\.com\/photos\/([A-Za-z0-9_]+)\/([0-9]+)\//
    this._flickrRESTEndpoint = 'http://www.flickr.com/services/rest/'
    
    this._authKey = '0e96a6f88118ed4d866a0651e45383c1'

    this._parseRestResponse = function (respDoc) {
        var top = respDoc.documentElement
        if (top.nodeName != 'rsp') {
            throw new Error('malformed REST response, document element was not \"rsp\"')
        }
        var stat = top.getAttribute("stat")
        if (stat != "ok") {
            for (var i = 0; i < top.childNodes.length; i++) {
                var node = top.childNodes[i]
                if (node.nodeName == 'err') {
                    var msg = node.getAttribute('msg')
                    if (!msg)
                        msg = "(no error text given)";
                    var code = node.getAttribute('code')
                    if (!code)
                        code = "(no code given)"
                    throw new Error("Got error in REST invocation, code=" + code + ", msg=\"" + msg + "\"");
                }
            }
            throw new Error('Got malformed error in REST invocation')
        }
        return top.childNodes
    }

    this._flickrInvoke = function (methodName, args, callback) {
        var modifiedArgs = {}
        for (arg in args) {
            modifiedArgs[arg] = args[arg]
        }
        modifiedArgs.method = methodName
        modifiedArgs.api_key = this._authKey
        var url = this._flickrRESTEndpoint + dh.util.encodeQueryString(modifiedArgs)
        dh.util.debug("using flickr url " + url)        
        var flickr = this
        var xmlhttp = dh.getXmlHttp()
        dh.util.debug("got xmlhttp " + xmlhttp)
        xmlhttp.open('POST', url, true)
        dh.util.debug("setting onreadystatechange")        
        xmlhttp.onreadystatechange=function() {
            dh.util.debug("got flickr onreadystatechange, readyState=" + xmlhttp.readyState) 
            if (xmlhttp.readyState==4) {
                try {
                    callback(flickr._parseRestResponse(xmlhttp.responseXML))
                } catch (e) {
                    dh.util.debug("error in xmlhttp callback: " + e.message);
                }
             }
         }
        xmlhttp.send(null)
        dh.util.debug("sent async POST")        
    }

    this.accept = function(post) {
        return post.linkURL && post.linkURL.match(this._singlePhotoURL) != null;
    }
    
    this._flickrGetSizesHandler = function (resp, postContentNode) {
        dh.util.debug("got response for flickr.photos.getSizes" + resp)
        for (var i = 0; i < resp.length; i++) {
                var child = resp[i]
                dh.util.debug("examining child " + child)
                if (child.nodeType != 1)
                    continue
                if (child.nodeName != 'sizes')
                    throw new Error('Invalid size response, expected "sizes"')
                for (var subChild = child.firstChild; subChild; subChild = subChild.nextSibling) {
                    var label = subChild.getAttribute("label")
                    dh.util.debug("examining response label " + label)
                    if (label == 'Thumbnail') { // TODO use different sizes too
                        var src = subChild.getAttribute("source")
                        var url = subChild.getAttribute("url")
                        var img = document.createElement("img")
                        img.setAttribute("src", src)
                        postContentNode.appendChild(img)
                    }
                }
            }    
    }

    this.drawContent = function(post, postContentNode) {
        var match = post.linkURL.match(this._singlePhotoURL);
        try {
            var photoId = match[2]        
            dh.util.debug("being async invoke of flickr.photos.getSizes for photoId" + photoId)
            var flickr = this
            var contentNode = postContentNode            
            this._flickrInvoke("flickr.photos.getSizes", {photo_id: photoId}, function (resp) { flickr._flickrGetSizesHandler(resp, contentNode) })
        } catch (e) {
            dh.util.debug('Flickr error: ' + e.message)
        }
    }
}

dh.notification.extensions.flickr = new dh.notification.extension.Flickr()
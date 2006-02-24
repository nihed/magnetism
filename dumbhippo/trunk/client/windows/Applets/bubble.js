// bubble.js: Shared handling of bubbles that display notifications to the
//   user of posts, MySpace comments ,and so forth
// Copyright Red Hat, Inc. 2006

dh.bubble = {}

//////////////////////////////////////////////////////////////////////////////
// Global cache from personId => name
//////////////////////////////////////////////////////////////////////////////

dh.bubble._nameCache = {}
    
dh.bubble.addPersonName = function (personId, name) {
    dh.bubble._nameCache[personId] = name
}

dh.bubble.getPersonName = function (personId) {
    var ret = this._nameCache[personId]
    if (!ret)
        ret = "(unknown)"
    return ret;
}    

//////////////////////////////////////////////////////////////////////////////
// Generic display code for a single notification bubble
//////////////////////////////////////////////////////////////////////////////
    
dh.bubble.BASE_WIDTH = 400
dh.bubble.BASE_HEIGHT = 150
    
// Create a new bubble object    
// @param includeNavigation whether the navigation arrows and close button 
//        should be included in the result
dh.bubble.Bubble = function(includeNavigation) {
    // Whether to include the quit button and previous/next arrows
    this._includeNavigation = includeNavigation
    
    // The notification currently being displayed
    this._data = null
    
    ///////////// Callbacks /////////////
    
    // After creating the Bubble object, the caller can assign to these fields
    // to override the default empty handling with real callbacks
    
    // Called when the close button is clicked
    this.onClose = function() {}
    
    // Called when the 'next' arrow is clicked
    this.onNext = function() {}
    
    // Called when the 'previous' arrow is clicked
    this.onPrevious = function() {}
    
    // Called when the user clicks on a DumbHippo post link
    // @param postId the ID of the post that was clicked on
    // @param url the URL of the link that was shared with the post (this is not
    //      the URL we take the user to; we redirect them through our
    //      site instead.)
    this.onLinkClick = function(postId, url) {}
    
    // Called when the size of the bubble changes
    this.onSizeChange = function() {}
    
    // Build the DOM tree for the bubble
    // @return the DOM node of the top node for the bubble
    this.create = function() {
        function append(parent, elementName, className) {
            var element = document.createElement(elementName)
            element.setAttribute("className", className)
            parent.appendChild(element)
            
            return element
        }
        function appendDiv(parent, className) {
            return append(parent, "div", className)
        }
    
        var bubble = this  // for callback closures

        this._topDiv = document.createElement("div") 
        this._topDiv.setAttribute("className", "dh-notification-top")
            var leftSide = appendDiv(this._topDiv, "dh-notification-leftside")
                this._photoDiv = appendDiv(leftSide, "dh-notification-photo-div")
                this._photoLinkDiv = appendDiv(leftSide, "dh-notification-photolink")
                
                if (this._includeNavigation) {
                    var navDiv = appendDiv(leftSide, "dh-notification-navigation")
                        this._navText = appendDiv(navDiv, "dh-notification-navigation-text")
                        this._navButtons = appendDiv(navDiv, "dh-notification-navigation-buttons")
                }
            this._rightsideDiv = appendDiv(this._topDiv, "dh-notification-rightside")
                this._titleDiv = appendDiv(this._rightsideDiv, "dh-notification-title")
                this._bodyDiv = appendDiv(this._rightsideDiv, "dh-notification-body")
                this._bottomrightDiv = appendDiv(this._rightsideDiv, "dh-notification-bottomright")
                    this._viewersOuterDiv = appendDiv(this._bottomrightDiv, "dh-notification-viewers-outer")
                        var viewersBubbleDiv = appendDiv(this._viewersOuterDiv, "dh-notification-viewers-bubble")
                            this._viewersPhotoDiv = appendDiv(viewersBubbleDiv, "dh-notification-viewers-photo")
                            this._viewersSpan = append(viewersBubbleDiv, "span", "dh-notification-viewers")
                    var metaOuterDiv = appendDiv(this._bottomrightDiv, "dh-notification-viewers-outer")
                        this._metaSpan = append(metaOuterDiv, "span", "dh-notification-meta")
        
            if (this._includeNavigation) {
                this._closeButton = append(this._topDiv, "img", "dh-close-button")
                this._closeButton.setAttribute("src", dh.appletUrl + "close.png")
                this._closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
                    bubble.onClose();
                    e.stopPropagation();
                    return false;
                })
            }
        
        return this._topDiv
    }
    
    // Set the notification that this bubble is currently displaying. 
    // @param data object, such as a dh.bubble.PostData or dh.bubble.MySpaceData
    //    While there isn't actually a base class, all data objects must export
    //    a set of methods forming a common interface.
    this.setData = function(data) {
        this._data = data
        this._render()
    }
    
    // Update the navigation arrows
    // @param position position of current notification in a list of notifications (first is 0)
    // @param numNotifications total number of notifications    
    this.updateNavigation = function(position, numNotifications) {
        if (!this._includeNavigation)
            return
    
        var bubble = this // for callback closures
        
        dh.util.dom.replaceContents(this._navText, document.createTextNode((position + 1) + " of " + numNotifications))
        
        dh.util.dom.clearNode(this._navButtons)
        var button = document.createElement("button")
        button.className = "dh-notification-navigation-button"
        button.value = "&lt;"
        button.disabled = position == 0
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onPrevious()
            return false;
        })
        
        button = document.createElement("button")
        button.className = "dh-notification-navigation-button"
        button.value = "&gt;"
        button.disabled = position == numNotifications - 1
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onNext()
            return false;
        })
    }
    
    // Helper function for data object display routines; create a link to a DumbHippo post
    this.createSharedLinkLink = function(linkTitle, postId, url) {
        var a = document.createElement("a")
        a.setAttribute("href", "javascript:true")
        
        var bubble = this
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            bubble.onLinkClick(postId, url)
            return false;
        })
        dh.util.dom.appendSpanText(a, linkTitle, "dh-notification-link-title")
        return a
    }

    // Helper function for data object display routines; render a list of recipients
    // @param node parent node
    // @param arr array of recipients
    // @param normalCssClass CSS class to use for display of a different user
    // @param cssCssClass CSS class to use for display of the current user
    this.renderRecipients = function (node, arr, normalCssClass, selfCssClass) {
        for (var i = 0; i < arr.length; i++) {
            var recipientNode = this._renderRecipient(arr[i], normalCssClass, selfCssClass)
            node.appendChild(recipientNode)
            if (i < arr.length - 1) {
                node.appendChild(document.createTextNode(", "))
            }
        }  
    }
    
    // Get the width of the bubble's desired area
    // @return the desired width, in pixels
    this.getWidth = function() {
        return dh.bubble.BASE_WIDTH
    }

    // Get the height of the bubble's desired area
    // @return the desired width, in pixels
    this.getHeight = function() {
        var height = dh.bubble.BASE_HEIGHT
        if (this._showViewers)
            height += this._viewersOuterDiv.offsetHeight
            
        return height
    }

    // Render a single recipient
    this._renderRecipient = function (recipient, normalCssClass, selfCssClass) {
        var id = recipient.id
        var name = recipient.name
        dh.util.debug("rendering recipient with name=" + name)
        var cssClass = normalCssClass;
        if (id == dh.selfId) {
            name = "you"
            cssClass = selfCssClass
        }
        var node = document.createElement("span")
        node.setAttribute("className", cssClass)
        node.appendChild(document.createTextNode(name))       
        return node
    }
     
    // Update the image for the photo on the left of the bubble
    this._setPhotoImage = function (src, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photo")
        var img = document.createElement("div")
        img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale')"
        img.setAttribute("className", "dh-notification-photo")
        a.appendChild(img)
        dh.util.dom.replaceContents(this._photoDiv, a)
    }
    
    // Update the title underneath the photo on the left
    this._setPhotoTitle = function (text, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photolink")
        a.appendChild(document.createTextNode(text))
        dh.util.dom.replaceContents(this._photoLinkDiv, a)
    }
    
    // Update the contents of the bubble to match this._data    
    this._render = function() {
        this._setPhotoImage(this._data.getPhotoSrc(), this._data.getPhotoLink())
        this._setPhotoTitle(this._data.getPhotoTitle(), this._data.getPhotoLink())
        
        dh.util.dom.clearNode(this._titleDiv)
        this._data.appendTitleContent(this, this._titleDiv)

        dh.util.dom.clearNode(this._bodyDiv)
        this._data.appendBodyContent(this, this._bodyDiv)
        
        dh.util.dom.clearNode(this._metaSpan)
        this._data.appendMetaContent(this, this._metaSpan)

        dh.util.dom.clearNode(this._viewersSpan)
        this._data.appendViewersContent(this, this._viewersSpan)
        this._setShowViewers(this._viewersSpan.firstChild != null)

        var viewersPhotoSrc = this._data.getViewersPhotoSrc()
        if (viewersPhotoSrc != null) {
            alert("Viewer photo not currently implemented")
        } else {
            this._viewersPhotoDiv.style.display = "None"
        }
        
        this._fixupLayout()
    }
    
    // Adjust various sizes that we can't make the CSS handle
    this._fixupLayout = function() {
        if (this._includeNavigation)
            this._titleDiv.style.width = (this._rightsideDiv.clientWidth - this._closeButton.offsetWidth) + "px"
        
        // Now set the height of the body element to be fixed to the remaining space
        var desiredHeight = this.getHeight() - this._titleDiv.offsetHeight - this._bottomrightDiv.offsetHeight
        //alert(this._showViewers + " " + this.getHeight() + " " + this._bottomrightDiv.offsetHeight + " " + desiredHeight)
        //alert(this._titleDiv.offsetHeight)
        
        // Hack - we don't want partial lines to be shown, so compute how many 
        // full lines fit. We do this by knowing that titleDiv is one line high
        // this will break if the title is changed to a different font, etc.
        var lineHeight = this._titleDiv.clientHeight
        if (lineHeight > 0)
            desiredHeight = Math.floor(desiredHeight / lineHeight) * lineHeight
            
        this._bodyDiv.style.height = desiredHeight + "px"
    }
    
    // Set whether the viewer bubble is currently showing
    this._setShowViewers = function(showViewers) {
        showViewers = !!showViewers
        if (this._showViewers != showViewers) {
            this._showViewers = showViewers
            
            if (showViewers) {
                this._viewersOuterDiv.style.display = "block"
            } else {
                this._viewersOuterDiv.style.display = "none"
            }
            
            this.onSizeChange()
        }
    }   
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for DumbHippo posts
//////////////////////////////////////////////////////////////////////////////
    
// Extension point for specific post types
dh.bubble.postExtensions = {}
    
dh.bubble.PostData = function(senderName, senderId, senderPhotoUrl, postId, linkTitle, 
                              linkURL, linkDescription, personRecipients, groupRecipients, 
                              viewers, postInfo) {
    this.senderId = senderId
    this.senderPhotoUrl = senderPhotoUrl
    this.postId = postId
    this.linkTitle = linkTitle
    this.linkURL = linkURL
    this.linkDescription = linkDescription
    this.personRecipients = personRecipients
    this.groupRecipients = groupRecipients
    this.viewers = viewers
    if (postInfo != null && !postInfo.match(/^\s*$/)) {
        this.info = dh.parseXML(postInfo)
    } else
        this.info = null

    this.getPhotoLink = function() {
        return dh.serverUrl + "person?who=" + this["senderId"]
    }
    
    this.getPhotoSrc = function() {
        return dh.serverUrl + this.senderPhotoUrl
    }
    
    this.getPhotoTitle = function() {
        return dh.bubble.getPersonName(this.senderId)
    }
    
    this.appendTitleContent = function(bubble, parent) {
        parent.appendChild(bubble.createSharedLinkLink(this.linkTitle, this.postId, this.linkURL))
    }
        
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.linkDescription))
        
        for (extension in dh.bubble.postExtensions) {
            var ext = dh.bubble.postExtensions[extension]
            if (ext.accept(this))
                ext.drawContent(this, bubble._bodyDiv)
        }
    }
    
    this.appendMetaContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode("This was sent to "))
        var personRecipients = this.personRecipients
        var groupRecipients = this.groupRecipients
        // FIXME this is all hostile to i18n
        bubble.renderRecipients(parent, personRecipients, "dh-notification-recipient", "dh-notification-self-recipient")
        if (personRecipients.length > 0 && groupRecipients.length > 0) {
            parent.appendChild(document.createTextNode(" and "))
        }
        if (groupRecipients.length > 1) {
            parent.appendChild(document.createTextNode("the groups "))            
            dh.util.dom.joinSpannedText(parent, groupRecipients, "dh-notification-group-recipient", ", ")
        } else if (groupRecipients.length == 1) {
            parent.appendChild(document.createTextNode("the "))
            dh.util.dom.appendSpanText(parent, groupRecipients[0], "dh-notification-group-recipient")
            parent.appendChild(document.createTextNode(" group"))
        }
    }
            
    this.appendViewersContent = function(bubble, parent) {
        var viewers = this.viewers
        if (viewers.length > 0) {
            dh.util.dom.appendSpanText(parent, "Viewed by: ", "dh-notification-viewers-label")
            bubble.renderRecipients(parent, viewers, "dh-notification-viewer", "dh-notification-self-viewer")
        }
    }
    
    this.getViewersPhotoSrc = function() {
        // Need to pass in the viewer ID as well as name to here to display
        return null
    }
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for MySpace blog comments
//////////////////////////////////////////////////////////////////////////////
    
// Create a new MySpace blog comment object
// @param myId ID of the current user (the user owning the blog)
// @param blogId ID of the blog *post* that was commented on
// @param commentId ID of the new comment on the post
// @param posterName Name of the comment poster
// @param posterImgUrl URL to a photo of the comment poster
// @param content the content of the post
dh.bubble.MySpaceData = function(myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    this.myId = myId,
    this.blogId = blogId
    this.commentId = commentId
    this.posterId = posterId
    this.posterName = posterName
    this.posterImgUrl = posterImgUrl
    this.content = content
    
    this.getPhotoLink = function() {
        return "http://myspace.com/" + this.posterId
    }
    
    this.getPhotoSrc = function() {
        return this.posterImgUrl
    }
    
    this.getPhotoTitle = function() {
        return this.posterName
    }

    this.appendTitleContent = function(bubble, parent) {
        var a = document.createElement("a")
        a.appendChild(document.createTextNode("New MySpace comment from " + this.posterName))
        a.setAttribute("href", "http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=" + this.myId + "&blogID=" + this.blogId)
        
        parent.appendChild(a)
    }
            
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.content))
    }
    
    this.appendMetaContent = function(bubble, parent) {
    }
    
    this.appendViewersContent = function(bubble, parent) {
    }
    
    this.getViewersPhotoSrc = function() {
        return null
    }
}

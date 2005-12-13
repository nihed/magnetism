dojo.require("dh.sharelink");
dojo.provide("dh.sharephotoset");
dojo.provide("dh.flickrupload");

dh.flickrupload.Photo = function(filename, thumbnailPath) {
	this.filename = filename
	// The locally cached version of the image on disk
	this.thumbnailPath = thumbnailPath
	// The URL for it on our server
	this.thumbnailUrl = null
	this.state = 'queued'
	this.flickrId = null
	
	this.render = function() {
		this.div = document.createElement("div")	
		this.img = document.createElement("img")
		this.img.setAttribute("src", thumbnailPath)
		this.div.appendChild(this.img)
		this.textSpan = document.createElement("span")
		this.div.appendChild(this.textSpan)
		var text;
		if (this.state == 'queued')
			text = "Waiting to upload"
		else if (this.state == 'uploading')
			text = "Uploading, " + this.progress + "% complete"
		else if (this.state == 'uploaded')
			text = "Processing..."
		else if (this.state == 'pending') // All upload is done, just need to add to photoset
			text = "Complete"
		else if (this.state == 'complete')
			text = "Complete"
		else
			text = "(error)"
		this.textSpan.appendChild(document.createTextNode(text))
		return this.div
	}

	this.isPending = function () {
		return this.state == 'pending'
	}
	
	this.isComplete = function () {
		return this.state == 'complete'
	}

	this.getId = function() {
		return this.filename
	}
	
	this.getFlickrId = function () {
		return this.flickrId
	}
	
	this.uploadProgress = function (progress) {
		this.state = 'uploading'
		this.progress = progress
	}
	
	this.uploadComplete = function (flickrId) {
		this.state = 'uploaded'
		this.flickrId = flickrId
	}
	
	this.setComplete = function () {
		if (this.state != 'pending')
			throw new Error("got setComplete in invalid state " + this.state)
		this.state = 'complete'
	}
	
	this.setThumbnailUrl = function (thumbnailUrl) {
		this.state = 'pending'
		this.thumbnailUrl = thumbnailUrl
	}
	
	this.getThumbnailUrl = function () {
		return this.thumbnailUrl
	}
}
dojo.inherits(dh.flickrupload.Photo, Object);

dh.flickrupload.PhotoContainer = function () {
	dojo.debug("instantiating PhotoContainer")
	this.photos = []
	this.userId = null
	
	this.setUserId = function (userId) {
		this.userId = userId
	}
	
	this.getUserId = function () {
		return this.userId
	}
	
	this.uploadProgress = function (photoId, progress) {
		this.invokeAndRedraw(photoId, function (photo) { photo.uploadProgress(progress) })
	}
	
	this.uploadComplete = function (photoId, flickrPhotoId) {
		this.invokeAndRedraw(photoId, function (photo) { photo.uploadComplete(flickrPhotoId) })
	}
	
	this.thumbnailUploadComplete = function (photoId, thumbnailUrl) {
		this.invokeAndRedraw(photoId, function (photo) { photo.setThumbnailUrl(thumbnailUrl) })
	}	
	
	this.photoComplete = function (photoId) {
		this.invokeAndRedraw(photoId, function (photo) { photo.setComplete(); })
	}

	this.findPhotoIndexForId = function (id) {
		var i;
		for (i = 0; i < this.photos.length; i++) {
			if (this.photos[i].getId() == id) {
				return i;
			}
		}
		return null
	}
	
	this.getPhotos = function() {
		return this.photos
	}
}

dh.flickrupload.UploadStatus = function(photos, userId, photoTitle, descriptionHtml) {
	dojo.debug("creating UploadStatus")
	dh.flickrupload.PhotoContainer.call(this)
	this.photos = photos
	this.userId = userId
	this.photoTitle = photoTitle
	this.tagName = null
	this.descriptionHtml = descriptionHtml
	this.div = document.createElement("div")
	this.title = document.createElement("div")
	this.div.appendChild(this.title)
	this.photoDisplay = document.createElement("div")
	this.div.appendChild(this.photoDisplay)
	dojo.debug("done creating UploadStatus")
		
	this.setTagName = function (tagName) {
		this.tagName = tagName
	}
		
	this.analyzeState = function () {
		this.state = 'complete'
		for (var i = 0; i < this.photos.length; i++) {
			var photo = this.photos[i]
			if (!(photo.isComplete())) {
				this.state = 'uploading'
				break
			}
		}
		dojo.debug("current status state=" + this.state)		
	}
	
	this.render = function () {
		this.analyzeState()
		if (this.state == 'uploading') {
			this.container = document.getElementById("dhMain")
			dh.util.clearNode(this.container)
			this.container.appendChild(this.div)
		}
		this.redrawOrStartShare()
	}
	
	this.invokeAndRedraw = function (photoId, cb) {
		var i = this.findPhotoIndexForId(photoId)
		var photo = this.photos[i]
		cb(photo)
		this.redrawOrStartShare()
	}
	
	this.redrawOrStartShare = function () {
		this.analyzeState()
		if (this.state == 'uploading') {
			this.redraw()
		} else {
			this.startShare()
		}
	}
	
	this.redraw = function () {
		dojo.debug("redrawing status")	
		if (this.state == 'uploading') {
			this.setTitle("Photo Upload Progress")
		} else {
			this.setTitle("Photo Upload Complete")
		}
		var i;
		var displayedPhotos = [];
		for (i = 0; i < photos.length; i++) {
			// Try to find the currently uploading photo
			if (!(photos[i].isComplete())) {
				if (i > 0) {
					displayedPhotos.push(photos[i - 1])
				}
				displayedPhotos.push(photos[i])
				if (i+1 < photos.length) {
					displayedPhotos.push(photos[i + 1])
				}
				break
			}
		}
		if (displayedPhotos.length == 0) {
			for (i = photos.length - 1; i >= 0 && displayedPhotos.length < 3; i++) {
				displayedPhotos.unshift(photos[i])
			}
		}
		for (i = 0; i < displayedPhotos.length; i++) {
			dh.util.clearNode(this.photoDisplay)
			dojo.debug("displaying photoId=", displayedPhotos[i].getId())				
			this.photoDisplay.appendChild(displayedPhotos[i].render())
		}
	}
	
	this.setTitle = function (text) {
		dh.util.clearNode(this.title)
		this.title.appendChild(document.createTextNode(text))
	}
	
	this.startShare = function () {
		dojo.debug("starting link share");
	
		var url = "http://www.flickr.com/photos/" + this.userId + "/tags/" + this.tagName
	
		var commaRecipients = dh.util.join(dh.share.selectedRecipients, ",", "id");

		var postInfoDoc = dh.sharelink.postInfo
		var flickrElt = postInfoDoc.createElement("flickr")
		var photosetElt = postInfoDoc.createElement("photos")
		flickrElt.appendChild(photosetElt)
		postInfoDoc.documentElement.appendChild(flickrElt)
		for (var i = 0; i < this.photos.length; i++) {
			var photo = this.photos[i]
			var photoElt = postInfoDoc.createElement("photo")
			photosetElt.appendChild(photoElt)
			var photoUrlElt = postInfoDoc.createElement("photoUrl")
			photoElt.appendChild(photoUrlElt)
			photoUrlElt.appendChild(postInfoDoc.createTextNode(photo.getThumbnailUrl()))			
			var photoIdElt = postInfoDoc.createElement("photoId")
			photoElt.appendChild(photoIdElt)			
			photoIdElt.appendChild(postInfoDoc.createTextNode(photo.getFlickrId()))
		}

		var postInfoXml = dojo.dom.toText(dh.sharelink.postInfo);

		dojo.debug("url = " + url);
		dojo.debug("title = " + this.photoTitle);
		dojo.debug("desc = " + descriptionHtml);
		dojo.debug("rcpts = " + commaRecipients);
		dojo.debug("postInfo = " + postInfoXml);
	
		dh.server.doPOST("sharelink",
						{ 
							"url" : url,
							"title" : this.photoTitle, 
						  	"description" : this.descriptionHtml,
						  	"recipients" : commaRecipients,
						  	"secret" : "false",
							"postInfoXml" : postInfoXml
						},
						function(type, data, http) {
							dojo.debug("sharelink got back data " + dhAllPropsAsString(data));
							dh.util.goToNextPage("home", "You've been shared!");
						},
						function(type, error, http) {
							dojo.debug("sharelink got back error " + dhAllPropsAsString(error));

						});
	}
}
dojo.inherits(dh.flickrupload.UploadStatus, dh.flickrupload.PhotoContainer);

dh.flickrupload.UploadSet = function() {
	dh.flickrupload.PhotoContainer.call(this);
	this.currentPhoto = null
	this.div = document.createElement("div")
	this.photoDiv = document.createElement("div")
	this.div.appendChild(this.photoDiv)
	this.statusText = document.createElement("span")
	this.div.appendChild(this.statusText)
	this.flickrUrl = null
	
	this.createNavigationLink = function (text, cb) {
		var a = document.createElement("a")
		a.appendChild(document.createTextNode(text))
		a.style.margin = "1em"
		a.setAttribute("href", "javascript:")
		a.onclick = cb
		return a
	}
	var uploadset = this
	this.next = this.createNavigationLink("Next", function() { uploadset.doNext(); })
	this.prev = this.createNavigationLink("Previous", function() { uploadset.doPrev(); })
	this.div.appendChild(document.createElement("br"))
	this.div.appendChild(this.prev)	
	this.div.appendChild(this.next)
	
	this.render = function () {
		var parent = document.getElementById("dhFlickrPhotoUpload")
		parent.appendChild(this.div)
	}
	
	// invoked by PhotoContainer
	this.invokeAndRedraw = function (photoId, cb) {
		var i = this.findPhotoIndexForId(photoId)
		var photo = this.photos[i]
		cb(photo)
		if (this.currentPhoto == i)
			this.setPhoto(this.currentPhoto) // redraws	
	}	
	
	this.getUrl = function () {
		return this.flickrUrl
	}

	this.setFlickrUrl = function (url) {
		this.flickrUrl = url
	}
	
	this.addPhoto = function (filename, thumbnailPath) {
		var photo = new dh.flickrupload.Photo(filename, thumbnailPath)
		dojo.debug("constructed photo " + photo + " thumbnail: " + thumbnailPath)
		this.photos.push(photo)
		if (this.currentPhoto == null) {
			this.setPhoto(0)
		} else {
			this.redrawStatusText()		
		}
	}

	this.doNext = function() {
		this.setPhoto(this.currentPhoto + 1)
	}
	
	this.doPrev = function() {
		this.setPhoto(this.currentPhoto - 1)
	}

	this.setPhoto = function(i) {
		if (i < 0 || i >= this.photos.length) {
			dojo.debug("invalid photo index " + i)			
			return
		}
		dojo.debug("displaying photo index " + i)	
		var photo = this.photos[i]
		this.currentPhoto = i
		dh.util.clearNode(this.photoDiv)
		this.photoDiv.appendChild(photo.render())
		this.redrawStatusText()		
	}

	this.redrawStatusText = function () {
		dh.util.clearNode(this.statusText)
		var i = this.currentPhoto
		if (i == null) {
			i = 0;
		}
		this.statusText.appendChild(document.createTextNode("" + (i+1) + " of " + this.photos.length))
	}
}
dojo.inherits(dh.flickrupload.UploadSet, dh.flickrupload.PhotoContainer);

dojo.debug("creating photoset instance"); 
dh.sharephotoset.instance = new dh.flickrupload.UploadSet()

dh.sharephotoset.submitButtonClicked = function() {
	try {
	dojo.debug("clicked share photo button");
	var title = dh.sharelink.urlTitleToShareEditBox.textValue
	dojo.debug("title = " + title)
	var descriptionHtml = dh.util.getTextFromRichText(dh.share.descriptionRichText)
	var currentPhotos = dh.sharephotoset.instance.getPhotos()
	var userId = dh.sharephotoset.instance.getUserId()
	dojo.debug("userid = " + userId)	
	dh.sharephotoset.instance = new dh.flickrupload.UploadStatus(currentPhotos, userId, title, descriptionHtml)
	dojo.debug("rendering UploadStatus")	
	dh.sharephotoset.instance.render()
	window.external.application.CreatePhotoset(title)
	} catch (e) {
		dojo.debug("error in submitButtonClicked:" + e.message)
	}
}

dhFlickrSetUserId = function (userId) {
	try {
	dojo.debug("setting userId " + userId)
	dh.sharephotoset.instance.setUserId(userId);
	} catch (e) {
		dojo.debug("dhFlickrSetUserid failed:" + e.message)
	}		
}

dhFlickrSetTagName = function (tagName) {
	try {
	dojo.debug("setting tag name " + tagName)
	dh.sharephotoset.instance.setTagName(tagName);
	} catch (e) {
		dojo.debug("dhFlickrSetTagName failed:" + e.message)
	}	
}

// These functions are invoked from the client directly; they're
// globally namespaced in a hackish way because doing real javascript
// namespacing was too annoying to code in C++
dhFlickrAddPhoto = function (filename, thumbnailFilename) {
	try {
	dojo.debug("adding photo " + filename)
	dh.sharephotoset.instance.addPhoto(filename, thumbnailFilename)
	} catch (e) {
		dojo.debug("dhFlickrAddPhoto failed:" + e.message)
	}
}

dhFlickrPhotoUploadStarted = function (filename) {
	try {
	dojo.debug("got upload started for " + filename)
	dh.sharephotoset.instance.uploadProgress(filename, 69)
	} catch (e) {
		dojo.debug("dhFlickrPhotoUploadStarted failed:" + e.message)
	}	
}

dhFlickrPhotoUploadComplete = function (filename, photoId) {
	try {
	dojo.debug("upload complete for photo " + filename)
	dh.sharephotoset.instance.uploadComplete(filename, photoId)
	} catch (e) {
		dojo.debug("dhFlickrPhotoUploadComplete failed:" + e.message)
	}		
}

dhFlickrPhotoThumbnailUploadComplete = function (filename, thumbnailUrl) {
	try {
	dojo.debug("thumbnail upload complete for photo " + filename + " to " + thumbnailUrl)
	dh.sharephotoset.instance.thumbnailUploadComplete(filename, thumbnailUrl)
	} catch (e) {
		dojo.debug("dhFlickrPhotoThumbnailUploadComplete failed:" + e.message)
	}		
}

dhFlickrPhotoComplete = function (filename) {
	try {
	dojo.debug("got photo complete for " + filename)
	dh.sharephotoset.instance.photoComplete(filename)
	} catch (e) {
		dojo.debug("dhFlickrPhotoComplete failed:" + e.message)
	}	
}

dhFlickrError = function (text) {
	var display = document.getElementById("dhFlickrError")
	dh.util.clearNode(display)
	display.appendChild(document.createTextNode(text))
}

dh.flickrupload.ShareExt = function () {
	this.render = function () {
		dojo.debug("rendering photoset"); 	
		dh.sharephotoset.instance.render();
	}
}
dojo.inherits(dh.flickrupload.ShareExt, Object);

dh.sharelink.extensions.push(new dh.flickrupload.ShareExt());

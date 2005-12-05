dhFlickrInit = function () {
}

dhFlickrPhotoUploadStarted = function (filename, thumbnailFilename) {
    var dlg = document.getElementById('dh-dialog')
    var img = document.createElement("img")
    img.setAttribute("src", thumbnailFilename)
    dlg.appendChild(img)
    dlg.appendChild(document.createTextNode("upload of "))
    dh.util.dom.appendSpanText(dlg, filename, "dh-filename")
    dlg.appendChild(document.createTextNode(" started"))
    dlg.appendChild(document.createElement("br"))    
}

dhFlickrAddPhoto = function (filename, thumbnailFilename) {
}

dhFlickrPhotoUploadComplete = function (filename, photoId) {
    var dlg = document.getElementById('dh-dialog')
    dlg.appendChild(document.createTextNode("upload of "))
    dh.util.dom.appendSpanText(dlg, filename, "dh-filename")
    dlg.appendChild(document.createTextNode(" complete, photoid="))
    dh.util.dom.appendSpanText(dlg, photoId, "dh-filename")
    dlg.appendChild(document.createElement("br"))
}


/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.2</a>, using an XML
 * Schema.
 * $Id$
 */

package com.dumbhippo.services.smugmug.rest.bind;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Image.
 * 
 * @version $Revision$ $Date$
 */
public class Image implements java.io.Serializable, com.dumbhippo.Thumbnail {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/
	
	private static int THUMBNAIL_WIDTH = 50;
	private static int THUMBNAIL_HEIGHT = 50;

    /**
     * Field _id.
     */
    private java.lang.String _id;

    /**
     * Field _key.
     */
    private java.lang.String _key;

    /**
     * Field _fileName.
     */
    private java.lang.String _fileName;

    /**
     * Field _caption.
     */
    private java.lang.String _caption;

    /**
     * Field _date.
     */
    private java.lang.String _date;

    /**
     * Field _format.
     */
    private java.lang.String _format;

    /**
     * Field _width.
     */
    private java.lang.Short _width;

    /**
     * Field _height.
     */
    private java.lang.Short _height;

    /**
     * Field _lastUpdated.
     */
    private java.lang.String _lastUpdated;

    /**
     * Field _albumURL.
     */
    private java.lang.String _albumURL;

    /**
     * Field _thumbURL.
     */
    private java.lang.String _thumbURL;

    /**
     * Field _mediumURL.
     */
    private java.lang.String _mediumURL;

    /**
     * Field _albumList.
     */
    private java.util.Vector _albumList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Image() {
        super();
        this._albumList = new java.util.Vector();
    }


	public String getThumbnailHref() {
		return _thumbURL;
	}
	
	public String getThumbnailSrc() {
		return _thumbURL;
	}
	public String getThumbnailTitle() {
		return _fileName;
	}

	public int getThumbnailHeight() {
		return THUMBNAIL_HEIGHT;
	}	
	
	public int getThumbnailWidth() {
		return THUMBNAIL_WIDTH;
	}

      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vAlbum
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addAlbum(
            final com.dumbhippo.services.smugmug.rest.bind.Album vAlbum)
    throws java.lang.IndexOutOfBoundsException {
        this._albumList.addElement(vAlbum);
    }

    /**
     * 
     * 
     * @param index
     * @param vAlbum
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addAlbum(
            final int index,
            final com.dumbhippo.services.smugmug.rest.bind.Album vAlbum)
    throws java.lang.IndexOutOfBoundsException {
        this._albumList.add(index, vAlbum);
    }

    /**
     * Method enumerateAlbum.
     * 
     * @return an Enumeration over all
     * com.dumbhippo.services.smugmug.rest.bind.Album elements
     */
    public java.util.Enumeration enumerateAlbum(
    ) {
        return this._albumList.elements();
    }

    /**
     * Method getAlbum.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the
     * com.dumbhippo.services.smugmug.rest.bind.Album at the given
     * index
     */
    public com.dumbhippo.services.smugmug.rest.bind.Album getAlbum(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._albumList.size()) {
            throw new IndexOutOfBoundsException("getAlbum: Index value '" + index + "' not in range [0.." + (this._albumList.size() - 1) + "]");
        }
        
        return (com.dumbhippo.services.smugmug.rest.bind.Album) _albumList.get(index);
    }

    /**
     * Method getAlbum.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public com.dumbhippo.services.smugmug.rest.bind.Album[] getAlbum(
    ) {
        com.dumbhippo.services.smugmug.rest.bind.Album[] array = new com.dumbhippo.services.smugmug.rest.bind.Album[0];
        return (com.dumbhippo.services.smugmug.rest.bind.Album[]) this._albumList.toArray(array);
    }

    /**
     * Method getAlbumCount.
     * 
     * @return the size of this collection
     */
    public int getAlbumCount(
    ) {
        return this._albumList.size();
    }

    /**
     * Returns the value of field 'albumURL'.
     * 
     * @return the value of field 'AlbumURL'.
     */
    public java.lang.String getAlbumURL(
    ) {
        return this._albumURL;
    }

    /**
     * Returns the value of field 'caption'.
     * 
     * @return the value of field 'Caption'.
     */
    public java.lang.String getCaption(
    ) {
        return this._caption;
    }

    /**
     * Returns the value of field 'date'.
     * 
     * @return the value of field 'Date'.
     */
    public java.lang.String getDate(
    ) {
        return this._date;
    }

    /**
     * Returns the value of field 'fileName'.
     * 
     * @return the value of field 'FileName'.
     */
    public java.lang.String getFileName(
    ) {
        return this._fileName;
    }

    /**
     * Returns the value of field 'format'.
     * 
     * @return the value of field 'Format'.
     */
    public java.lang.String getFormat(
    ) {
        return this._format;
    }

    /**
     * Returns the value of field 'height'.
     * 
     * @return the value of field 'Height'.
     */
    public java.lang.Short getHeight(
    ) {
        return this._height;
    }

    /**
     * Returns the value of field 'id'.
     * 
     * @return the value of field 'Id'.
     */
    public java.lang.String getId(
    ) {
        return this._id;
    }

    /**
     * Returns the value of field 'key'.
     * 
     * @return the value of field 'Key'.
     */
    public java.lang.String getKey(
    ) {
        return this._key;
    }

    /**
     * Returns the value of field 'lastUpdated'.
     * 
     * @return the value of field 'LastUpdated'.
     */
    public java.lang.String getLastUpdated(
    ) {
        return this._lastUpdated;
    }

    /**
     * Returns the value of field 'mediumURL'.
     * 
     * @return the value of field 'MediumURL'.
     */
    public java.lang.String getMediumURL(
    ) {
        return this._mediumURL;
    }

    /**
     * Returns the value of field 'thumbURL'.
     * 
     * @return the value of field 'ThumbURL'.
     */
    public java.lang.String getThumbURL(
    ) {
        return this._thumbURL;
    }

    /**
     * Returns the value of field 'width'.
     * 
     * @return the value of field 'Width'.
     */
    public java.lang.Short getWidth(
    ) {
        return this._width;
    }

    /**
     * Method isValid.
     * 
     * @return true if this object is valid according to the schema
     */
    public boolean isValid(
    ) {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * 
     * @param out
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void marshal(
            final java.io.Writer out)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, out);
    }

    /**
     * 
     * 
     * @param handler
     * @throws java.io.IOException if an IOException occurs during
     * marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     */
    public void marshal(
            final org.xml.sax.ContentHandler handler)
    throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, handler);
    }

    /**
     * Method removeAlbum.
     * 
     * @param vAlbum
     * @return true if the object was removed from the collection.
     */
    public boolean removeAlbum(
            final com.dumbhippo.services.smugmug.rest.bind.Album vAlbum) {
        boolean removed = _albumList.remove(vAlbum);
        return removed;
    }

    /**
     * Method removeAlbumAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public com.dumbhippo.services.smugmug.rest.bind.Album removeAlbumAt(
            final int index) {
        java.lang.Object obj = this._albumList.remove(index);
        return (com.dumbhippo.services.smugmug.rest.bind.Album) obj;
    }

    /**
     */
    public void removeAllAlbum(
    ) {
        this._albumList.clear();
    }

    /**
     * 
     * 
     * @param index
     * @param vAlbum
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setAlbum(
            final int index,
            final com.dumbhippo.services.smugmug.rest.bind.Album vAlbum)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._albumList.size()) {
            throw new IndexOutOfBoundsException("setAlbum: Index value '" + index + "' not in range [0.." + (this._albumList.size() - 1) + "]");
        }
        
        this._albumList.set(index, vAlbum);
    }

    /**
     * 
     * 
     * @param vAlbumArray
     */
    public void setAlbum(
            final com.dumbhippo.services.smugmug.rest.bind.Album[] vAlbumArray) {
        //-- copy array
        _albumList.clear();
        
        for (int i = 0; i < vAlbumArray.length; i++) {
                this._albumList.add(vAlbumArray[i]);
        }
    }

    /**
     * Sets the value of field 'albumURL'.
     * 
     * @param albumURL the value of field 'albumURL'.
     */
    public void setAlbumURL(
            final java.lang.String albumURL) {
        this._albumURL = albumURL;
    }

    /**
     * Sets the value of field 'caption'.
     * 
     * @param caption the value of field 'caption'.
     */
    public void setCaption(
            final java.lang.String caption) {
        this._caption = caption;
    }

    /**
     * Sets the value of field 'date'.
     * 
     * @param date the value of field 'date'.
     */
    public void setDate(
            final java.lang.String date) {
        this._date = date;
    }

    /**
     * Sets the value of field 'fileName'.
     * 
     * @param fileName the value of field 'fileName'.
     */
    public void setFileName(
            final java.lang.String fileName) {
        this._fileName = fileName;
    }

    /**
     * Sets the value of field 'format'.
     * 
     * @param format the value of field 'format'.
     */
    public void setFormat(
            final java.lang.String format) {
        this._format = format;
    }

    /**
     * Sets the value of field 'height'.
     * 
     * @param height the value of field 'height'.
     */
    public void setHeight(
            final java.lang.Short height) {
        this._height = height;
    }

    /**
     * Sets the value of field 'id'.
     * 
     * @param id the value of field 'id'.
     */
    public void setId(
            final java.lang.String id) {
        this._id = id;
    }

    /**
     * Sets the value of field 'key'.
     * 
     * @param key the value of field 'key'.
     */
    public void setKey(
            final java.lang.String key) {
        this._key = key;
    }

    /**
     * Sets the value of field 'lastUpdated'.
     * 
     * @param lastUpdated the value of field 'lastUpdated'.
     */
    public void setLastUpdated(
            final java.lang.String lastUpdated) {
        this._lastUpdated = lastUpdated;
    }

    /**
     * Sets the value of field 'mediumURL'.
     * 
     * @param mediumURL the value of field 'mediumURL'.
     */
    public void setMediumURL(
            final java.lang.String mediumURL) {
        this._mediumURL = mediumURL;
    }

    /**
     * Sets the value of field 'thumbURL'.
     * 
     * @param thumbURL the value of field 'thumbURL'.
     */
    public void setThumbURL(
            final java.lang.String thumbURL) {
        this._thumbURL = thumbURL;
    }

    /**
     * Sets the value of field 'width'.
     * 
     * @param width the value of field 'width'.
     */
    public void setWidth(
            final java.lang.Short width) {
        this._width = width;
    }

    /**
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled
     * com.dumbhippo.services.smugmug.rest.bind.Image
     */
    public static com.dumbhippo.services.smugmug.rest.bind.Image unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.Image) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.Image.class, reader);
    }

    /**
     * 
     * 
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     */
    public void validate(
    )
    throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }

}

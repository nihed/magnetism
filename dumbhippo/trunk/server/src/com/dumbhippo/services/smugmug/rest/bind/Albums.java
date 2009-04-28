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
 * Class Albums.
 * 
 * @version $Revision$ $Date$
 */
public class Albums implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _albumList.
     */
    private java.util.Vector _albumList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Albums() {
        super();
        this._albumList = new java.util.Vector();
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
     * Method unmarshal.
     * 
     * @param reader
     * @throws org.exolab.castor.xml.MarshalException if object is
     * null or if any SAXException is thrown during marshaling
     * @throws org.exolab.castor.xml.ValidationException if this
     * object is an invalid instance according to the schema
     * @return the unmarshaled
     * com.dumbhippo.services.smugmug.rest.bind.Albums
     */
    public static com.dumbhippo.services.smugmug.rest.bind.Albums unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.Albums) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.Albums.class, reader);
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

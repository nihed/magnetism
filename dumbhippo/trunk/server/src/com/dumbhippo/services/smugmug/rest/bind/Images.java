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
 * Class Images.
 * 
 * @version $Revision$ $Date$
 */
public class Images implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _imageList.
     */
    private java.util.Vector _imageList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Images() {
        super();
        this._imageList = new java.util.Vector();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vImage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addImage(
            final com.dumbhippo.services.smugmug.rest.bind.Image vImage)
    throws java.lang.IndexOutOfBoundsException {
        this._imageList.addElement(vImage);
    }

    /**
     * 
     * 
     * @param index
     * @param vImage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addImage(
            final int index,
            final com.dumbhippo.services.smugmug.rest.bind.Image vImage)
    throws java.lang.IndexOutOfBoundsException {
        this._imageList.add(index, vImage);
    }

    /**
     * Method enumerateImage.
     * 
     * @return an Enumeration over all
     * com.dumbhippo.services.smugmug.rest.bind.Image elements
     */
    public java.util.Enumeration enumerateImage(
    ) {
        return this._imageList.elements();
    }

    /**
     * Method getImage.
     * 
     * @param index
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the
     * com.dumbhippo.services.smugmug.rest.bind.Image at the given
     * index
     */
    public com.dumbhippo.services.smugmug.rest.bind.Image getImage(
            final int index)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._imageList.size()) {
            throw new IndexOutOfBoundsException("getImage: Index value '" + index + "' not in range [0.." + (this._imageList.size() - 1) + "]");
        }
        
        return (com.dumbhippo.services.smugmug.rest.bind.Image) _imageList.get(index);
    }

    /**
     * Method getImage.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public com.dumbhippo.services.smugmug.rest.bind.Image[] getImage(
    ) {
        com.dumbhippo.services.smugmug.rest.bind.Image[] array = new com.dumbhippo.services.smugmug.rest.bind.Image[0];
        return (com.dumbhippo.services.smugmug.rest.bind.Image[]) this._imageList.toArray(array);
    }

    /**
     * Method getImageCount.
     * 
     * @return the size of this collection
     */
    public int getImageCount(
    ) {
        return this._imageList.size();
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
     */
    public void removeAllImage(
    ) {
        this._imageList.clear();
    }

    /**
     * Method removeImage.
     * 
     * @param vImage
     * @return true if the object was removed from the collection.
     */
    public boolean removeImage(
            final com.dumbhippo.services.smugmug.rest.bind.Image vImage) {
        boolean removed = _imageList.remove(vImage);
        return removed;
    }

    /**
     * Method removeImageAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public com.dumbhippo.services.smugmug.rest.bind.Image removeImageAt(
            final int index) {
        java.lang.Object obj = this._imageList.remove(index);
        return (com.dumbhippo.services.smugmug.rest.bind.Image) obj;
    }

    /**
     * 
     * 
     * @param index
     * @param vImage
     * @throws java.lang.IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setImage(
            final int index,
            final com.dumbhippo.services.smugmug.rest.bind.Image vImage)
    throws java.lang.IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._imageList.size()) {
            throw new IndexOutOfBoundsException("setImage: Index value '" + index + "' not in range [0.." + (this._imageList.size() - 1) + "]");
        }
        
        this._imageList.set(index, vImage);
    }

    /**
     * 
     * 
     * @param vImageArray
     */
    public void setImage(
            final com.dumbhippo.services.smugmug.rest.bind.Image[] vImageArray) {
        //-- copy array
        _imageList.clear();
        
        for (int i = 0; i < vImageArray.length; i++) {
                this._imageList.add(vImageArray[i]);
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
     * com.dumbhippo.services.smugmug.rest.bind.Images
     */
    public static com.dumbhippo.services.smugmug.rest.bind.Images unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.Images) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.Images.class, reader);
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

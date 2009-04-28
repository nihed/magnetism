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
 * Class RspChoice.
 * 
 * @version $Revision$ $Date$
 */
public class RspChoice implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _login.
     */
    private com.dumbhippo.services.smugmug.rest.bind.Login _login;

    /**
     * Field _albums.
     */
    private com.dumbhippo.services.smugmug.rest.bind.Albums _albums;

    /**
     * Field _images.
     */
    private com.dumbhippo.services.smugmug.rest.bind.Images _images;

    /**
     * Field _err.
     */
    private com.dumbhippo.services.smugmug.rest.bind.Err _err;


      //----------------/
     //- Constructors -/
    //----------------/

    public RspChoice() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'albums'.
     * 
     * @return the value of field 'Albums'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.Albums getAlbums(
    ) {
        return this._albums;
    }

    /**
     * Returns the value of field 'err'.
     * 
     * @return the value of field 'Err'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.Err getErr(
    ) {
        return this._err;
    }

    /**
     * Returns the value of field 'images'.
     * 
     * @return the value of field 'Images'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.Images getImages(
    ) {
        return this._images;
    }

    /**
     * Returns the value of field 'login'.
     * 
     * @return the value of field 'Login'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.Login getLogin(
    ) {
        return this._login;
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
     * Sets the value of field 'albums'.
     * 
     * @param albums the value of field 'albums'.
     */
    public void setAlbums(
            final com.dumbhippo.services.smugmug.rest.bind.Albums albums) {
        this._albums = albums;
    }

    /**
     * Sets the value of field 'err'.
     * 
     * @param err the value of field 'err'.
     */
    public void setErr(
            final com.dumbhippo.services.smugmug.rest.bind.Err err) {
        this._err = err;
    }

    /**
     * Sets the value of field 'images'.
     * 
     * @param images the value of field 'images'.
     */
    public void setImages(
            final com.dumbhippo.services.smugmug.rest.bind.Images images) {
        this._images = images;
    }

    /**
     * Sets the value of field 'login'.
     * 
     * @param login the value of field 'login'.
     */
    public void setLogin(
            final com.dumbhippo.services.smugmug.rest.bind.Login login) {
        this._login = login;
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
     * com.dumbhippo.services.smugmug.rest.bind.RspChoice
     */
    public static com.dumbhippo.services.smugmug.rest.bind.RspChoice unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.RspChoice) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.RspChoice.class, reader);
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

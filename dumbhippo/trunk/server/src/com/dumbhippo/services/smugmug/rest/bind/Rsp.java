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
 * Class Rsp.
 * 
 * @version $Revision$ $Date$
 */
public class Rsp implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field _stat.
     */
    private com.dumbhippo.services.smugmug.rest.bind.types.RspStatType _stat;

    /**
     * Field _method.
     */
    private java.lang.String _method;

    /**
     * Field _rspChoice.
     */
    private com.dumbhippo.services.smugmug.rest.bind.RspChoice _rspChoice;


      //----------------/
     //- Constructors -/
    //----------------/

    public Rsp() {
        super();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'method'.
     * 
     * @return the value of field 'Method'.
     */
    public java.lang.String getMethod(
    ) {
        return this._method;
    }

    /**
     * Returns the value of field 'rspChoice'.
     * 
     * @return the value of field 'RspChoice'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.RspChoice getRspChoice(
    ) {
        return this._rspChoice;
    }

    /**
     * Returns the value of field 'stat'.
     * 
     * @return the value of field 'Stat'.
     */
    public com.dumbhippo.services.smugmug.rest.bind.types.RspStatType getStat(
    ) {
        return this._stat;
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
     * Sets the value of field 'method'.
     * 
     * @param method the value of field 'method'.
     */
    public void setMethod(
            final java.lang.String method) {
        this._method = method;
    }

    /**
     * Sets the value of field 'rspChoice'.
     * 
     * @param rspChoice the value of field 'rspChoice'.
     */
    public void setRspChoice(
            final com.dumbhippo.services.smugmug.rest.bind.RspChoice rspChoice) {
        this._rspChoice = rspChoice;
    }

    /**
     * Sets the value of field 'stat'.
     * 
     * @param stat the value of field 'stat'.
     */
    public void setStat(
            final com.dumbhippo.services.smugmug.rest.bind.types.RspStatType stat) {
        this._stat = stat;
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
     * com.dumbhippo.services.smugmug.rest.bind.Rsp
     */
    public static com.dumbhippo.services.smugmug.rest.bind.Rsp unmarshal(
            final java.io.Reader reader)
    throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (com.dumbhippo.services.smugmug.rest.bind.Rsp) Unmarshaller.unmarshal(com.dumbhippo.services.smugmug.rest.bind.Rsp.class, reader);
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

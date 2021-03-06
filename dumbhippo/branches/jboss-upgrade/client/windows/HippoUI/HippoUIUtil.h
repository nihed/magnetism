/* HippoUIUtil.h: Some useful string and URL manipulation functions
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include "stdafx.h"
#include <HippoUtil.h>
#include <HippoArray.h>
#include <glib-object.h>

class HippoUIUtil
{
public:
    /**
     * Append a query string to an url.
     * @param url         the base url; will be modified by appending the query string
     * @param paramNames  the parameter names to append
     * @param paramValues the parameter values to append; must correspond one-to-one with paramNames
     */
    static void encodeQueryString(HippoBSTR                   &url, 
                                  const HippoArray<HippoBSTR> &paramNames, 
                                  const HippoArray<HippoBSTR> &paramValues);
    /**
     * Splits a string on a separator character; if there are N occurrences
     * of the separator in the string, the result will have N+1 items.
     *
     * @param str       the string to split
     * @param separator the separator character
     * @param result    an array into which to store the result; the results are simply
     *                  appended to this without clearing it before hand
     */
    static void splitString(const HippoBSTR       &str, 
                            WCHAR                  separator, 
                            HippoArray<HippoBSTR> &result);
};

// Basically just a wrapper for g_utf16_to_utf8(), think three times before extending it
class HippoUStr 
{
public:

    HippoUStr() {
        str = NULL;
    }

    HippoUStr(const HippoBSTR &bstr) {
        if (bstr.m_str)       
            str = g_utf16_to_utf8((gunichar2 *)bstr.m_str, SysStringLen(bstr.m_str), NULL, NULL, NULL);
        else
            str = NULL;
    }

    HippoUStr(WCHAR *wstr) {
        if (wstr)
            str = g_utf16_to_utf8((gunichar2 *)wstr, -1, NULL, NULL, NULL);
        else
            str = NULL;
    }

    HippoUStr(WCHAR *wstr, int len) {
        if (wstr)
            str = g_utf16_to_utf8((gunichar2 *)wstr, len, NULL, NULL, NULL);
        else
            str = NULL;
    }

    ~HippoUStr() {
        g_free((void*)str);
    }

    char** operator&() {
        assert(str == NULL);
        return &str;
    }

    const char *c_str() const {
        return str;
    }

    HippoBSTR toBSTR() const {
        if (str == NULL)
            return HippoBSTR();
        else
            return HippoBSTR::fromUTF8(str, -1);
    }

    // get a g_malloc allocated string and unset this one;
    // must be g_free'd 
    char *steal() {
        char *tmp = str;
        str = NULL;
        return tmp;
    }

private:
    HippoUStr(const HippoUStr &other) {}
    HippoUStr operator=(const HippoUStr &other) {}

    char *str;
};

/* this is just so the implementations aren't templatized */
class HippoGObjectRefcounter {
protected:
    static void ref(GObject *object);
    static void unref(GObject *object);
};

template<class T>
class HippoGObjectPtr
    : private HippoGObjectRefcounter
{
public:
    HippoGObjectPtr() : raw_(0) {
    }
    HippoGObjectPtr(T *t) : raw_(t) {
        if (raw_)
            ref(G_OBJECT(raw_));
    }
    HippoGObjectPtr(const HippoGObjectPtr &other) : raw_(0) {
        assign(other.raw_);
    }
    ~HippoGObjectPtr() {
        if (raw_) {
            unref(G_OBJECT(raw_));
        }
    }
    operator T *(){
        return raw_;
    }
    operator const T *() const{
        return raw_;
    }

    // FIXME These two conversions make operator == and != ambiguous, which kind 
    // of sucks... but they are needed to make the GLib cast macros work.
    // possible solutions include use .g_obj() instead of the conversion, 
    // operator bool, explicit operator==, ... some of those could have their
    // own issues. I did add operator! for now and you can also solve it 
    // by casting NULL to a type, like "ptr == (GtkWidget*)NULL"

    operator GObject *() {
        return (GObject*) raw_;
    }

    operator GTypeInstance *() {
        return (GTypeInstance*) raw_;
    }

    bool operator!() {
        return !raw_;
    }

    T **operator&() {
        assert(raw_ == NULL);
        return &raw_;
    }
    HippoGObjectPtr& operator=(T *t) {
        assign(t);
        return *this;
    }
    HippoGObjectPtr& operator=(const HippoGObjectPtr &other) {
        assign(other.raw_);
        return *this;
    }

protected:
    T *raw_;

private:
    void assign(T *t) {
        // ref first to protect against self-assignment
        if (t)
            ref(G_OBJECT(t));
        if (raw_)
            unref(G_OBJECT(raw_));
        raw_ = t;
    }
};

/* HippoArray.h: utility array class
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <limits.h>
#include <new>

template <class T>
class HippoArray {
public:
    HippoArray() {
        elements_ = NULL;
        length_ = 0;
        maxLength_ = 0;
    }

    HippoArray(const HippoArray<T> &other) {
        copyFrom(other);
    }

    ~HippoArray() {
        if (elements_)
            delete[] elements_;
    }

    const T& operator[](ULONG i) const {
        return elements_[i];
    }

    T& operator[](ULONG i) {
        return elements_[i];
    }

    ULONG length() const {
        return length_;
    }

    HippoArray<T> & operator=(const HippoArray<T> &other) throw (std::bad_alloc) {
        copyFrom(other);
        return *this;
    }

    HRESULT copyFrom(const HippoArray<T> &other);
    HRESULT append(const T& t);
    void remove(ULONG element);

private:
    T *elements_;
    ULONG length_;
    ULONG maxLength_;
};

template <class T>
HRESULT
HippoArray<T>::copyFrom(const HippoArray<T> &other)
{
    T *newElements;

    if (other.elements_) {
        newElements = new T[other.maxLength_];
        if (!newElements)
            return E_OUTOFMEMORY;

        for (ULONG i = 0; i < other.length_; i++)
            newElements[i] = other.elements_[i];
    } else {
        newElements = NULL;
    }

    if (elements_)
        delete[] elements_;

    elements_ = newElements;
    maxLength_ = other.maxLength_;
    length_ = other.length_;

    return S_OK;
}

template <class T>
HRESULT
HippoArray<T>::append(const T& t) 
{
    if (length_ == maxLength_) {
        ULONG newLength;

        if (maxLength_ == ULONG_MAX)
            return E_OUTOFMEMORY;

        if (maxLength_ == 0)
            newLength = 1;
        else if (length_ <= ULONG_MAX / 2)
            newLength = maxLength_ * 2;
        else
            newLength = ULONG_MAX;

        T *newElements = new T[newLength];
        if (!newElements) 
            return E_OUTOFMEMORY;
        for (ULONG i = 0; i < length_; i++)
            newElements[i] = elements_[i];
        
        elements_ = newElements;
        maxLength_ = newLength;
    }

    elements_[length_++] = t;

    return S_OK;
}

template <class T> 
void 
HippoArray<T>::remove(ULONG element) 
{
    for (ULONG i = element; i + 1 < length_; i++)
        elements_[i] = elements_[i + 1];
    elements_[length_ - 1] = T();
   
    length_--;
}

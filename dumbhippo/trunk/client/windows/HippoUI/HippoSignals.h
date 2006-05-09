// Generated file from codegen.py ; do not edit
#pragma once


// Abstract base class for slots with 0 argument(s)
template <class ReturnType>
class Slot0 : public Slot
{
protected:
  Slot0 ()
  {
  }
public:
  virtual ReturnType invoke () const
  {
    // we're effectively const since we ref/unref in a pair here
    const_cast<Slot0*>(this)->ref ();
    return invoke_impl ();
    const_cast<Slot0*>(this)->unref ();
  }
  ReturnType operator() () const
  {
    return invoke ();
  }
protected:
  virtual ReturnType invoke_impl () const = 0;

};

// Concrete class for slots created from methods with 0 argument(s)
template <class MethodClassType, class ReturnType>
class MethodSlot0 : public Slot0<ReturnType>
{
private:
  typedef ReturnType (MethodClassType::* MethodType) ();
public:
  MethodSlot0 (MethodClassType *object,
               MethodType method)
    : obj_(object), method_(method)
  {
  }
  virtual ReturnType invoke_impl () const
  {
    return (obj_->*method_) ();
  }
protected:
  virtual ~MethodSlot0 ()
  {
  }
private:
  MethodClassType *obj_;
  MethodType method_;
}; // class MethodSlot0

// Concrete class for slots created from static functions with 0 argument(s)
template <class ReturnType>
class FunctionSlot0 : public Slot0<ReturnType>
{
private:
  typedef ReturnType (* FunctionType) ();
public:
  FunctionSlot0 (FunctionType function)
    : function_(function)
  {
  }
  virtual ReturnType invoke_impl () const
  {
    return (* function_) ();
  }
protected:
  virtual ~FunctionSlot0 ()
  {
  }
private:
  FunctionType function_;
}; // class FunctionSlot0

// convenience function that creates a MethodSlot
template <class MethodClassType, class ReturnType>
inline Slot0<ReturnType> *
slot (MethodClassType *obj, ReturnType (MethodClassType::* method) ())
{
  return new MethodSlot0<MethodClassType, ReturnType> (obj, method);
}

// convenience function that creates a FunctionSlot
template <class ReturnType>
inline Slot0<ReturnType> *
slot (ReturnType (* function) ())
{
  return new FunctionSlot0<ReturnType> (function);
}

// Abstract base class for slots with 1 argument(s)
template <class ReturnType, class Arg1Type>
class Slot1 : public Slot
{
protected:
  Slot1 ()
  {
  }
public:
  virtual ReturnType invoke (Arg1Type arg1) const
  {
    // we're effectively const since we ref/unref in a pair here
    const_cast<Slot1*>(this)->ref ();
    return invoke_impl (arg1);
    const_cast<Slot1*>(this)->unref ();
  }
  ReturnType operator() (Arg1Type arg1) const
  {
    return invoke (arg1);
  }
protected:
  virtual ReturnType invoke_impl (Arg1Type arg1) const = 0;

};

// Concrete class for slots created from methods with 1 argument(s)
template <class MethodClassType, class ReturnType, class Arg1Type>
class MethodSlot1 : public Slot1<ReturnType, Arg1Type>
{
private:
  typedef ReturnType (MethodClassType::* MethodType) (Arg1Type);
public:
  MethodSlot1 (MethodClassType *object,
               MethodType method)
    : obj_(object), method_(method)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1) const
  {
    return (obj_->*method_) (arg1);
  }
protected:
  virtual ~MethodSlot1 ()
  {
  }
private:
  MethodClassType *obj_;
  MethodType method_;
}; // class MethodSlot1

// Concrete class for slots created from static functions with 1 argument(s)
template <class ReturnType, class Arg1Type>
class FunctionSlot1 : public Slot1<ReturnType, Arg1Type>
{
private:
  typedef ReturnType (* FunctionType) (Arg1Type);
public:
  FunctionSlot1 (FunctionType function)
    : function_(function)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1) const
  {
    return (* function_) (arg1);
  }
protected:
  virtual ~FunctionSlot1 ()
  {
  }
private:
  FunctionType function_;
}; // class FunctionSlot1

// convenience function that creates a MethodSlot
template <class MethodClassType, class ReturnType, class Arg1Type>
inline Slot1<ReturnType, Arg1Type> *
slot (MethodClassType *obj, ReturnType (MethodClassType::* method) (Arg1Type))
{
  return new MethodSlot1<MethodClassType, ReturnType, Arg1Type> (obj, method);
}

// convenience function that creates a FunctionSlot
template <class ReturnType, class Arg1Type>
inline Slot1<ReturnType, Arg1Type> *
slot (ReturnType (* function) (Arg1Type))
{
  return new FunctionSlot1<ReturnType, Arg1Type> (function);
}

// Abstract base class for slots with 2 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type>
class Slot2 : public Slot
{
protected:
  Slot2 ()
  {
  }
public:
  virtual ReturnType invoke (Arg1Type arg1, Arg2Type arg2) const
  {
    // we're effectively const since we ref/unref in a pair here
    const_cast<Slot2*>(this)->ref ();
    return invoke_impl (arg1, arg2);
    const_cast<Slot2*>(this)->unref ();
  }
  ReturnType operator() (Arg1Type arg1, Arg2Type arg2) const
  {
    return invoke (arg1, arg2);
  }
protected:
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2) const = 0;

};

// Concrete class for slots created from methods with 2 argument(s)
template <class MethodClassType, class ReturnType, class Arg1Type, class Arg2Type>
class MethodSlot2 : public Slot2<ReturnType, Arg1Type, Arg2Type>
{
private:
  typedef ReturnType (MethodClassType::* MethodType) (Arg1Type, Arg2Type);
public:
  MethodSlot2 (MethodClassType *object,
               MethodType method)
    : obj_(object), method_(method)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2) const
  {
    return (obj_->*method_) (arg1, arg2);
  }
protected:
  virtual ~MethodSlot2 ()
  {
  }
private:
  MethodClassType *obj_;
  MethodType method_;
}; // class MethodSlot2

// Concrete class for slots created from static functions with 2 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type>
class FunctionSlot2 : public Slot2<ReturnType, Arg1Type, Arg2Type>
{
private:
  typedef ReturnType (* FunctionType) (Arg1Type, Arg2Type);
public:
  FunctionSlot2 (FunctionType function)
    : function_(function)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2) const
  {
    return (* function_) (arg1, arg2);
  }
protected:
  virtual ~FunctionSlot2 ()
  {
  }
private:
  FunctionType function_;
}; // class FunctionSlot2

// convenience function that creates a MethodSlot
template <class MethodClassType, class ReturnType, class Arg1Type, class Arg2Type>
inline Slot2<ReturnType, Arg1Type, Arg2Type> *
slot (MethodClassType *obj, ReturnType (MethodClassType::* method) (Arg1Type, Arg2Type))
{
  return new MethodSlot2<MethodClassType, ReturnType, Arg1Type, Arg2Type> (obj, method);
}

// convenience function that creates a FunctionSlot
template <class ReturnType, class Arg1Type, class Arg2Type>
inline Slot2<ReturnType, Arg1Type, Arg2Type> *
slot (ReturnType (* function) (Arg1Type, Arg2Type))
{
  return new FunctionSlot2<ReturnType, Arg1Type, Arg2Type> (function);
}

// Abstract base class for slots with 3 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
class Slot3 : public Slot
{
protected:
  Slot3 ()
  {
  }
public:
  virtual ReturnType invoke (Arg1Type arg1, Arg2Type arg2, Arg3Type arg3) const
  {
    // we're effectively const since we ref/unref in a pair here
    const_cast<Slot3*>(this)->ref ();
    return invoke_impl (arg1, arg2, arg3);
    const_cast<Slot3*>(this)->unref ();
  }
  ReturnType operator() (Arg1Type arg1, Arg2Type arg2, Arg3Type arg3) const
  {
    return invoke (arg1, arg2, arg3);
  }
protected:
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2, Arg3Type arg3) const = 0;

};

// Concrete class for slots created from methods with 3 argument(s)
template <class MethodClassType, class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
class MethodSlot3 : public Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type>
{
private:
  typedef ReturnType (MethodClassType::* MethodType) (Arg1Type, Arg2Type, Arg3Type);
public:
  MethodSlot3 (MethodClassType *object,
               MethodType method)
    : obj_(object), method_(method)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2, Arg3Type arg3) const
  {
    return (obj_->*method_) (arg1, arg2, arg3);
  }
protected:
  virtual ~MethodSlot3 ()
  {
  }
private:
  MethodClassType *obj_;
  MethodType method_;
}; // class MethodSlot3

// Concrete class for slots created from static functions with 3 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
class FunctionSlot3 : public Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type>
{
private:
  typedef ReturnType (* FunctionType) (Arg1Type, Arg2Type, Arg3Type);
public:
  FunctionSlot3 (FunctionType function)
    : function_(function)
  {
  }
  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2, Arg3Type arg3) const
  {
    return (* function_) (arg1, arg2, arg3);
  }
protected:
  virtual ~FunctionSlot3 ()
  {
  }
private:
  FunctionType function_;
}; // class FunctionSlot3

// convenience function that creates a MethodSlot
template <class MethodClassType, class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
inline Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> *
slot (MethodClassType *obj, ReturnType (MethodClassType::* method) (Arg1Type, Arg2Type, Arg3Type))
{
  return new MethodSlot3<MethodClassType, ReturnType, Arg1Type, Arg2Type, Arg3Type> (obj, method);
}

// convenience function that creates a FunctionSlot
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
inline Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> *
slot (ReturnType (* function) (Arg1Type, Arg2Type, Arg3Type))
{
  return new FunctionSlot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> (function);
}

// slot with 0 argument(s) created from a slot with 1 argument(s)
template <class ReturnType, class Arg1Type>
class BoundSlot0_1: public Slot0<ReturnType>
{
public:
  BoundSlot0_1 (Slot1<ReturnType, Arg1Type> * slot, Arg1Type arg1)
    : original_slot_(slot), arg1_(arg1)
  {
    original_slot_->ref ();
    original_slot_->sink ();
  }

  virtual ReturnType invoke_impl () const
  {
    return original_slot_->invoke (arg1_);
  }
protected:
  virtual ~BoundSlot0_1 ()
  {
    original_slot_->unref ();
  }
private:
  Slot1<ReturnType, Arg1Type> * original_slot_;
  Arg1Type arg1_;
}; // class BoundSlot0_1

// convenience function that creates a BoundSlot
template <class ReturnType, class Arg1Type>
inline Slot0<ReturnType> *
bind (Slot1<ReturnType, Arg1Type> * s, Arg1Type arg1)
{
  return new BoundSlot0_1<ReturnType, Arg1Type> (s, arg1);
}

// slot with 1 argument(s) created from a slot with 2 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type>
class BoundSlot1_2: public Slot1<ReturnType, Arg1Type>
{
public:
  BoundSlot1_2 (Slot2<ReturnType, Arg1Type, Arg2Type> * slot, Arg2Type arg2)
    : original_slot_(slot), arg2_(arg2)
  {
    original_slot_->ref ();
    original_slot_->sink ();
  }

  virtual ReturnType invoke_impl (Arg1Type arg1) const
  {
    return original_slot_->invoke (arg1, arg2_);
  }
protected:
  virtual ~BoundSlot1_2 ()
  {
    original_slot_->unref ();
  }
private:
  Slot2<ReturnType, Arg1Type, Arg2Type> * original_slot_;
  Arg2Type arg2_;
}; // class BoundSlot1_2

// convenience function that creates a BoundSlot
template <class ReturnType, class Arg1Type, class Arg2Type>
inline Slot1<ReturnType, Arg1Type> *
bind (Slot2<ReturnType, Arg1Type, Arg2Type> * s, Arg2Type arg2)
{
  return new BoundSlot1_2<ReturnType, Arg1Type, Arg2Type> (s, arg2);
}

// slot with 2 argument(s) created from a slot with 3 argument(s)
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
class BoundSlot2_3: public Slot2<ReturnType, Arg1Type, Arg2Type>
{
public:
  BoundSlot2_3 (Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> * slot, Arg3Type arg3)
    : original_slot_(slot), arg3_(arg3)
  {
    original_slot_->ref ();
    original_slot_->sink ();
  }

  virtual ReturnType invoke_impl (Arg1Type arg1, Arg2Type arg2) const
  {
    return original_slot_->invoke (arg1, arg2, arg3_);
  }
protected:
  virtual ~BoundSlot2_3 ()
  {
    original_slot_->unref ();
  }
private:
  Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> * original_slot_;
  Arg3Type arg3_;
}; // class BoundSlot2_3

// convenience function that creates a BoundSlot
template <class ReturnType, class Arg1Type, class Arg2Type, class Arg3Type>
inline Slot2<ReturnType, Arg1Type, Arg2Type> *
bind (Slot3<ReturnType, Arg1Type, Arg2Type, Arg3Type> * s, Arg3Type arg3)
{
  return new BoundSlot2_3<ReturnType, Arg1Type, Arg2Type, Arg3Type> (s, arg3);
}


/* HippoMenu.h: popup menu for the notification icon
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoAbstractWindow.h"
#include <vector>

class HippoActivePost {
public:
    HippoActivePost(const HippoBSTR &postId, 
                    const HippoBSTR &title,
                    const HippoBSTR &senderName,
                    int              chattingUserCount);

    const HippoBSTR &getPostId() const { return postId_; }
    const HippoBSTR &getTitle() const { return title_; }
    const HippoBSTR &getSenderName() const { return senderName_; }
    int getChattingUserCount() const { return chattingUserCount_; }

    // Default copy constructor and assignment operator work

private:
    HippoBSTR postId_;
    HippoBSTR title_;
    HippoBSTR senderName_;
    int chattingUserCount_;
};

class HippoMenu :
    public IHippoMenu,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoMenu();
    ~HippoMenu();

    /**
     * Pop up the menu
     * @param mouseX X coordinate the mouse was clicked at
     * @param mouseY Y coordinate the mouse was clicked at
     **/
    void popup(int mouseX, int mouseY);

    /**
     * Remove all active posts listed in the menu
     **/
    void clearActivePosts();

    /**
     * Add an active post to the list at the top of the menu; if a post
     * with the same ID is already in the list, it will be removed first.
     */
    void addActivePost(const HippoActivePost &post);

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
    STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);           
    STDMETHODIMP GetTypeInfoCount (unsigned int *);
    STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                         VARIANT *, EXCEPINFO *, unsigned int *);

    // IHippoMenu
    STDMETHODIMP Exit();
    STDMETHODIMP GetServerBaseUrl(BSTR *result);
    STDMETHODIMP Hush();
    STDMETHODIMP Resize(int width, int height);

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();
    virtual void initializeBrowser();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onClose(bool fromScript);

private:
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    DWORD refCount_;
    std::vector<HippoActivePost> activePosts_;

    int desiredWidth_;
    int desiredHeight_;
    int mouseX_;
    int mouseY_;

    void moveResizeWindow(void);
    void invokeRemoveActivePost(int i);
    void invokeInsertActivePost(int i, const HippoActivePost &post);
};

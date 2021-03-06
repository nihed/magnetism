package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;

import com.dumbhippo.SortUtils;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.OnlineAccountTypeView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class OnlineAccountTypesPage extends AbstractSigninOptionalPage {
	private ListBean<OnlineAccountTypeView> accountTypesListBean;
	private OnlineAccountTypeView onlineAccountTypeView;
	private boolean accountTypeNameValid;
	private boolean allowEdit;
	private boolean allowRemoval;
	
	private ExternalAccountSystem externalAccounts;
	
	private IdentitySpider identitySpider;
	
	public OnlineAccountTypesPage() {
		externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		accountTypesListBean = null;
		onlineAccountTypeView = null;
		accountTypeNameValid = true;
		allowEdit = false;
		allowRemoval = false;
	}
	
    public void setAccountTypeName(String accountTypeName) throws HumanVisibleException {
    	try {
    		onlineAccountTypeView = new OnlineAccountTypeView(externalAccounts.lookupOnlineAccountTypeForName(accountTypeName));
            if ((getViewpoint() instanceof UserViewpoint) &&
            	(identitySpider.isAdministrator(((UserViewpoint)getViewpoint()).getViewer()) ||
            	 ((UserViewpoint)getViewpoint()).getViewer().equals(onlineAccountTypeView.getOnlineAccountType().getCreator()))) {
            	allowEdit = true;
            }   	    
            if (allowEdit && 
            	externalAccounts.getNumberOfOnlineAccountsForType(getViewpoint(), onlineAccountTypeView.getOnlineAccountType()) == 0) {
            	allowRemoval = true;
            }
    	} catch (NotFoundException e) {
    		accountTypeNameValid = false;
    	}
    }
    
    public OnlineAccountTypeView getOnlineAccountType() {
    	return onlineAccountTypeView;
    }
    
    public boolean isAllowEdit() {
    	return allowEdit;
    }
    
    public boolean isAllowRemoval() {
    	return allowRemoval;
    }
    
    public boolean isAccountTypeNameValid() {
    	return accountTypeNameValid;
    }
	
	public ListBean<OnlineAccountTypeView> getAllOnlineAccountTypes() {
		if (accountTypesListBean != null) {
			return accountTypesListBean;
		}
			
		List<OnlineAccountTypeView> allTypeViews = new ArrayList<OnlineAccountTypeView>(); 
		List<OnlineAccountType> allTypes = externalAccounts.getAllOnlineAccountTypes();
		List<OnlineAccountType> alphabetizedTypes =
			SortUtils.sortCollection(allTypes.toArray(new OnlineAccountType[allTypes.size()]), "getFullName");
		
		for (OnlineAccountType type : alphabetizedTypes) {
		    allTypeViews.add(new OnlineAccountTypeView(type));
		}
		
		accountTypesListBean = new ListBean<OnlineAccountTypeView>(allTypeViews);
		return accountTypesListBean;
	}
}

package com.dumbhippo.services.caches;

import javax.ejb.Local;

import com.dumbhippo.services.AmazonItemView;

@Local
public interface AmazonItemCache extends Cache<String,AmazonItemView> {

}

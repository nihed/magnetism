package com.dumbhippo.web;

import java.util.HashMap;
import java.util.Map;
import com.dumbhippo.server.Pageable;

import javax.servlet.http.HttpServletRequest;

public class PagePositionsBean {
	// When "viewing all results" isn't meaningful, we create a "bounded" 
	// Pageable that has an arbitrary limit. 57 gives us 10 pages of results
	// with the default page sizes of initial=3, subsequent=6
	static final int DEFAULT_BOUND = 57;
	
	private Map<String, Integer> positions;
	
	private PagePositionsBean(HttpServletRequest request) {
		positions = new HashMap<String, Integer>();
		
		String param = request.getParameter("pos");
		if (param == null)
			return;
		
		String[] settings = param.split("!");
		for (int i = 0; i < settings.length; i++) {
			int colon = settings[i].indexOf("-");
			if (colon > 0) {
				try { 
					int position = Integer.parseInt(settings[i].substring(colon + 1));
					if (position >= 0)
						positions.put(settings[i].substring(0, colon), position);
				} catch (NumberFormatException e) {
					// Ignore
				}
			}
		}
	}
	
	public static PagePositionsBean getForRequest(HttpServletRequest request) {
		PagePositionsBean bean = (PagePositionsBean)request.getAttribute("pagePositions");
		if (bean == null) {
			bean = new PagePositionsBean(request);
			request.setAttribute("pagePositions", bean);
		}
	
		return bean;
	}
	
	public <T> Pageable<T> createPageable(String name) {
		return createPageable(name, -1);
	}
	
	public <T> Pageable<T> createPageable(String name, int initialPerPage) {
		Pageable<T> pageable = new Pageable<T>(name);
		
		Integer position = positions.get(name);
		if (position != null)
			pageable.setPosition(position);
		if (initialPerPage > 0)
			pageable.setInitialPerPage(initialPerPage);
		
		return pageable;
	}
	
	public <T> Pageable<T> createBoundedPageable(String name) {
		return createBoundedPageable(name, -1);
	}
	
	public <T> Pageable<T> createBoundedPageable(String name, int initialPerPage) {
		Pageable<T> pageable = createPageable(name, initialPerPage);
		pageable.setBound(DEFAULT_BOUND);
		
		return pageable;
	}
}

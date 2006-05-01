package com.dumbhippo.web;

import java.util.HashMap;
import java.util.Map;
import com.dumbhippo.server.Pageable;

import javax.servlet.http.HttpServletRequest;

public class PagePositionsBean {
	Map<String, Integer> positions;
	
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
		Pageable<T> pageable = new Pageable<T>(name);
		
		Integer position = positions.get(name);
		if (position != null)
			pageable.setPosition(position);
		
		return pageable;
	}
}

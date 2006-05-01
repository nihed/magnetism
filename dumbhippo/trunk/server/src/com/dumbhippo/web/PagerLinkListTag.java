package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class PagerLinkListTag extends SimpleTagSupport {
	private int initialPerPage;
	private int subsequentPerPage;
	private int index;
	private int total;
	private String onClick;
	
	private void writeLinkHtml(JspWriter writer, String cssClass, int idx, String content) throws IOException {
		writer.append("<a class=\"");
		writer.append(cssClass);
		writer.append("\" href=\"\" onclick=\"return ");
		writer.append(onClick);
		writer.append("(");
		writer.append(""+idx);
		writer.append(");\">");
		writer.append(content);
		writer.append("</a>");		
	}
	
	@Override
	public void doTag() throws IOException, JspException {
		JspWriter writer = getJspContext().getOut();		
		
		int pages;
		if (total < initialPerPage)
			pages = 1;
		else 
			pages = 1 + (total - initialPerPage + subsequentPerPage - 1) / subsequentPerPage;
		
		for (int i = 0; i < pages; i++) {
			String visiblePage = Integer.toString(i+1);
			if (i == index) {
				writer.append(visiblePage);
			} else {
				writeLinkHtml(writer, "dh-pager-link", i, visiblePage);
			}
			if (i < (pages - 1)) {
				writer.append(" ");
			}			
		}
		if (index < (pages-1)) {
			writer.append(" ");
			writeLinkHtml(writer, "dh-pager-link-next", index+1, "Next");
		}
	}
	
	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}
	
	public void setInitialPerPage(int perPage) {
		initialPerPage = perPage;
	}
	
	public void setSubsequentPerPage(int perPage) {
		subsequentPerPage = perPage;
	}
	
	public void setIndex(int idx) {
		this.index = idx;
	}

	public void setTotal(int total) {
		this.total = total;
	}
}

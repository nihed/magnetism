package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class PagerLinkListTag extends SimpleTagSupport {
	private int resultsPerPage;
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
		
		int pages = total / resultsPerPage;
		if ((total % resultsPerPage) != 0)
			pages++;
		
		for (int i = 0; i < pages; i++) {
			String visibleIdx = new Integer(i+1).toString();
			if (i == index) {
				writer.append(visibleIdx);
			} else {
				writeLinkHtml(writer, "dh-pager-link", i, visibleIdx);
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
	
	public void setResultsPerPage(int perPage) {
		resultsPerPage = perPage;
	}
	
	public void setIndex(int idx) {
		this.index = idx;
	}

	public void setTotal(int total) {
		this.total = total;
	}
}

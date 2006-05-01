package com.dumbhippo.web;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.dumbhippo.server.Pageable;

public class PagerLinkListTag extends SimpleTagSupport {
	
	private Pageable<?> pageable;
	private String anchor;
	
	private void writeLinkHtml(JspWriter writer, String cssClass, int page, String content) throws IOException {
		writer.append("<a class=\"");
		writer.append(cssClass);
		writer.append("\" href=\"#\" onclick=\'return dh.actions.switchPage(\"");
		writer.append(pageable.getName());
		writer.append("\",");
		if (anchor != null) {
			writer.append("\"" + anchor + "\"");
		} else {
			writer.append("null");
		}
		writer.append(",");
		writer.append("" + page);
		writer.append(");\'>");
		writer.append(content);
		writer.append("</a>");		
	}
	
	@Override
	public void doTag() throws IOException, JspException {
		JspWriter writer = getJspContext().getOut();		
		
		int pages = pageable.getPageCount();;
		
		for (int i = 0; i < pages; i++) {
			String visiblePage = Integer.toString(i + 1);
			if (i == pageable.getPosition()) {
				writer.append(visiblePage);
			} else {
				writeLinkHtml(writer, "dh-pager-link", i, visiblePage);
			}
			if (i < (pages - 1)) {
				writer.append(" ");
			}			
		}
		if (pageable.getPosition() < (pages - 1)) {
			writer.append(" ");
			writeLinkHtml(writer, "dh-pager-link-next", pageable.getPosition() + 1, "Next");
		}
	}
	
	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}
	
	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
}

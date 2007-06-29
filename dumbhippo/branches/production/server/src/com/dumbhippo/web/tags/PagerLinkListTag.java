package com.dumbhippo.web.tags;

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
	
	public void appendPageNumber(JspWriter writer, int number) throws IOException {
		String visiblePage = Integer.toString(number + 1);
		if (number == pageable.getPosition()) {
			writer.append(visiblePage);
		} else {
			writeLinkHtml(writer, "dh-pager-link", number, visiblePage);
		}
		
		writer.append(" ");
	}
	
	@Override
	public void doTag() throws IOException, JspException {
		JspWriter writer = getJspContext().getOut();		
		
		int pages = pageable.getPageCount();
		int position = pageable.getPosition();
		
		// Possible displays
		
		if (pages <= 10) {
			// 1 2 3 4 5 6 7 8 9 10 Next
			for (int i = 0; i < pages; i++) {
				appendPageNumber(writer, i);
			}
		} else if (position <= 4) {
   		    // 1 2 3 4 5 6 7 ... 42 Next
			for (int i = 0; i < 7; i++) {
				appendPageNumber(writer, i);
			}
			writer.append("&#x2026; ");
			appendPageNumber(writer, pages - 1);
		} else if (position < pages - 5) {
			// 1 ... 21 22 23 24 25 ... 42
			appendPageNumber(writer, 0);
			writer.append("&#x2026; ");
			for (int i = position - 2; i <= position + 2; i++) {
				appendPageNumber(writer, i);
			}
			writer.append("&#x2026; ");
			appendPageNumber(writer, pages - 1);
		} else {
			// 1 ... 36 37 38 39 40 41 42
			appendPageNumber(writer, 0);
			writer.append("&#x2026; ");
			for (int i = pages - 7; i < pages; i++) {
				appendPageNumber(writer, i);
			}
		}
		
		if (position < (pages - 1)) {
			writeLinkHtml(writer, "dh-pager-link-next", position + 1, "Next");
		}
	}
	
	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}
	
	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
}

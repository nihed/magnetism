package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.dumbhippo.persistence.ApplicationCategory;
import com.dumbhippo.server.applications.ApplicationSystem;
import com.dumbhippo.server.applications.ApplicationView;
import com.dumbhippo.server.applications.CategoryView;
import com.dumbhippo.web.WebEJBUtil;

public class ApplicationsPage {
	private ApplicationSystem applicationSystem;
	public List<CategoryInfo> categories;
	public List<ApplicationView> popularApplications;
	private ApplicationCategory category;
	
	static final int ICON_SIZE = 48;
	static final int BAR_LENGTH = 80;
	
	static final RgbColor MIN_COLOR = new RgbColor(0xeb, 0xdc, 0xf3); 
	static final RgbColor MAX_COLOR = new RgbColor(0xa4, 0x5a, 0xc6); 
	
	public static class CategoryInfo {
		private ApplicationCategory category;
		private String color;
		private int length;
		
		public CategoryInfo(ApplicationCategory category, String color, int length) {
			this.category = category;
			this.color = color;
			this.length = length;
		}

		public ApplicationCategory getCategory() {
			return category;
		}

		public String getColor() {
			return color;
		}

		public int getLength() {
			return length;
		}
	}

	private static class RgbColor {
		private int r;
		private int g;
		private int b;
		
		RgbColor(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		static RgbColor interpolate(RgbColor color1, RgbColor color2, double factor) {
			return new RgbColor(
					(int)Math.round(color1.r * (1. - factor) + color2.r * factor),
					(int)Math.round(color1.g * (1. - factor) + color2.g * factor),
					(int)Math.round(color1.b * (1. - factor) + color2.b * factor)
			);
		}
		
		@Override
		public String toString() {
			return String.format("#%02x%02x%02x", r, g, b);
		}
	}
	
	public ApplicationsPage() {
		applicationSystem = WebEJBUtil.defaultLookup(ApplicationSystem.class);
	}
	
	public void setCategoryName(String categoryName) {
		for (ApplicationCategory c : ApplicationCategory.values()) {
			if (c.name().equalsIgnoreCase(categoryName)) {
				category = c;
				break;
			}
		}
	}
	
	public ApplicationCategory getCategory() {
		return category;
	}
	
	private Date getSince() {
		return new Date(System.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000L);
	}
	
	public List<CategoryInfo> getCategories() {
		if (categories == null) {
			List<CategoryView> views = applicationSystem.getPopularCategories(getSince());
			
			int maxUsage = 0;
			
			for (CategoryView categoryView : views) {
				if (categoryView.getUsageCount() > maxUsage)
					maxUsage = categoryView.getUsageCount();
			}

			categories = new ArrayList<CategoryInfo>();
			
			for (CategoryView categoryView : views) {
				double factor = (double)categoryView.getUsageCount() / maxUsage;
				categories.add(new CategoryInfo(categoryView.getCategory(),
												RgbColor.interpolate(MIN_COLOR, MAX_COLOR, factor).toString(),
												(int)Math.round(BAR_LENGTH * factor)));
			}
			
			Collections.sort(categories, new Comparator<CategoryInfo>() {
				public int compare(CategoryInfo a, CategoryInfo b) {
					ApplicationCategory ac = a.getCategory();
					ApplicationCategory bc = b.getCategory();
					
					// Sort OTHER after everything else
					if (ac.equals(ApplicationCategory.OTHER))
						return bc.equals(ApplicationCategory.OTHER) ? 0 : 1;
					else if (bc.equals(ApplicationCategory.OTHER))
						return -1;
					else
						return ac.getDisplayName().compareTo(bc.getDisplayName());
				}
			});
		}
		
		return categories;
	}
	
	public List<ApplicationView> getPopularApplications() {
		if (popularApplications == null) {
			popularApplications = applicationSystem.getPopularApplications(getSince(), ICON_SIZE, category);
		}
		
		return popularApplications;
	}
}

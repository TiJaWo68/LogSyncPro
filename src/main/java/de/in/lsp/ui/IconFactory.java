package de.in.lsp.ui;

import javax.swing.Icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Factory for creating and managing application icons.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class IconFactory {

	private static final String SELECTED_ICON_PATH = "icons/selected.svg";

	/**
	 * Creates an SVG icon with the specified path and size.
	 * 
	 * @param path The path to the SVG resource.
	 * @param size The size (width and height) of the icon.
	 * @return The icon, or null if it cannot be created.
	 */
	public static Icon getIcon(String path, int size) {
		try {
			return new FlatSVGIcon(path, size, size);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns the icon used for selected views.
	 * 
	 * @return The selected icon.
	 */
	public static Icon getSelectedIcon() {
		return getIcon(SELECTED_ICON_PATH, 16);
	}

	public static Icon getShowAllIcon() {
		return getIcon("icons/show_all.svg", 16);
	}
}

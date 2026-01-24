package de.in.lsp.ui;

import javax.swing.Icon;

/**
 * Defines the type of log view and its associated icon.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public enum ViewType {
	INTERNAL("icons/internal.svg"),
	MERGED("icons/logo.svg"),
	K8S("icons/network.svg"),
	TCP("icons/tcp.svg"),
	FILE("icons/file.svg");

	private final String iconPath;

	ViewType(String iconPath) {
		this.iconPath = iconPath;
	}

	public String getIconPath() {
		return iconPath;
	}

	/**
	 * Creates the icon for this view type.
	 * 
	 * @return The icon, or null if creation fails.
	 */
	public Icon getIcon() {
		return IconFactory.getIcon(iconPath, 16);
	}
}

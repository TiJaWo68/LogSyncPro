package de.in.lsp.ui;

/**
 * Listener interface for LogView status changes and focus events.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public interface LogViewListener {
    void onClose(LogView view);

    void onMinimize(LogView view);

    void onMaximize(LogView view);

    void onFocusGained(LogView view);
}

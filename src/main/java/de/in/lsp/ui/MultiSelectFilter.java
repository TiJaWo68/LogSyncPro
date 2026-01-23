package de.in.lsp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * A filter component that displays a button and opens a popup with checkboxes
 * for multi-selection.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class MultiSelectFilter extends JPanel {
    private final String title;
    private final Consumer<Set<String>> onSelectionChanged;
    private final Set<String> domainOptions = new HashSet<>(); // All options ever seen
    private final Set<String> availableOptions = new HashSet<>(); // Options currently visible
    private final Set<String> selectedOptions = new HashSet<>();
    private final JButton button;
    private JPopupMenu popup;
    private boolean showAllMode = false;

    public MultiSelectFilter(String title, Consumer<Set<String>> onSelectionChanged) {
        this.title = title;
        this.onSelectionChanged = onSelectionChanged;
        setLayout(new BorderLayout());

        button = new JButton(title);
        button.setFocusable(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> showPopup());
        add(button, BorderLayout.CENTER);
    }

    public void setShowAllMode(boolean showAllMode) {
        this.showAllMode = showAllMode;
    }

    public void setOptions(Set<String> options) {
        if (options.equals(availableOptions))
            return;

        boolean wasAllSelected = selectedOptions.containsAll(domainOptions) && !domainOptions.isEmpty();
        boolean selectionChanged = false;

        // Add to domain
        domainOptions.addAll(options);

        // Update available
        availableOptions.clear();
        availableOptions.addAll(options);

        if (wasAllSelected) {
            // Automatically select new options if we were in "Select All" state
            for (String opt : options) {
                if (selectedOptions.add(opt)) {
                    selectionChanged = true;
                }
            }
        } else {
            // Remove selected options that are no longer in the domain (rarely happens)
            selectionChanged = selectedOptions.retainAll(domainOptions);
        }

        // Default to "all selected" for new components
        if (selectedOptions.isEmpty() && !domainOptions.isEmpty()) {
            selectedOptions.addAll(domainOptions);
            selectionChanged = true;
        }

        updateButtonText();

        if (selectionChanged) {
            onSelectionChanged.accept(new HashSet<>(selectedOptions));
        }
    }

    private void showPopup() {
        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // "Select All" always refers to the FULL DOMAIN to act as a proper reset
        JCheckBox selectAll = new JCheckBox("Select All", selectedOptions.containsAll(domainOptions));
        selectAll.addActionListener(e -> {
            boolean selected = selectAll.isSelected();
            // User requirement: "Select All as Reset".
            // Select All selects EVERYTHING.
            if (selected) {
                selectedOptions.addAll(domainOptions);
            } else {
                selectedOptions.clear();
            }
            refreshCheckboxes();
            onSelectionChanged.accept(new HashSet<>(selectedOptions));
            updateButtonText();
        });
        headerPanel.add(selectAll, BorderLayout.CENTER);

        // "Show All" CheckBox for switching mode
        JCheckBox showAllCb = new JCheckBox("Show all");
        showAllCb.setToolTipText("Switch between showing matched options only and showing all options");
        showAllCb.setSelected(showAllMode);
        showAllCb.setFocusable(false);
        // Ensure popup doesn't close on click, similar to JMenu item handling but for
        // custom component
        showAllCb.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // consume to prevent auto-close if triggered by parent
            }
        });
        showAllCb.addActionListener(e -> {
            showAllMode = showAllCb.isSelected();
            rebuildPopupList();
        });
        headerPanel.add(showAllCb, BorderLayout.EAST);

        popup.add(headerPanel, BorderLayout.NORTH);
        popup.add(new javax.swing.JSeparator(), BorderLayout.CENTER); // Placeholder

        // Content placeholder
        rebuildPopupList();

        popup.show(button, 0, button.getHeight());
    }

    private void rebuildPopupList() {
        // Remove existing center component (content or no-options label)
        for (Component c : popup.getComponents()) {
            // Header is at NORTH, separators/content at CENTER
            if (c instanceof JPanel && ((JPanel) c).getLayout() instanceof BorderLayout) {
                continue;
            }
            popup.remove(c);
        }

        Set<String> optionsToShow = showAllMode ? domainOptions : availableOptions;

        if (optionsToShow.isEmpty()) {
            popup.add(new JMenuItem("No options available"), BorderLayout.CENTER);
        } else {
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);

            List<String> sortedOptions = new ArrayList<>(optionsToShow);
            java.util.Collections.sort(sortedOptions);

            for (String option : sortedOptions) {
                boolean isAvailable = availableOptions.contains(option);
                JCheckBox cb = new JCheckBox(option, selectedOptions.contains(option));

                // If showing all, style unavailable options
                if (showAllMode && !isAvailable) {
                    cb.setForeground(Color.GRAY);
                    cb.setFont(cb.getFont().deriveFont(Font.ITALIC));
                }

                cb.addActionListener(e -> {
                    if (cb.isSelected()) {
                        selectedOptions.add(option);
                    } else {
                        selectedOptions.remove(option);
                    }
                    updateSelectAllState();
                    onSelectionChanged.accept(new HashSet<>(selectedOptions));
                    updateButtonText();
                });
                contentPanel.add(cb);
            }

            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(contentPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            // Calculate proper height based on actual content
            // We need to ask the contentPanel for its preferred size after adding
            // components
            java.awt.Dimension contentSize = contentPanel.getPreferredSize();
            int totalHeight = contentSize.height;

            // Limit to ScreenHeight * 0.6 or 600px
            int maxHeight = 600;
            try {
                // Try to get screen height if graphic environment is available
                maxHeight = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height * 2 / 3;
            } catch (Exception e) {
                // Headless or error
            }

            int prefHeight = Math.min(totalHeight + 5, maxHeight); // +5 for borders/padding safety
            // Ensure min height for at least a few items if they exist but don't force it
            // if content is small
            if (optionsToShow.size() > 0)
                prefHeight = Math.max(prefHeight, 30); // Min reasonable height

            int width = Math.max(250, button.getWidth());
            scrollPane.setPreferredSize(new java.awt.Dimension(width, prefHeight));

            popup.add(scrollPane, BorderLayout.CENTER);
        }

        // Force resize of the popup window
        popup.revalidate();
        popup.repaint();
        // Pack the window to adapt to new size (Show All -> larger list)
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(popup);
        if (w != null) {
            w.pack();
        }
    }

    private void updateSelectAllState() {
        if (popup == null)
            return;
        for (Component c : popup.getComponents()) {
            if (c instanceof JPanel headerPanel) {
                for (Component hc : headerPanel.getComponents()) {
                    if (hc instanceof JCheckBox selectAllCb && "Select All".equals(selectAllCb.getText())) {
                        selectAllCb.setSelected(selectedOptions.containsAll(domainOptions));
                        return;
                    }
                }
            }
        }
    }

    private void refreshCheckboxes() {
        if (popup == null)
            return;

        for (Component c : popup.getComponents()) {
            if (c instanceof javax.swing.JScrollPane scrollPane) {
                JPanel viewportOnly = (JPanel) scrollPane.getViewport().getView();
                for (Component comp : viewportOnly.getComponents()) {
                    if (comp instanceof JCheckBox cb) {
                        cb.setSelected(selectedOptions.contains(cb.getText()));
                    }
                }
            }
        }
    }

    private void updateButtonText() {
        if (!isActive()) {
            button.setText(title);
            button.setIcon(null);
            button.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            button.setText(title + " (" + selectedOptions.size() + ")");
            button.setIcon(new FilterIcon(Color.GRAY));
            button.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    public Set<String> getSelectedOptions() {
        return new HashSet<>(selectedOptions);
    }

    public boolean isActive() {
        return !selectedOptions.isEmpty() && selectedOptions.size() < domainOptions.size();
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getAllOptions() {
        return new HashSet<>(domainOptions);
    }

    public void setSelectedOptions(Set<String> options) {
        this.selectedOptions.clear();
        this.selectedOptions.addAll(options);
        updateButtonText();
        onSelectionChanged.accept(new HashSet<>(selectedOptions));
    }
}

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
        popup.setLayout(new BoxLayout(popup, BoxLayout.Y_AXIS));

        // In showAllMode, we show domainOptions. Otherwise, we show availableOptions.
        Set<String> optionsToShow = showAllMode ? domainOptions : availableOptions;

        if (optionsToShow.isEmpty()) {
            popup.add(new JMenuItem("No options available"));
        } else {
            // "Select All" always refers to the FULL DOMAIN to act as a proper reset
            JCheckBox selectAll = new JCheckBox("Select All", selectedOptions.containsAll(domainOptions));
            selectAll.addActionListener(e -> {
                if (selectAll.isSelected()) {
                    selectedOptions.addAll(domainOptions);
                } else {
                    selectedOptions.clear();
                }
                refreshCheckboxes();
                onSelectionChanged.accept(new HashSet<>(selectedOptions));
                updateButtonText();
            });
            popup.add(selectAll);
            popup.addSeparator();

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
                    selectAll.setSelected(selectedOptions.containsAll(domainOptions));
                    onSelectionChanged.accept(new HashSet<>(selectedOptions));
                    updateButtonText();
                });
                popup.add(cb);
            }
        }

        popup.show(button, 0, button.getHeight());
    }

    private void refreshCheckboxes() {
        for (Component c : popup.getComponents()) {
            if (c instanceof JCheckBox cb && !cb.getText().equals("Select All")) {
                cb.setSelected(selectedOptions.contains(cb.getText()));
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

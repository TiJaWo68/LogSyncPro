package de.in.lsp.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A filter component that displays a button and opens a popup with checkboxes
 * for multi-selection.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class MultiSelectFilter extends JPanel {
    private final String title;
    private final Consumer<Set<String>> onSelectionChanged;
    private final Set<String> allOptions = new HashSet<>();
    private final Set<String> selectedOptions = new HashSet<>();
    private final JButton button;
    private JPopupMenu popup;

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

    public void setOptions(Set<String> options) {
        if (options.equals(allOptions))
            return;

        boolean wasAllSelected = selectedOptions.containsAll(allOptions) && !allOptions.isEmpty();
        boolean selectionChanged = false;

        // Keep track of what was selected if it still exists
        Set<String> oldSelected = new HashSet<>(selectedOptions);
        allOptions.clear();
        allOptions.addAll(options);
        selectedOptions.clear();

        for (String opt : allOptions) {
            if (oldSelected.contains(opt)) {
                selectedOptions.add(opt);
            } else if (wasAllSelected) {
                // If "Select All" was active, automatically select new options
                selectedOptions.add(opt);
                selectionChanged = true;
            }
        }

        // Check if any old options were removed that were part of selection
        if (!selectionChanged) {
            for (String opt : oldSelected) {
                if (!allOptions.contains(opt)) {
                    selectionChanged = true;
                    break;
                }
            }
        }

        // If nothing was selected before or new options appeared,
        // we might want to default to "all selected" for new components
        if (oldSelected.isEmpty() && !allOptions.isEmpty()) {
            selectedOptions.addAll(allOptions);
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

        if (allOptions.isEmpty()) {
            popup.add(new JMenuItem("No options available"));
        } else {
            JCheckBox selectAll = new JCheckBox("Select All", selectedOptions.size() == allOptions.size());
            selectAll.addActionListener(e -> {
                if (selectAll.isSelected()) {
                    selectedOptions.addAll(allOptions);
                } else {
                    selectedOptions.clear();
                }
                refreshCheckboxes();
                onSelectionChanged.accept(new HashSet<>(selectedOptions));
                updateButtonText();
            });
            popup.add(selectAll);
            popup.addSeparator();

            List<String> sortedOptions = new ArrayList<>(allOptions);
            java.util.Collections.sort(sortedOptions);

            for (String option : sortedOptions) {
                JCheckBox cb = new JCheckBox(option, selectedOptions.contains(option));
                cb.addActionListener(e -> {
                    if (cb.isSelected()) {
                        selectedOptions.add(option);
                    } else {
                        selectedOptions.remove(option);
                    }
                    selectAll.setSelected(selectedOptions.size() == allOptions.size());
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
        if (selectedOptions.size() == allOptions.size() || selectedOptions.isEmpty()) {
            button.setText(title);
            button.setIcon(null);
            button.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            button.setText(title + " (" + selectedOptions.size() + ")");
            button.setIcon(new FilterIcon(Color.GRAY));
            button.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    private static class FilterIcon implements Icon {
        private final Color color;

        FilterIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int[] px = { x + 2, x + 10, x + 10, x + 6, x + 6, x + 2 };
            int[] py = { y + 2, y + 2, y + 6, y + 10, y + 6, y + 6 };
            g2.fillPolygon(px, py, 6);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    public Set<String> getSelectedOptions() {
        return new HashSet<>(selectedOptions);
    }

    public boolean isActive() {
        return !selectedOptions.isEmpty() && selectedOptions.size() < allOptions.size();
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getAllOptions() {
        return new HashSet<>(allOptions);
    }

    /**
     * Programmatically sets the selection and triggers the callback.
     * Useful for testing.
     */
    public void setSelectedOptions(Set<String> options) {
        this.selectedOptions.clear();
        this.selectedOptions.addAll(options);
        updateButtonText();
        onSelectionChanged.accept(new HashSet<>(selectedOptions));
    }
}

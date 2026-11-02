// FILE: src/main/java/com/streakkeeper/view/Sidebar.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import com.streakkeeper.model.Counter;
import com.streakkeeper.model.StreakModel;
import com.streakkeeper.util.AnimationUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Collapsible list of counters. Width is animated with a Swing Timer
 * (eased in/out, ~250 ms). The visual state is driven entirely by the model's
 * {@code sidebarExpanded} flag.
 */
public class Sidebar extends JPanel {

    private static final int EXPANDED_WIDTH  = 240;
    private static final int COLLAPSED_WIDTH = 0;
    private static final int SLIDE_MS        = 250;

    private final DefaultListModel<Counter> listModel = new DefaultListModel<>();
    private final JList<Counter> list = new JList<>(listModel);
    private final JButton newBtn = new JButton("+ New Counter");

    private Consumer<String> onCounterSelected;
    private Runnable onNewCounter;
    private Consumer<String> onRename;
    private Consumer<String> onDelete;

    private boolean expanded = true;
    private int currentWidth = EXPANDED_WIDTH;
    private Timer slideTimer;
    private boolean updatingFromModel = false;

    public Sidebar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                javax.swing.UIManager.getColor("Separator.foreground")));
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CounterRenderer());
        list.setFixedCellHeight(56);
        list.addListSelectionListener(e -> {
            if (updatingFromModel || e.getValueIsAdjusting()) return;
            Counter c = list.getSelectedValue();
            if (c != null && onCounterSelected != null) onCounterSelected.accept(c.getId());
        });

        // Context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename…");
        JMenuItem deleteItem = new JMenuItem("Delete…");
        renameItem.addActionListener(e -> {
            Counter c = list.getSelectedValue();
            if (c != null && onRename != null) onRename.accept(c.getId());
        });
        deleteItem.addActionListener(e -> {
            Counter c = list.getSelectedValue();
            if (c != null && onDelete != null) onDelete.accept(c.getId());
        });
        popup.add(renameItem);
        popup.add(deleteItem);
        list.setComponentPopupMenu(popup);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bottom.add(newBtn, BorderLayout.CENTER);
        newBtn.addActionListener(e -> { if (onNewCounter != null) onNewCounter.run(); });

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------- wiring

    public void setOnCounterSelected(Consumer<String> c) { this.onCounterSelected = c; }
    public void setOnNewCounter(Runnable r)              { this.onNewCounter = r; }
    public void setOnRename(Consumer<String> c)          { this.onRename = c; }
    public void setOnDelete(Consumer<String> c)          { this.onDelete = c; }

    // --------------------------------------------------------- model sync

    /** Rebuilds the list from the model, then restores selection. */
    public void updateFromModel(StreakModel model) {
        updatingFromModel = true;
        try {
            listModel.clear();
            for (Counter c : model.getCounters()) listModel.addElement(c);

            Counter selected = model.getSelectedCounter();
            if (selected != null) {
                list.setSelectedValue(selected, true);
            } else {
                list.clearSelection();
            }
        } finally {
            updatingFromModel = false;
        }
    }

    // --------------------------------------------------------- animations

    /** Sets expansion state immediately (no animation). Used during startup. */
    public void setExpandedInstant(boolean expanded) {
        this.expanded = expanded;
        this.currentWidth = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        setPreferredSize(new Dimension(currentWidth, 0));
        setVisible(expanded);
        revalidate();
        if (getParent() != null) getParent().revalidate();
    }

    /** Sets expansion state with a slide animation. */
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        int target = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        if (expanded) setVisible(true);
        animateWidth(target);
    }

    private void animateWidth(int targetWidth) {
        final int from = currentWidth;
        final long startNanos = System.nanoTime();
        if (slideTimer != null) slideTimer.stop();
        slideTimer = new Timer(16, null);
        slideTimer.addActionListener(e -> {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            double t = Math.min(1.0, elapsedMs / (double) SLIDE_MS);
            double eased = AnimationUtils.easeInOutCubic(t);
            currentWidth = (int) Math.round(from + (targetWidth - from) * eased);
            setPreferredSize(new Dimension(currentWidth, 0));
            revalidate();
            if (getParent() != null) getParent().revalidate();
            repaint();
            if (t >= 1.0) {
                currentWidth = targetWidth;
                if (targetWidth == COLLAPSED_WIDTH) setVisible(false);
                slideTimer.stop();
            }
        });
        slideTimer.start();
    }

    // ---------------------------------------------------------- rendering

    private static final class CounterRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Counter c) {
                setText("<html><b>" + escapeHtml(c.getName()) + "</b><br>"
                        + "<small>" + c.getCurrentValue() + " &middot; "
                        + escapeHtml(c.getDaysAgo()) + "</small></html>");
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            }
            return this;
        }

        private static String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
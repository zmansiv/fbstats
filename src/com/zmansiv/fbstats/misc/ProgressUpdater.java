package com.zmansiv.fbstats.misc;

import javax.swing.*;

public class ProgressUpdater {

    private final Runnable onFinish;
    private final JLabel[] labels;

    public ProgressUpdater(Runnable onFinish, JLabel... labels) {
        this.onFinish = onFinish;
        this.labels = labels;
    }

    public void update(final String text, final int level) {
        final StringBuilder sb = new StringBuilder();
        if (!"".equals(text)) {
            for (int i = 0; i < level; i++) {
                sb.append("-");
            }
        }
        final String s = sb.append(text).toString();
        if (level < labels.length) {
            try {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        labels[level].setText(s);
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(s);
        }
    }

    public void finish() {
        try {
            SwingUtilities.invokeLater(onFinish);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
package com.zmansiv.fbstats.ui;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

public abstract class FSFrame extends JFrame {

    {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static final String RESOURCE_FOLDER = "./resources/", ICON_IMG = "icon.png", CLOSE_IMG = "close.png", CLOSE_HOVER_IMG = "close_hover.png", CLOSE_PRESSED_IMG = "close_pressed.png", MINIMIZE_IMG = "minimize.png", MINIMIZE_HOVER_IMG = "minimize_hover.png", MINIMIZE_PRESSED_IMG = "minimize_pressed.png";
    private JComponent titlePane;
    private JLabel titleLabel;
    protected ImageIcon icon;

    public FSFrame() {
        this("FBStats");
    }

    public FSFrame(String title) {
        this(title, true);
    }

    public FSFrame(String title, boolean createContent) {
        super(title);
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        icon = new ImageIcon(RESOURCE_FOLDER + ICON_IMG);
        setIconImage(icon.getImage());
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        titlePane = createTitlePane();
        add(titlePane, BorderLayout.NORTH);
        if (createContent) {
            init();
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        titleLabel.setText(title);
    }

    private JComponent createTitlePane() {
        JComponent result = new JComponent() {

            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                LinearGradientPaint paint = new LinearGradientPaint(0, 0, 0, getHeight(), new float[]{.0f, .499f, .5f, 1.0f}, new Color[]{new Color(0x858585), new Color(0x3c3c3c), new Color(0x2c2c2c), new Color(0x333334)});
                g2.setPaint(paint);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() + 1, getHeight(), 20, 20));
                g2.fill(new Rectangle(0, getHeight() / 2, getWidth() + 1, getHeight() / 2 + 1));
                g2.dispose();
            }
        };
        result.setLayout(new BorderLayout());
        titleLabel = new JLabel(getTitle());
        titleLabel.setIcon(icon);
        titleLabel.setForeground(new Color(255, 255, 255, 200));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 0, 0));
        result.add(titleLabel, BorderLayout.WEST);
        JButton closeButton = new JButton();
        closeButton.setIcon(new ImageIcon(RESOURCE_FOLDER + CLOSE_IMG));
        closeButton.setRolloverIcon(new ImageIcon(RESOURCE_FOLDER + CLOSE_HOVER_IMG));
        closeButton.setPressedIcon(new ImageIcon(RESOURCE_FOLDER + CLOSE_PRESSED_IMG));
        closeButton.setFocusable(false);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 3));
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FSFrame.this.processWindowEvent(new WindowEvent(FSFrame.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        JButton minimizeButton = new JButton();
        minimizeButton.setIcon(new ImageIcon(RESOURCE_FOLDER + MINIMIZE_IMG));
        minimizeButton.setRolloverIcon(new ImageIcon(RESOURCE_FOLDER + MINIMIZE_HOVER_IMG));
        minimizeButton.setPressedIcon(new ImageIcon(RESOURCE_FOLDER + MINIMIZE_PRESSED_IMG));
        minimizeButton.setFocusable(false);
        minimizeButton.setFocusPainted(false);
        minimizeButton.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 3));
        minimizeButton.setContentAreaFilled(false);
        minimizeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FSFrame.this.setState(Frame.ICONIFIED);
            }
        });
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BorderLayout());
        buttons.add(minimizeButton, BorderLayout.WEST);
        buttons.add(closeButton, BorderLayout.EAST);
        result.add(buttons, BorderLayout.EAST);
        MouseInputHandler handler = new MouseInputHandler();
        result.addMouseListener(handler);
        result.addMouseMotionListener(handler);
        return result;
    }

    protected void init() {
        JPanel contentPane = new JPanel() {

            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 75));
                g2.drawLine(0, 0, 0, getHeight());
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }

        };
        contentPane.setLayout(null);
        createContent(contentPane);
        add(contentPane, BorderLayout.CENTER);
        setSize(contentPane.getWidth(), contentPane.getHeight() + titlePane.getHeight());
        setVisible(true);
        Container parent = this.getParent();
        if (parent != null) {
            setLocationRelativeTo(parent);
        } else {
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((dim.width - getSize().width) / 2, (dim.height - getSize().height) / 2);
        }
    }

    protected abstract void createContent(JPanel pane);

    private class MouseInputHandler extends MouseInputAdapter {

        private int dragOffsetX, dragOffsetY;
        private static final int BORDER_DRAG_THICKNESS = 5;
        private JFrame f = FSFrame.this;

        public void mousePressed(MouseEvent ev) {
            f.toFront();
            Point dragWindowOffset = ev.getPoint();
            int frameState = f.getExtendedState();
            if ((frameState & Frame.MAXIMIZED_BOTH) == 0 && dragWindowOffset.y >= BORDER_DRAG_THICKNESS && dragWindowOffset.x >= BORDER_DRAG_THICKNESS && dragWindowOffset.x < f.getWidth() - BORDER_DRAG_THICKNESS) {
                dragOffsetX = dragWindowOffset.x;
                dragOffsetY = dragWindowOffset.y;
            }
        }

        public void mouseDragged(MouseEvent ev) {
            Point windowPt = MouseInfo.getPointerInfo().getLocation();
            windowPt.x -= dragOffsetX;
            windowPt.y -= dragOffsetY;
            f.setLocation(windowPt);
        }
    }

}
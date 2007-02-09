/**
 * $RCSfile$
 * $Revision: 3054 $
 * $Date: 2005-11-10 21:08:33 -0300 (Thu, 10 Nov 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.launcher;

import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;
import org.jivesoftware.util.WebManager;
import org.jivesoftware.util.XMLProperties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Graphical launcher for Wildfire.
 *
 * @author Matt Tucker
 */
public class Launcher {

    private String appName;
    private File binDir;
    private Process wildfired;
    private String configFile;
    private JPanel toolbar = new JPanel();

    private ImageIcon offIcon;
    private ImageIcon onIcon;
    private TrayIcon trayIcon;
    private JFrame frame;
    private JPanel cardPanel = new JPanel();
    private CardLayout cardLayout = new CardLayout();

    private JTextPane pane;
    private boolean freshStart = true;

    /**
     * Creates a new Launcher object.
     */
    public Launcher() {
        // Initialize the SystemTray now (to avoid a bug!)
        SystemTray tray = null;
        try {
            tray = SystemTray.getDefaultSystemTray();
        }
        catch (Throwable e) {
            // Log to System error instead of standard error log.
            System.err.println("Error loading system tray library, system tray support disabled.");
        }
        
        // Use the native look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (System.getProperty("app.name") != null) {
            appName = System.getProperty("app.name");
        }
        else {
            appName = "Wildfire";
        }

        binDir = new File("").getAbsoluteFile();
        // See if the appdir property is set. If so, use it to find the executable.
        if (System.getProperty("appdir") != null) {
            binDir = new File(System.getProperty("appdir"));
        }

        configFile = new File(new File(binDir.getParent(), "conf"),
                "wildfire.xml").getAbsolutePath();

        frame = new DroppableFrame() {
            public void fileDropped(File file) {
                String fileName = file.getName();
                if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
                    installPlugin(file);
                }
            }
        };

        frame.setTitle(appName);

        ImageIcon splash = null;
        JPanel mainPanel = new JPanel();
        JLabel splashLabel = null;

        cardPanel.setLayout(cardLayout);

        // Set the icon.
        try {
            splash = new ImageIcon(getClass().getClassLoader().getResource("splash.gif"));
            splashLabel = new JLabel("", splash, JLabel.CENTER);

            onIcon = new ImageIcon(getClass().getClassLoader().getResource("wildfire_on-16x16.gif"));
            offIcon = new ImageIcon(getClass().getClassLoader().getResource("wildfire_off-16x16.gif"));
            frame.setIconImage(offIcon.getImage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        mainPanel.setLayout(new BorderLayout());
        cardPanel.setBackground(Color.white);

        // Add buttons
        final JButton startButton = new JButton("Start");
        startButton.setActionCommand("Start");

        final JButton stopButton = new JButton("Stop");
        stopButton.setActionCommand("Stop");

        final JButton browserButton = new JButton("Launch Admin");
        browserButton.setActionCommand("Launch Admin");

        final JButton quitButton = new JButton("Quit");
        quitButton.setActionCommand("Quit");

        toolbar.setLayout(new GridBagLayout());
        toolbar.add(startButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        toolbar.add(stopButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        toolbar.add(browserButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        toolbar.add(quitButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        mainPanel.add(toolbar, BorderLayout.SOUTH);

        // create the main menu of the system tray icon
        JPopupMenu menu = new JPopupMenu(appName + " Menu");

        final JMenuItem showMenuItem = new JMenuItem("Hide");
        showMenuItem.setActionCommand("Hide/Show");
        menu.add(showMenuItem);

        final JMenuItem startMenuItem = new JMenuItem("Start");
        startMenuItem.setActionCommand("Start");
        menu.add(startMenuItem);

        final JMenuItem stopMenuItem = new JMenuItem("Stop");
        stopMenuItem.setActionCommand("Stop");
        menu.add(stopMenuItem);

        final JMenuItem browserMenuItem = new JMenuItem("Launch Admin");
        browserMenuItem.setActionCommand("Launch Admin");
        menu.add(browserMenuItem);

        menu.addSeparator();

        final JMenuItem quitMenuItem = new JMenuItem("Quit");
        quitMenuItem.setActionCommand("Quit");
        menu.add(quitMenuItem);

        browserButton.setEnabled(false);
        stopButton.setEnabled(false);
        browserMenuItem.setEnabled(false);
        stopMenuItem.setEnabled(false);

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("Start".equals(e.getActionCommand())) {
                    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    // Adjust button and menu items.
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    startMenuItem.setEnabled(false);
                    stopMenuItem.setEnabled(true);

                    // Startup Application
                    startApplication();

                    // Change to the "on" icon.
                    frame.setIconImage(onIcon.getImage());
                    trayIcon.setIcon(onIcon);

                    // Start a thread to enable the admin button after 8 seconds.
                    Thread thread = new Thread() {
                        public void run() {
                            try {
                                sleep(8000);
                            }
                            catch (InterruptedException ie) {
                                // Ignore.
                            }
                            // Enable the Launch Admin button/menu item only if the
                            // server has started.
                            if (stopButton.isEnabled()) {
                                browserButton.setEnabled(true);
                                browserMenuItem.setEnabled(true);
                                frame.setCursor(Cursor.getDefaultCursor());
                            }
                        }
                    };
                    thread.start();
                }
                else if ("Stop".equals(e.getActionCommand())) {
                    stopApplication();
                    // Change to the "off" button.
                    frame.setIconImage(offIcon.getImage());
                    trayIcon.setIcon(offIcon);
                    // Adjust buttons and menu items.
                    frame.setCursor(Cursor.getDefaultCursor());
                    browserButton.setEnabled(false);
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    browserMenuItem.setEnabled(false);
                    startMenuItem.setEnabled(true);
                    stopMenuItem.setEnabled(false);
                }
                else if ("Launch Admin".equals(e.getActionCommand())) {
                    launchBrowser();
                }
                else if ("Quit".equals(e.getActionCommand())) {
                    stopApplication();
                    System.exit(0);
                }
                else if ("Hide/Show".equals(e.getActionCommand()) || "PressAction".equals(e.getActionCommand())) {
                    // Hide/Unhide the window if the user clicked in the system tray icon or
                    // selected the menu option
                    if (frame.isVisible()) {
                        frame.setVisible(false);
                        frame.setState(Frame.ICONIFIED);
                        showMenuItem.setText("Show");
                    }
                    else {
                        frame.setVisible(true);
                        frame.setState(Frame.NORMAL);
                        showMenuItem.setText("Hide");
                    }
                }
            }
        };

        // Register a listener for the radio buttons.
        startButton.addActionListener(actionListener);
        stopButton.addActionListener(actionListener);
        browserButton.addActionListener(actionListener);
        quitButton.addActionListener(actionListener);

        // Register a listener for the menu items.
        quitMenuItem.addActionListener(actionListener);
        browserMenuItem.addActionListener(actionListener);
        stopMenuItem.addActionListener(actionListener);
        startMenuItem.addActionListener(actionListener);
        showMenuItem.addActionListener(actionListener);

        // Set the system tray icon with the menu
        trayIcon = new TrayIcon(offIcon, appName, menu);
        trayIcon.setIconAutoSize(true);
        trayIcon.addActionListener(actionListener);

        if (tray != null) {
            tray.addTrayIcon(trayIcon);
        }

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stopApplication();
                System.exit(0);
            }

            public void windowIconified(WindowEvent e) {
                // Make the window disappear when minimized
                frame.setVisible(false);
                showMenuItem.setText("Show");
            }
        });

        cardPanel.add("main", splashLabel);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.pack();

        frame.setSize(400, 300);
        frame.setResizable(true);

        GraphicUtils.centerWindowOnScreen(frame);

        frame.setVisible(true);

        // Setup command area
        final ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("splash2.gif"));
        pane = new DroppableTextPane() {
            public void paintComponent(Graphics g) {
                final Dimension size = pane.getSize();

                int x = (size.width - icon.getIconWidth()) / 2;
                int y = (size.height - icon.getIconHeight()) / 2;
                //  Approach 1: Dispaly image at at full size
                g.setColor(Color.white);
                g.fillRect(0, 0, size.width, size.height);
                g.drawImage(icon.getImage(), x, y, null);


                setOpaque(false);
                super.paintComponent(g);
            }

            public void fileDropped(File file) {
                String fileName = file.getName();
                if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
                    installPlugin(file);
                }
            }
        };

        pane.setEditable(false);

        final JPanel bevelPanel = new JPanel();
        bevelPanel.setBackground(Color.white);
        bevelPanel.setLayout(new BorderLayout());
        bevelPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        bevelPanel.add(new JScrollPane(pane), BorderLayout.CENTER);
        cardPanel.add("running", bevelPanel);

        // Start the app.
        startButton.doClick();
    }

    /**
     * Creates a new GUI launcher instance.
     */
    public static void main(String[] args) {
        new Launcher();
    }

    private synchronized void startApplication() {
        if (wildfired == null) {
            try {
                File windowsExe = new File(binDir, "wildfired.exe");
                File unixExe = new File(binDir, "wildfired");
                if (windowsExe.exists()) {
                    wildfired = Runtime.getRuntime().exec(new String[]{windowsExe.toString()});
                }
                else if (unixExe.exists()) {
                    wildfired = Runtime.getRuntime().exec(new String[]{unixExe.toString()});
                }
                else {
                    throw new FileNotFoundException();
                }
            }
            catch (Exception e) {
                // Try one more time using the jar and hope java is on the path
                try {
                    File libDir = new File(binDir.getParentFile(), "lib").getAbsoluteFile();
                    wildfired = Runtime.getRuntime().exec(new String[]{
                        "java", "-jar", new File(libDir, "startup.jar").toString()
                    });
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Launcher could not start,\n" + appName,
                            "File not found", JOptionPane.ERROR_MESSAGE);
                }
            }

            final SimpleAttributeSet styles = new SimpleAttributeSet();
            SwingWorker inputWorker = new SwingWorker() {
                public Object construct() {
                    if (wildfired != null) {
                        try {
                            // Get the input stream and read from it
                            InputStream in = wildfired.getInputStream();
                            int c;
                            while ((c = in.read()) != -1) {
                                try {
                                    StyleConstants.setFontFamily(styles, "courier new");
                                    pane.getDocument().insertString(pane.getDocument().getLength(),
                                            "" + (char)c, styles);
                                }
                                catch (BadLocationException e) {
                                    // Ignore.
                                }
                            }
                            in.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return "ok";
                }
            };
            inputWorker.start();


            SwingWorker errorWorker = new SwingWorker() {
                public Object construct() {
                    if (wildfired != null) {
                        try {
                            // Get the input stream and read from it
                            InputStream in = wildfired.getErrorStream();
                            int c;
                            while ((c = in.read()) != -1) {
                                try {
                                    StyleConstants.setForeground(styles, Color.red);
                                    pane.getDocument().insertString(pane.getDocument().getLength(), "" + (char)c, styles);
                                }
                                catch (BadLocationException e) {
                                    // Ignore.
                                }
                            }
                            in.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return "ok";
                }
            };
            errorWorker.start();

            if (freshStart) {
                try {
                    Thread.sleep(1000);
                    cardLayout.show(cardPanel, "running");
                }
                catch (Exception ex) {
                    // Ignore.
                }
                freshStart = false;
            }
            else {
                 // Clear Text
                pane.setText("");
                cardLayout.show(cardPanel, "running");
            }

        }
    }

    private synchronized void stopApplication() {
        if (wildfired != null) {
            try {
                wildfired.destroy();
                wildfired.waitFor();
                cardLayout.show(cardPanel, "main");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        wildfired = null;
    }

    private synchronized void launchBrowser() {
        try {
            XMLProperties props = new XMLProperties(configFile);
            String port = props.getProperty("adminConsole.port");
            String securePort = props.getProperty("adminConsole.securePort");
            if ("-1".equals(port)) {
                BrowserLauncher.openURL("https://127.0.0.1:" + securePort + "/index.html");
            }
            else {
                BrowserLauncher.openURL("http://127.0.0.1:" + port + "/index.html");
            }
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), configFile + " " + e.getMessage());
        }
    }

    private void installPlugin(final File plugin) {
        final JDialog dialog = new JDialog(frame, "Installing Plugin", true);
        dialog.getContentPane().setLayout(new BorderLayout());
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setString("Installing Plugin.  Please wait...");
        bar.setStringPainted(true);
        dialog.getContentPane().add(bar, BorderLayout.CENTER);
        dialog.pack();
        dialog.setSize(225, 55);

        final SwingWorker installerThread = new SwingWorker() {
            public Object construct() {
                File pluginsDir = new File(binDir.getParentFile(), "plugins");
                String tempName = plugin.getName() + ".part";
                File tempPluginsFile = new File(pluginsDir, tempName);

                File realPluginsFile = new File(pluginsDir, plugin.getName());

                // Copy Plugin into Dir.
                try {
                    // Just for fun. Show no matter what for two seconds.
                    Thread.sleep(2000);

                    WebManager.copy(plugin.toURL(), tempPluginsFile);

                    // If successfull, rename to real plugin name.
                    tempPluginsFile.renameTo(realPluginsFile);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return realPluginsFile;
            }

            public void finished() {
                dialog.setVisible(false);
            }
        };

        // Start installation
        installerThread.start();

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }
}
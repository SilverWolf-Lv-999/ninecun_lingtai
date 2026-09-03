package net.seraphina.ninecun_lingtai.client.utility;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

public final class SwingUtility {
    private static final String OPTIFINE_WARNING_TITLE = "九寸灵台 - 兼容性警告";
    private static final String OPTIFINE_WARNING_MESSAGE = "<html>"
            + "<div style='text-align:center;'>"
            + "检测到当前环境中存在 OptiFine。<br>"
            + "九寸灵台与 OptiFine 不兼容，请删除 OptiFine 后重新启动游戏。<br><br>"
            + "OptiFine was detected. Ninecun Lingtai is incompatible with OptiFine.<br>"
            + "Please remove OptiFine and restart the game."
            + "</div>"
            + "</html>";

    public static JWindow createWindow() {
        JWindow window = new JWindow();
        window.setAlwaysOnTop(true);
        window.setFocusableWindowState(true);
        window.setContentPane(createWarningPanel(window));
        window.pack();
        window.setMinimumSize(new Dimension(520, 190));
        window.setLocationRelativeTo(null);
        return window;
    }

    public static JFrame createFrame() {
        JFrame frame = new JFrame(OPTIFINE_WARNING_TITLE);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setContentPane(createWarningPanel(frame));
        frame.pack();
        frame.setMinimumSize(new Dimension(520, 190));
        frame.setLocationRelativeTo(null);
        return frame;
    }

    private static JPanel createWarningPanel(java.awt.Window window) {
        JLabel messageLabel = new JLabel(OPTIFINE_WARNING_MESSAGE, SwingConstants.CENTER);
        messageLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));

        JButton closeButton = new JButton("我已了解 / I understand");
        closeButton.addActionListener(event -> window.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    public static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }
}

package applications;

import javax.swing.SwingUtilities;

public class SwingUITest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingUI());
    }
}
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client implements Runnable {

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private JTextField inputField;
    private JTextPane chatPane;
    private StyledDocument chatDoc;
    private SimpleAttributeSet serverStyle;
    private SimpleAttributeSet clientStyle;
    private JLabel statusLabel;
    private JPanel statusPanel;
    private Font chatFont;
    private Font PFont;

    private final Color lightGray = new Color(191, 191, 191);
    private final Color primaryColor = new Color(153, 0, 51);
    private final Color backgroundColor = new Color(191, 191, 191);
    private final Color chatBackgroundColor = new Color(191, 191, 191);
    private final Color statusActiveColor = new Color(0, 128, 64);
    private final Color statusOfflineColor = Color.RED;
    private final Color serverMessageColor = new Color(153, 0, 51);
    private final Color clientMessageColor = new Color(255, 255, 255);

    private final String FPath = "resources/fonts/semi.ttf";
    private final int serverPort = 12000;


    public Client() {
        try {
            socket = new Socket("localhost", serverPort);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            try {
                chatFont = loadFont(FPath, 18);
                PFont = loadFont(FPath, 18);
            } catch (FontFormatException e) {
                e.printStackTrace();
                chatFont = new Font("SansSerif", Font.PLAIN, 16);
                PFont = new Font("SansSerif", Font.PLAIN, 12);
            }

            SwingUtilities.invokeLater(this::createGUI);

            new Thread(this).start();

            SwingUtilities.invokeLater(() -> updateStatus("Active", statusActiveColor));

        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> updateStatus("Offline", statusOfflineColor));
        }
    }

    private Font loadFont(String fontPath, float size) throws FontFormatException, IOException {
        File fontFile = new File(fontPath);
        if (fontFile.exists()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(size);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            return font;
        } else {
            throw new IOException("Font file not found at " + fontPath);
        }
    }

    private void createGUI() {
        JFrame frame = new JFrame("Client Chat");
        frame.setSize(500, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        statusLabel = new JLabel("Connecting...", SwingConstants.LEFT);
        statusLabel.setFont(chatFont);
        statusLabel.setForeground(statusOfflineColor);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(backgroundColor);
        statusLabel.setPreferredSize(new Dimension(200, 30));

        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(backgroundColor);
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Menu");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(PFont);
        chatPane.setBackground(chatBackgroundColor);
        chatDoc = chatPane.getStyledDocument();

        serverStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(serverStyle, serverMessageColor);

        clientStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(clientStyle, clientMessageColor);

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        inputField = new JTextField();
        inputField.setFont(chatFont);
        inputField.setBackground(lightGray);
        inputField.setForeground(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(lightGray.darker(), 1));
        inputField.addActionListener(e -> sendMessage(inputField.getText()));

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sendButton.setBackground(primaryColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sendButton.addActionListener(e -> sendMessage(inputField.getText()));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(backgroundColor);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(statusPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void sendMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            try {
                dataOutputStream.writeUTF(message);
                appendMessage("You", message, clientStyle);
                inputField.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void appendMessage(String sender, String message, SimpleAttributeSet style) {
        SwingUtilities.invokeLater(() -> {
            try {
                chatDoc.insertString(chatDoc.getLength(), sender + ": " + message + "\n", style);
                chatPane.setCaretPosition(chatDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateStatus(String status, Color textColor) {
        statusLabel.setText(status);
        statusLabel.setForeground(textColor);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = dataInputStream.readUTF();
                appendMessage("Server", message, serverStyle);
            }
        } catch (IOException e) {
            appendMessage("Connection", "Disconnected.", serverStyle);
            updateStatus("Offline", statusOfflineColor);
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}

package applications;

import chess.*;
import exceptions.ChessException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class SwingUI extends JFrame {
    private static final java.awt.Color LIGHT_SQUARE = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color DARK_SQUARE = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color HIGHLIGHT_COLOR = new java.awt.Color(124, 192, 203, 170);
    private static final java.awt.Color MOVABLE_PIECE_HIGHLIGHT = new java.awt.Color(152, 251, 152, 170); // Light green with transparency
    private static final java.awt.Color AI_THINKING_COLOR = new java.awt.Color(255, 165, 0, 100); // Orange with transparency
    private static final int MIN_SQUARE_SIZE = 30;
    private static final int PREFERRED_SQUARE_SIZE = 60;

    private ChessMatch chessMatch;
    private ChessPosition sourcePosition;
    private ChessPosition targetPosition;
    private List<ChessPiece> capturedPieces;
    private ChessBoardPanel boardPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel capturedPanel;
    private JPanel whiteCapturedPanel;
    private JPanel blackCapturedPanel;

    // AI related fields
    private boolean aiModeEnabled = false;
    private ChessAI chessAI;
    private boolean aiIsThinking = false;
    private JCheckBoxMenuItem aiModeMenuItem;

    public SwingUI() {
        chessMatch = new ChessMatch();
        capturedPieces = new ArrayList<>();
        sourcePosition = null;
        chessAI = new ChessAI(); // Initialize the AI

        setTitle("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        // Set minimum size for the window
        setMinimumSize(new Dimension(400, 450));

        // Create menu bar
        createMenuBar();

        // Create chess board panel
        boardPanel = new ChessBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        // Create status panel
        statusPanel = new JPanel();
        statusPanel.setLayout(new BorderLayout());
        statusLabel = new JLabel("Turn: 1 - WHITE");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.NORTH);

        // Create captured pieces panel
        capturedPanel = new JPanel();
        capturedPanel.setLayout(new GridLayout(2, 1));

        whiteCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        javax.swing.border.TitledBorder whiteTitle = BorderFactory.createTitledBorder("Captured White");
        whiteTitle.setTitleColor(new java.awt.Color(80, 60, 40));
        whiteCapturedPanel.setBorder(whiteTitle);
        whiteCapturedPanel.setBackground(LIGHT_SQUARE);
        whiteCapturedPanel.setOpaque(true);

        blackCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        javax.swing.border.TitledBorder blackTitle = BorderFactory.createTitledBorder("Captured Black");
        blackTitle.setTitleColor(new java.awt.Color(245, 245, 245));
        blackCapturedPanel.setBorder(blackTitle);
        blackCapturedPanel.setBackground(DARK_SQUARE);
        blackCapturedPanel.setOpaque(true);

        capturedPanel.add(whiteCapturedPanel);
        capturedPanel.add(blackCapturedPanel);
        add(capturedPanel, BorderLayout.SOUTH);

        // Add window resize listener to update the board when window is resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                boardPanel.repaint();
            }
        });

        // Set preferred size and pack
        setPreferredSize(new Dimension(600, 650));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class ChessBoardPanel extends JPanel {
        private boolean[][] possibleMoves;
        private boolean[][] movablePieces; // Tracks squares with pieces that can move
        private int squareSize;
        private int labelMargin; // space around the board to draw coordinates
        private int offsetX; // top-left X of the board drawing area inside this panel
        private int offsetY; // top-left Y of the board drawing area inside this panel

        private int getSquareSize() {
            int width = getWidth();
            int height = getHeight();
            return Math.max(MIN_SQUARE_SIZE, Math.min(width / 8, height / 8));
        }

        private void updateSquareSize() {
            int width = getWidth();
            int height = getHeight();

            // Base tentative square size ignoring margins first
            int tentative = Math.max(MIN_SQUARE_SIZE, Math.min((width - 40) / 8, (height - 40) / 8));
            // Margin for labels outside the board
            labelMargin = Math.max(14, tentative / 3);

            // Compute square size considering margins
            int boardAvailW = Math.max(0, width - 2 * labelMargin);
            int boardAvailH = Math.max(0, height - 2 * labelMargin);
            squareSize = Math.max(MIN_SQUARE_SIZE, Math.min(boardAvailW / 8, boardAvailH / 8));

            // Center the board area inside the panel while keeping at least labelMargin around
            int boardPixelW = 8 * squareSize;
            int boardPixelH = 8 * squareSize;
            offsetX = Math.max(labelMargin, (width - boardPixelW) / 2);
            offsetY = Math.max(labelMargin, (height - boardPixelH) / 2);
        }

        public ChessBoardPanel() {
            // Add extra space for labels around the board in preferred/min sizes
            int extra = 80; // 40px per side initially
            setPreferredSize(new Dimension(8 * PREFERRED_SQUARE_SIZE + extra, 8 * PREFERRED_SQUARE_SIZE + extra));
            setMinimumSize(new Dimension(8 * MIN_SQUARE_SIZE + extra, 8 * MIN_SQUARE_SIZE + extra));
            possibleMoves = new boolean[8][8];
            movablePieces = new boolean[8][8];
            squareSize = PREFERRED_SQUARE_SIZE;

            // Initialize by finding all movable pieces for the current player
            updateMovablePieces();

            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    updateSquareSize();
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Do not allow interactions if the game is over (checkmate or stalemate)
                    if (chessMatch.getCheckMate() || chessMatch.getStalemate()) {
                        return;
                    }
                    updateSquareSize(); // Ensure we have the latest square size
                    int localX = e.getX() - offsetX;
                    int localY = e.getY() - offsetY;

                    // Ignore clicks outside the board area
                    if (localX < 0 || localY < 0) return;
                    int col = localX / squareSize;
                    int row = localY / squareSize;
                    if (col < 0 || col >= 8 || row < 0 || row >= 8) return;

                    // Convert to chess position (a8 is top left in chess notation)
                    char column = (char) ('a' + col);
                    int chessBoardRow = 8 - row;

                    try {
                        if (sourcePosition == null) {
                            // First click - select piece
                            sourcePosition = new ChessPosition(column, chessBoardRow);
                            ChessPiece piece = (ChessPiece) chessMatch.getPieces()[row][col];

                            if (piece == null || piece.getColor() != chessMatch.getCurrentPlayer()) {
                                sourcePosition = null;
                                return;
                            }

                            possibleMoves = chessMatch.possibleMoves(sourcePosition);
                        } else {
                            // Second click - move piece
                            targetPosition = new ChessPosition(column, chessBoardRow);

                            ChessPiece capturedPiece = chessMatch.performeChessMove(sourcePosition, targetPosition);
                            if (capturedPiece != null) {
                                capturedPieces.add(capturedPiece);
                                updateCapturedPiecesDisplay();
                            }

                            // Handle promotion
                            if (chessMatch.getPromoted() != null) {
                                String[] options = {"Queen", "Rook", "Bishop", "Knight"};
                                int choice = JOptionPane.showOptionDialog(
                                    SwingUI.this,
                                    "Choose piece for promotion:",
                                    "Pawn Promotion",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                                );

                                String type = "";
                                switch (choice) {
                                    case 0: type = "Q"; break;
                                    case 1: type = "R"; break;
                                    case 2: type = "B"; break;
                                    case 3: type = "N"; break;
                                    default: type = "Q"; break;
                                }

                                chessMatch.replacePromotedPiece(type);
                            }

                            // Reset for next move
                            sourcePosition = null;
                            possibleMoves = new boolean[8][8];

                            // Update status and refresh movable pieces for the new turn
                            updateStatus();
                            updateMovablePieces();

                            // If AI mode is enabled and it's black's turn, make AI move
                            if (aiModeEnabled && chessMatch.getCurrentPlayer() == chess.Color.BLACK && !chessMatch.getCheckMate() && !chessMatch.getStalemate()) {
                                // Use SwingUtilities.invokeLater to ensure UI updates before AI move
                                SwingUtilities.invokeLater(() -> makeAIMove());
                            }
                        }

                        repaint();
                    } catch (ChessException ex) {
                        JOptionPane.showMessageDialog(SwingUI.this, ex.getMessage(), "Chess Error", JOptionPane.ERROR_MESSAGE);
                        sourcePosition = null;
                        possibleMoves = new boolean[8][8];
                        updateMovablePieces(); // Refresh movable pieces highlighting
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Update square size based on current panel dimensions
            updateSquareSize();

            // Draw board
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    // Determine square color
                    if ((row + col) % 2 == 0) {
                        g.setColor(LIGHT_SQUARE);
                    } else {
                        g.setColor(DARK_SQUARE);
                    }

                    // Draw square
                    g.fillRect(offsetX + col * squareSize, offsetY + row * squareSize, squareSize, squareSize);

                    // Highlight movable pieces (if no piece is selected)
                    if (sourcePosition == null && movablePieces[row][col]) {
                        g.setColor(MOVABLE_PIECE_HIGHLIGHT);
                        g.fillRect(offsetX + col * squareSize, offsetY + row * squareSize, squareSize, squareSize);
                    }

                    // Highlight possible moves (when a piece is selected)
                    if (possibleMoves[row][col]) {
                        g.setColor(HIGHLIGHT_COLOR);
                        g.fillRect(offsetX + col * squareSize, offsetY + row * squareSize, squareSize, squareSize);
                    }

                    // Show AI thinking indicator
                    if (aiIsThinking && chessMatch.getCurrentPlayer() == chess.Color.BLACK) {
                        // Only highlight black pieces when AI is thinking
                        ChessPiece piece = chessMatch.getPieces()[row][col];
                        if (piece != null && piece.getColor() == chess.Color.BLACK) {
                            g.setColor(AI_THINKING_COLOR);
                            g.fillRect(offsetX + col * squareSize, offsetY + row * squareSize, squareSize, squareSize);
                        }
                    }
                }
            }

            // Draw pieces
            ChessPiece[][] pieces = chessMatch.getPieces();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    ChessPiece piece = pieces[row][col];
                    if (piece != null) {
                        drawPiece(g, row, col, piece);
                    }
                }
            }

            // Draw coordinates (letters for columns and numbers for rows) OUTSIDE the board
            int coordFontSize = Math.max(10, squareSize / 3);
            g.setFont(new Font("Arial", Font.BOLD, coordFontSize));
            FontMetrics fm = g.getFontMetrics();

            g.setColor(java.awt.Color.DARK_GRAY);

            // Files (a-h) above and below the board
            for (int col = 0; col < 8; col++) {
                char fileChar = (char) ('a' + col);
                int xLeft = offsetX + col * squareSize + (squareSize - fm.charWidth(fileChar)) / 2;
                // Above
                int topY = offsetY - 4;
                g.drawString(String.valueOf(fileChar), xLeft, topY);
                // Below
                int bottomY = offsetY + 8 * squareSize + fm.getAscent() + 4;
                g.drawString(String.valueOf(fileChar), xLeft, bottomY);
            }

            // Ranks (1-8) left and right of the board
            for (int row = 0; row < 8; row++) {
                char rankChar = (char) ('0' + (8 - row));
                int yBaseline = offsetY + row * squareSize + (squareSize + fm.getAscent()) / 2 - 2;
                // Left
                int leftX = offsetX - fm.charWidth(rankChar) - 6;
                g.drawString(String.valueOf(rankChar), leftX, yBaseline);
                // Right
                int rightX = offsetX + 8 * squareSize + 6;
                g.drawString(String.valueOf(rankChar), rightX, yBaseline);
            }
        }

        private void drawPiece(Graphics g, int row, int col, ChessPiece piece) {
            String pieceSymbol = getPieceSymbol(piece);
            java.awt.Color pieceColor = (piece.getColor() == chess.Color.WHITE) ? java.awt.Color.WHITE : java.awt.Color.BLACK;

            int x = offsetX + col * squareSize;
            int y = offsetY + row * squareSize;

            // Draw piece - scale font size based on square size
            g.setColor(pieceColor);
            int fontSize = Math.max(12, squareSize * 2 / 3); // Scale font size proportionally to square size
            g.setFont(new Font("Arial Unicode MS", Font.BOLD, fontSize));
            FontMetrics metrics = g.getFontMetrics();
            int xPos = x + (squareSize - metrics.stringWidth(pieceSymbol)) / 2;
            int yPos = y + ((squareSize - metrics.getHeight()) / 2) + metrics.getAscent();
            g.drawString(pieceSymbol, xPos, yPos);
        }

        private String getPieceSymbol(ChessPiece piece) {
            String symbol = piece.toString();
            return symbol;
        }

        /**
         * Updates the movablePieces array to highlight all pieces that can move for the current player
         */
        public void updateMovablePieces() {
            // Reset the array
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    movablePieces[i][j] = false;
                }
            }

            // Get all pieces on the board
            ChessPiece[][] pieces = chessMatch.getPieces();

            // Check each piece
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    ChessPiece piece = pieces[row][col];

                    // If the piece belongs to the current player
                    if (piece != null && piece.getColor() == chessMatch.getCurrentPlayer()) {
                        // Convert board position to chess position
                        char column = (char) ('a' + col);
                        int chessBoardRow = 8 - row;
                        ChessPosition position = new ChessPosition(column, chessBoardRow);

                        try {
                            // Check if the piece has any possible moves
                            boolean[][] pieceMoves = chessMatch.possibleMoves(position);
                            boolean hasMoves = false;

                            // Check if there's at least one possible move
                            for (int i = 0; i < 8 && !hasMoves; i++) {
                                for (int j = 0; j < 8 && !hasMoves; j++) {
                                    if (pieceMoves[i][j]) {
                                        hasMoves = true;
                                    }
                                }
                            }

                            // If the piece has moves, mark it as movable
                            if (hasMoves) {
                                movablePieces[row][col] = true;
                            }
                        } catch (Exception e) {
                            // If there's an error (like the piece can't move), just continue
                            continue;
                        }
                    }
                }
            }
        }
    }

    private void updateStatus() {
        StringBuilder status = new StringBuilder("<html>");

        // Add turn number
        status.append("Turn: ").append(chessMatch.getTurn()).append(" - ");

        // Add player color with visual styling
        if (chessMatch.getCurrentPlayer() == chess.Color.WHITE) {
            // White player's turn - use white color with black border for visibility
            status.append("<span style='background-color: white; color: black; border: 1px solid black; padding: 2px 5px; font-weight: bold;'>⚪ WHITE</span>");
        } else {
            // Black player's turn - use black background with white text
            status.append("<span style='background-color: black; color: white; padding: 2px 5px; font-weight: bold;'>⚫ BLACK</span>");
        }

        // Add check status
        if (chessMatch.getCheck()) {
            status.append(" <span style='color: red; font-weight: bold;'>(CHECK!)</span>");
        }

        // Handle checkmate
        if (chessMatch.getCheckMate()) {
            status = new StringBuilder("<html><span style='color: red; font-size: 110%; font-weight: bold;'>CHECKMATE! Winner: ");
            if (chessMatch.getCurrentPlayer() == chess.Color.WHITE) {
                status.append("<span style='background-color: white; color: black; border: 1px solid black; padding: 2px 5px;'>⚪ WHITE</span>");
            } else {
                status.append("<span style='background-color: black; color: white; padding: 2px 5px;'>⚫ BLACK</span>");
            }
            status.append("</span></html>");
            statusLabel.setText(status.toString());
            return;
        }

        // Handle stalemate (draw)
        if (chessMatch.getStalemate()) {
            status = new StringBuilder("<html><span style='color: #333; font-size: 110%; font-weight: bold;'>DRAW by stalemate</span></html>");
            statusLabel.setText(status.toString());
            return;
        }

        status.append("</html>");
        statusLabel.setText(status.toString());
    }

    private void updateCapturedPiecesDisplay() {
        List<ChessPiece> whiteCaptured = new ArrayList<>();
        List<ChessPiece> blackCaptured = new ArrayList<>();

        for (ChessPiece piece : capturedPieces) {
            if (piece.getColor() == chess.Color.WHITE) {
                whiteCaptured.add(piece);
            } else {
                blackCaptured.add(piece);
            }
        }

        // Rebuild panels
        whiteCapturedPanel.removeAll();
        blackCapturedPanel.removeAll();

        // Render white pieces captured (i.e., pieces of WHITE color)
        for (ChessPiece piece : whiteCaptured) {
            JLabel lbl = new JLabel(piece.toString());
            lbl.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
            // Use dark glyphs on light background for better contrast
            lbl.setForeground(java.awt.Color.BLACK);
            whiteCapturedPanel.add(lbl);
        }

        // Render black pieces captured (i.e., pieces of BLACK color)
        for (ChessPiece piece : blackCaptured) {
            JLabel lbl = new JLabel(piece.toString());
            lbl.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
            // Use light glyphs on dark background for better contrast
            lbl.setForeground(java.awt.Color.WHITE);
            blackCapturedPanel.add(lbl);
        }

        whiteCapturedPanel.revalidate();
        whiteCapturedPanel.repaint();
        blackCapturedPanel.revalidate();
        blackCapturedPanel.repaint();
    }

    /**
     * Creates the menu bar with game options
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Game menu
        JMenu gameMenu = new JMenu("Game");

        // New Game option
        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(e -> {
            // Reset the game
            chessMatch = new ChessMatch();
            capturedPieces.clear();
            sourcePosition = null;
            updateStatus();
            updateCapturedPiecesDisplay();
            boardPanel.repaint();
        });

        // AI Mode option
        aiModeMenuItem = new JCheckBoxMenuItem("Play against Computer");
        aiModeMenuItem.addActionListener(e -> {
            aiModeEnabled = aiModeMenuItem.isSelected();

            // If AI mode is enabled and it's black's turn, make AI move
            if (aiModeEnabled && chessMatch.getCurrentPlayer() == chess.Color.BLACK) {
                makeAIMove();
            }
        });

        // Exit option
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        // Add items to game menu
        gameMenu.add(newGameItem);
        gameMenu.add(aiModeMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        // Add menus to menu bar
        menuBar.add(gameMenu);

        // Set the menu bar
        setJMenuBar(menuBar);
    }

    /**
     * Makes a move for the AI player
     */
    private void makeAIMove() {
        if (!aiModeEnabled || chessMatch.getCheckMate() || chessMatch.getStalemate()) {
            return;
        }

        // Set thinking flag
        aiIsThinking = true;
        boardPanel.repaint(); // Repaint to show thinking indicator

        // Use SwingWorker to run AI in background thread
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Add a small delay to make the AI seem like it's "thinking"
                    Thread.sleep(500);

                    // Get AI move
                    ChessPosition[] move = chessAI.selectMove(chessMatch);

                    if (move != null) {
                        ChessPosition source = move[0];
                        ChessPosition target = move[1];

                        // Perform the move
                        ChessPiece capturedPiece = chessMatch.performeChessMove(source, target);

                        if (capturedPiece != null) {
                            capturedPieces.add(capturedPiece);
                        }

                        // Handle promotion (AI always chooses Queen)
                        if (chessMatch.getPromoted() != null) {
                            chessMatch.replacePromotedPiece("Q");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                // Update UI after AI move
                aiIsThinking = false;
                updateStatus();
                updateCapturedPiecesDisplay();
                boardPanel.updateMovablePieces();
                boardPanel.repaint();
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingUI());
    }
}

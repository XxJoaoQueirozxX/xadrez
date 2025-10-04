package chess;

import boardgame.Position;
import chess.pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AI player for chess game
 * Implements a simple algorithm to select moves for the computer player
 */
public class ChessAI {
    private final Random random = new Random();

    /**
     * Selects a move for the computer player
     * @param chessMatch The current chess match
     * @return An array with source and target positions for the move
     */
    public ChessPosition[] selectMove(ChessMatch chessMatch) {
        // Get all possible moves for the current player
        List<MoveOption> allMoves = getAllPossibleMoves(chessMatch);

        if (allMoves.isEmpty()) {
            return null; // No moves available
        }

        // Evaluate and score each move
        for (MoveOption move : allMoves) {
            evaluateMove(move, chessMatch);
        }

        // Sort moves by score (highest first)
        allMoves.sort((a, b) -> Integer.compare(b.score, a.score));

        // Select one of the top moves (with some randomness to avoid predictability)
        int selectionIndex = 0;
        if (allMoves.size() > 1) {
            // Select from top 3 moves or all moves if less than 3
            int topMoveCount = Math.min(3, allMoves.size());
            selectionIndex = random.nextInt(topMoveCount);
        }

        // Try to find a legal move (that doesn't leave the king in check)
        for (int k = selectionIndex; k < allMoves.size(); k++) {
            MoveOption candidate = allMoves.get(k);
            if (chessMatch.isLegalMove(candidate.source, candidate.target)) {
                return new ChessPosition[]{candidate.source, candidate.target};
            }
        }

        // If none found from selectionIndex onward, try earlier ones too
        for (int k = 0; k < selectionIndex; k++) {
            MoveOption candidate = allMoves.get(k);
            if (chessMatch.isLegalMove(candidate.source, candidate.target)) {
                return new ChessPosition[]{candidate.source, candidate.target};
            }
        }

        // No legal moves available (checkmate or stalemate)
        return null;
    }

    /**
     * Gets all possible moves for the current player
     */
    private List<MoveOption> getAllPossibleMoves(ChessMatch chessMatch) {
        List<MoveOption> allMoves = new ArrayList<>();
        ChessPiece[][] board = chessMatch.getPieces();

        // Iterate through all squares on the board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                ChessPiece piece = board[i][j];

                // If the square contains a piece of the current player
                if (piece != null && piece.getColor() == chessMatch.getCurrentPlayer()) {
                    // Convert board position to chess position
                    char column = (char) ('a' + j);
                    int row = 8 - i;
                    ChessPosition source = new ChessPosition(column, row);

                    try {
                        // Get all possible moves for this piece
                        boolean[][] possibleMoves = chessMatch.possibleMoves(source);

                        // Convert possible moves to target positions
                        for (int targetRow = 0; targetRow < 8; targetRow++) {
                            for (int targetCol = 0; targetCol < 8; targetCol++) {
                                if (possibleMoves[targetRow][targetCol]) {
                                    char targetColumn = (char) ('a' + targetCol);
                                    int targetChessRow = 8 - targetRow;
                                    ChessPosition target = new ChessPosition(targetColumn, targetChessRow);

                                    allMoves.add(new MoveOption(source, target));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Skip if there's an error getting possible moves
                        continue;
                    }
                }
            }
        }

        return allMoves;
    }

    /**
     * Evaluates a move and assigns a score
     */
    private void evaluateMove(MoveOption move, ChessMatch chessMatch) {
        int score = 0;

        try {
            // Convert chess positions to board positions
            Position source = move.source.toPosition();
            Position target = move.target.toPosition();

            // Get the piece at the source position
            ChessPiece piece = (ChessPiece) chessMatch.getPieces()[source.getRow()][source.getColumn()];

            // Get the piece at the target position (if any)
            ChessPiece targetPiece = chessMatch.getPieces()[target.getRow()][target.getColumn()];

            // 1. Capturing pieces is good - score based on the captured piece value
            if (targetPiece != null) {
                score += getPieceValue(targetPiece) * 10;
            }

            // 2. Moving to the center of the board is generally good
            score += evaluateCentralization(target);

            // 3. Developing pieces in the opening is good
            if (chessMatch.getTurn() <= 10) {
                score += evaluateDevelopment(piece, target);
            }

            // 4. Pawn promotion is very good
            if (piece instanceof Pawn) {
                if ((piece.getColor() == Color.WHITE && target.getRow() == 0) || 
                    (piece.getColor() == Color.BLACK && target.getRow() == 7)) {
                    score += 900; // Queen value
                }
            }

            // 5. Castling is good for king safety
            if (piece instanceof King) {
                if (Math.abs(source.getColumn() - target.getColumn()) > 1) {
                    score += 30; // Bonus for castling
                }
            }

            // Store the evaluated score
            move.score = score;

        } catch (Exception e) {
            // If evaluation fails, assign a low score
            move.score = -1000;
        }
    }

    /**
     * Returns the value of a chess piece
     */
    private int getPieceValue(ChessPiece piece) {
        if (piece instanceof Pawn) return 10;
        if (piece instanceof Knight) return 30;
        if (piece instanceof Bishop) return 30;
        if (piece instanceof Rook) return 50;
        if (piece instanceof Queen) return 90;
        if (piece instanceof King) return 900; // Very high value for the king
        return 0;
    }

    /**
     * Evaluates how central a position is on the board
     */
    private int evaluateCentralization(Position position) {
        int row = position.getRow();
        int col = position.getColumn();

        // Distance from center (0-3)
        // Center of the board is between 3 and 4 (both row and column)
        int rowDistance = Math.abs(row - 3);
        int colDistance = Math.abs(col - 3);

        // Convert to a score (higher for more central positions)
        return 5 - (rowDistance + colDistance);
    }

    /**
     * Evaluates development value for opening moves
     */
    private int evaluateDevelopment(ChessPiece piece, Position target) {
        int score = 0;

        // Encourage knights and bishops to move out
        if (piece instanceof Knight || piece instanceof Bishop) {
            // If the piece has moved from its starting position
            if (piece.getMoveCount() == 0) {
                score += 15;
            }
        }

        return score;
    }

    /**
     * Class to represent a possible move with its evaluation score
     */
    private static class MoveOption {
        ChessPosition source;
        ChessPosition target;
        int score;

        public MoveOption(ChessPosition source, ChessPosition target) {
            this.source = source;
            this.target = target;
            this.score = 0;
        }
    }
}

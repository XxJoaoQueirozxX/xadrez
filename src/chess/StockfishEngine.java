package chess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Minimal UCI Stockfish engine integration.
 * Start the engine process, send FEN, and get a best move in UCI (e2e4, e7e8q, etc.).
 */
public class StockfishEngine {
    private Process process;
    private BufferedWriter engineIn;
    private BufferedReader engineOut;
    private String enginePath;

    public StockfishEngine() {}

    public void setEnginePath(String path) {
        this.enginePath = path;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public boolean start() {
        if (enginePath == null || enginePath.trim().isEmpty()) {
            return false;
        }
        if (isRunning()) return true;
        try {
            ProcessBuilder pb = new ProcessBuilder(enginePath);
            pb.redirectErrorStream(true);
            process = pb.start();
            engineIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            engineOut = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            // Initialize UCI
            sendCommand("uci");
            // wait for uciok
            String line;
            long startWait = System.currentTimeMillis();
            while ((line = engineOut.readLine()) != null) {
                if (line.contains("uciok")) break;
                if (System.currentTimeMillis() - startWait > 3000) break;
            }
            // Ensure ready
            if (!isReady(3000)) {
                stop();
                return false;
            }
            return true;
        } catch (IOException e) {
            stop();
            return false;
        }
    }

    public void stop() {
        try {
            if (engineIn != null) {
                try { sendCommand("quit"); } catch (Exception ignored) {}
                engineIn.close();
            }
        } catch (IOException ignored) {}
        try {
            if (engineOut != null) engineOut.close();
        } catch (IOException ignored) {}
        if (process != null) {
            process.destroy();
            try { process.waitFor(500, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {}
        }
        process = null;
        engineIn = null;
        engineOut = null;
    }

    private void sendCommand(String cmd) throws IOException {
        if (engineIn == null) throw new IOException("Engine not started");
        engineIn.write(cmd);
        engineIn.write("\n");
        engineIn.flush();
    }

    private boolean isReady(long timeoutMs) throws IOException {
        sendCommand("isready");
        String line;
        long start = System.currentTimeMillis();
        while ((line = engineOut.readLine()) != null) {
            if (line.startsWith("readyok")) return true;
            if (System.currentTimeMillis() - start > timeoutMs) return false;
        }
        return false;
    }

    /**
     * Get best move in UCI format for a given position (FEN).
     * @param fen the FEN string
     * @param movetimeMs time to think in milliseconds (e.g., 500)
     * @return bestmove in UCI (e.g., "e2e4" or "e7e8q"), or null on failure
     */
    public String getBestMoveUci(String fen, int movetimeMs) {
        try {
            if (!isRunning() && !start()) {
                return null;
            }
            // set position
            sendCommand("position fen " + fen);
            // go command
            sendCommand("go movetime " + Math.max(100, movetimeMs));

            String line;
            String best = null;
            long start = System.currentTimeMillis();
            while ((line = engineOut.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        best = parts[1];
                    }
                    break;
                }
                if (System.currentTimeMillis() - start > movetimeMs + 4000) {
                    break;
                }
            }
            return best;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Convert a UCI move (e2e4, e7e8q) into ChessPosition[] {source, target}.
     */
    public static ChessPosition[] parseUciMove(String uci) {
        if (uci == null || uci.length() < 4) return null;
        try {
            char fromFile = uci.charAt(0);
            char fromRank = uci.charAt(1);
            char toFile = uci.charAt(2);
            char toRank = uci.charAt(3);
            ChessPosition source = new ChessPosition(fromFile, Character.getNumericValue(fromRank));
            ChessPosition target = new ChessPosition(toFile, Character.getNumericValue(toRank));
            return new ChessPosition[]{source, target};
        } catch (Exception e) {
            return null;
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Player {
    String nickname;
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    String move = null;
    int score = 0;
    boolean inGame = false;
    Player opponent = null;
    Queue<Player> invitations = new LinkedList<>();

    public Player(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
}

public class rps_server {
    private static final int PORT = 12345;
    private static final Map<String, Player> players = new ConcurrentHashMap<>();
    private static final List<Player> waitingPlayers = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private static final String SCORE_FILE = "scores.txt";

    public static void main(String[] args) {
        loadScores();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new PlayerHandler(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadScores() {
        try (BufferedReader br = new BufferedReader(new FileReader(SCORE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    scores.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException e) {
            System.out.println("No existing score file found, starting a fresh game.");
        }
    }

    private static void saveScores() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SCORE_FILE))) {
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Error saving scores: " + e.getMessage());
        }
    }

    private static class PlayerHandler implements Runnable {
        private final Socket socket;
        private Player player;

        public PlayerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                player = new Player(socket);
                player.out.println("***Choose a nickname***");
                String nickname = player.in.readLine().trim();
                player.nickname = nickname;
                if (scores.containsKey(nickname)) {
                    player.score = scores.get(nickname);
                }
                players.put(nickname, player);
                player.out.println("Welcome, " + nickname + "! Your current score is " + player.score + ". You can type 'play' to start a game.");

                String input;
                while ((input = player.in.readLine()) != null) {
                    handleCommand(input.trim());
                }
            } catch (IOException e) {
                System.out.println("Player disconnected: " + (player != null ? player.nickname : "unknown"));
            } finally {
                if (player != null) {
                    players.remove(player.nickname);
                    waitingPlayers.remove(player);
                    if (player.opponent != null) {
                        player.opponent.opponent = null;
                        player.opponent.inGame = false;
                        player.opponent.out.println("Your opponent has disconnected.");
                    }
                    scores.put(player.nickname, player.score);
                    saveScores();
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void handleCommand(String command) throws IOException {
            String[] parts = command.split(" ");
            switch (parts[0].toLowerCase()) {
                case "play":
                    handlePlayCommand(parts);
                    break;
                case "score":
                    player.out.println("Your score is " + player.score);
                    break;
                case "players":
                    handlePlayersCommand();
                    break;
                case "y":
                case "n":
                    handleInvitationResponse(command);
                    break;
                case "r":
                case "p":
                case "s":
                    handleMoveCommand(command.toUpperCase());
                    break;
                default:
                    player.out.println("Unknown command. Available commands: play <opponent>, score, players");
                    break;
            }
        }

        private void handlePlayCommand(String[] parts) {
            synchronized (waitingPlayers) {
                if (player.inGame) {
                    player.out.println("You are already in a game.");
                } else {
                    if (parts.length == 2) {
                        String opponentNickname = parts[1];
                        Player opponent = players.get(opponentNickname);
                        if (opponent == null) {
                            player.out.println("Player " + opponentNickname + " does not exist.");
                        } else if (opponent.inGame) {
                            opponent.invitations.add(player);
                            player.out.println("Player " + opponentNickname + " is currently in a game. Your invitation is queued.");
                        } else {
                            opponent.out.println("***You have an invitation from " + player.nickname + ", play game? (y/n)***");
                            opponent.invitations.add(player);
                        }
                    } else {
                        if (waitingPlayers.isEmpty()) {
                            waitingPlayers.add(player);
                            player.out.println("Waiting for an opponent...");
                        } else {
                            Player opponent = waitingPlayers.remove(0);
                            startGame(player, opponent);
                        }
                    }
                }
            }
        }

        private void handlePlayersCommand() {
            player.out.println("Current players:");
            for (String nickname : players.keySet()) {
                player.out.println(nickname);
            }
        }

        private void handleMoveCommand(String move) {
            if (!player.inGame || player.opponent == null) {
                player.out.println("You are not in a game. Type 'play' to start a game.");
                return;
            }

            player.move = move;
            if (player.opponent.move == null) {
                player.out.println("Waiting for opponent to make a move...");
            } else {
                determineWinner();
                player.move = null;
                player.opponent.move = null;
            }
        }

        private void handleInvitationResponse(String response) throws IOException {
            Player inviter = player.invitations.poll();
            if (inviter == null) {
                player.out.println("No invitations to respond to.");
                return;
            }

            if (response.equalsIgnoreCase("y")) {
                if (player.inGame) {
                    player.out.println("You are already in a game.");
                } else {
                    startGame(player, inviter);
                }
            } else {
                inviter.out.println(player.nickname + " declined your invitation.");
            }

            if (!player.invitations.isEmpty()) {
                Player nextInviter = player.invitations.peek();
                player.out.println("***You have an invitation from " + nextInviter.nickname + ", play game? (y/n)***");
            }
        }

        private void startGame(Player player, Player opponent) {
            player.opponent = opponent;
            opponent.opponent = player;
            player.inGame = true;
            opponent.inGame = true;
            player.out.println("***You are now playing with " + opponent.nickname + "***");
            opponent.out.println("***You are now playing with " + player.nickname + "***");
        }

        private void determineWinner() {
            String result;
            String playerMove = player.move.toUpperCase();
            String opponentMove = player.opponent.move.toUpperCase();
        
            if (playerMove.equals(opponentMove)) {
                result = "It's a tie!";
            } else if ((playerMove.equals("R") && opponentMove.equals("S")) ||
                       (playerMove.equals("S") && opponentMove.equals("P")) ||
                       (playerMove.equals("P") && opponentMove.equals("R"))) {
                player.score++;
                if (player.score % 3 == 0) {
                    result = "***You have won the game! Your final score is " + player.score + "***";
                    player.opponent.out.println("***You have lost the game. Your final score is " + player.opponent.score + "***");
                } else {
                    result = "***You have won this challenge! Your score is " + player.score + "***";
                    player.opponent.out.println("***You have lost this challenge. Your score is " + player.opponent.score + "***");
                }
            } else {
                player.opponent.score++;
                if (player.opponent.score % 3 == 0) {
                    result = "***You have lost the game. Your final score is " + player.score + "***";
                    player.opponent.out.println("***You have won the game! Your final score is " + player.opponent.score + "***");
                } else {
                    result = "***You have lost this challenge. Your score is " + player.score + "***";
                    player.opponent.out.println("***You have won this challenge! Your score is " + player.opponent.score + "***");
                }
            }
            player.out.println(result);
        }
    }
}

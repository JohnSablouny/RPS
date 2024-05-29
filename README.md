# RPS
This project showcases a networked multiplayer Rock Paper Scissors game built with Java. It uses Java Sockets for network communication, Java I/O for handling data input/output, and concurrent collections for managing multiple clients. A new thread is opened for every new connection on the port. The game supports player invitations, score persistence, and multiple concurrent games, demonstrating the use of Java's core and networking libraries effectively.

Technologies used:
This entire project is written in Java using the following technologies:
1.Multithreading: Thread and Runnable classes used to handle multiple clients simultaneously.
2.Java Sockets to establish network connection between the players and the server.
3.Java I/O: Streams (InputStream, OutputStream, BufferedReader) used to handle input/output operations for the sockets connections and FileHandling (FileReader, FileWriter, BufferedReader, PrintWriter) used to store players score in the allocated file.
4.Concurrent collections used to manage the connections and the players list(ConcurrentHashmap, synchronizedList).

Project Components:
Player class: contains all the info concerning the players including their socket connection, input/output streams, game state, and score.
rps_server class: this class manages the players connections and the game logic like loading the existent scores and saving them.
PlayerHandler class: Handles individual player sessions, including commands and game interactions.
ServerReader class: running in a different thread, the main functionnality of this class is to continuously read and display server messages.
rps class: connects to the server and manage player's input.

Use those command to run the game:
javac rps_server.java
javac rps.java
java rps_server
Then every player connects to the game using:
java rps 127.0.0.1 or the IP provided by the user.

How it works:
The server listens for incoming connections on a specified port. When a player connects, a new thread is created to handle their interactions with the server. Each player is represented by an instance of the Player class, which contains their nickname, socket, input/output streams, current move, score, game state, and opponent. Player scores are stored in a file named "scores.txt". When a player connects, their score is loaded from the file if it exists. Scores are saved to the file whenever a player disconnects.


Players can perform the following actions in the game:
play <opponent>: Challenge a specific opponent to a game.
play: If there is only 2 players in the queue, they will play against each other.Also if there is multiple players, the first 2 that enter this command play against each other.If the player is connected to a game and type this command he'll be notified that he's already connected to a game.
score: Display the player's current score.
players: List all connected players.
R, P, S: Make a move (Rock, Paper, or Scissors) during a game.
y, n: Accept or decline a game invitation.
The play, score and players commands can be triggered anytime during the game.
If the player enters an unknown command, a message will be sent to inform him.

The game's flow:
1.Open VSCode terminal and change directory to the one where the files are located.
2.Run those commands to create the different classes needed in the following game:
        javac rps_server.java
        javac rps.java
3.Run the server in the same terminal using: java rps_server.
4.To play the game you have to run this command: java rps 127.0.0.1.
5.choose a nickname to be known as in the game.
6.You can challenge a specific player by typing "play {name}" or if there is only 2 players in the game both of you should type play and you will find yourselves playing against each others.
7.To see the players in the game type "players".
8.To see your score type "score".
9.When the player wins 3 challenges he wins the game automatically.
10.When you disconnect from the game, your opponent will be notified.
11.If you reconnect using the same nickname, your score will be recovered.





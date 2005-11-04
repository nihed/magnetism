/*------------------------------------------------------------------------------
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is levelonelabs.com code.
 * The Initial Developer of the Original Code is Level One Labs. Portions
 * created by the Initial Developer are Copyright (C) 2001 the Initial
 * Developer. All Rights Reserved.
 *
 *         Contributor(s):
 *             Scott Oster      (ostersc@alum.rpi.edu)
 *             Steve Zingelwicz (sez@po.cwru.edu)
 *             William Gorman   (willgorman@hotmail.com)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this
 * file only under the terms of either the GPL or the LGPL, and not to allow
 * others to use your version of this file under the terms of the NPL, indicate
 * your decision by deleting the provisions above and replace them with the
 * notice and other provisions required by the GPL or the LGPL. If you do not
 * delete the provisions above, a recipient may use your version of this file
 * under the terms of any one of the NPL, the GPL or the LGPL.
 *----------------------------------------------------------------------------*/

package com.levelonelabs.aimbot.modules;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.levelonelabs.aim.AIMBuddy;
import com.levelonelabs.aimbot.AIMBot;
import com.levelonelabs.aimbot.BotModule;


/**
 * Allows someone to play Tic Tac Toe against the computer or competitior. This
 * is to demonstrate how state can be saved for a game or other type of module.
 * Thanks to Niel Eyde for the idea.
 * 
 * @author Will Gorman
 * @created April 18, 2003
 */
public class TicTacToeModule extends BotModule {
    private static ArrayList services;
    /** This hashtable stores the states of all the games */
    private static Hashtable games;
    static Random rand = new Random();
    static Logger logger = Logger.getLogger(TicTacToeModule.class.getName());

    /**
     * Initialize the service commands.
     */
    static {
        services = new ArrayList();
        services.add("ttt");
        services.add("tictactoe");
        games = new Hashtable();
    }


    /**
     * Constructor for the TicTacToeModule object
     * 
     * @param bot
     */
    public TicTacToeModule(AIMBot bot) {
        super(bot);
    }


    /**
     * Gets the services attribute of the PreferenceModule object
     * 
     * @return The services value
     */
    public ArrayList getServices() {
        return services;
    }


    /**
     * Gets the name attribute of the Tic Tack Toe Module object
     * 
     * @return The name value
     */
    public String getName() {
        return "TicTacToe Module";
    }


    /**
     * Describes the usage of the module
     * 
     * @return the usage of the module
     */
    public String help() {
        StringBuffer sb = new StringBuffer();
        sb.append("<B>ttt <i>BUDDY</i></B> (starts a game with a buddy, if buddy is left blank, bot will play)\n");
        sb.append("<B>ttt <i>N</i></B> (tells tic tac toe what your move is, where N is the place to move)\n");
        sb.append("<B>ttt end</B> (ends a current game)\n");
        sb.append("<B>ttt show</B> (shows the current game)\n");
        return sb.toString();
    }


    /**
     * sends a message unless it is to the computer
     * 
     * @param buddy
     *            buddy to send message to
     * @param message
     *            message to send
     */
    public void sendMessageUnlessRobot(AIMBuddy buddy, String message) {
        if (!buddy.getName().equalsIgnoreCase(bot.getUsername())) {
            sendMessage(buddy, message);
        }
    }


    /**
     * Handle a tic tac toe query
     * 
     * @param buddy
     * @param query
     */
    public void performService(AIMBuddy buddy, String query) {
        if (query.toLowerCase().startsWith("ttt") || query.toLowerCase().startsWith("tictactoe")) {
            StringTokenizer st = new StringTokenizer(query, " ");
            st.nextToken(); // command
            String you = buddy.getName();
            TicTacToeGame tttg = (TicTacToeGame) games.get(you);
            if (tttg == null) {
                // starting a new game. either computer or player
                AIMBuddy competitor = null;
                String compname = null;
                if (st.hasMoreTokens()) {
                    compname = st.nextToken();
                    competitor = getBuddy(compname);
                }
                if ((compname == null) || compname.equalsIgnoreCase(bot.getUsername())) {
                    // see if the person is already in a game? (i guess not..
                    // see above).
                    // play against the computer
                    tttg = new TicTacToeGame(you, bot.getUsername());
                    tttg.state = tttg.STARTED;
                    tttg.computerMove();
                    games.put(you, tttg);
                    sendMessage(buddy, "\n" + tttg.displayMoves() + "\nSelect " + tttg.getCurrentXO()
                        + " Move (ttt N):");
                } else if (compname != null) {
                    if (competitor != null) {
                        // check if competitor is online, if so, invite him to
                        // the game.
                        if (games.get(competitor.getName()) != null) {
                            sendMessage(buddy, "Sorry, " + competitor.getName() + " is in another tic tac toe game.");
                        } else {
                            // let the user know competitor has been asked.
                            if (competitor.getName().equalsIgnoreCase(buddy.getName())) {
                                sendMessage(buddy, "Sorry, no playing with yourself.");
                            } else if (competitor.isOnline()) {
                                tttg = new TicTacToeGame(you, competitor.getName());
                                games.put(competitor.getName(), tttg);
                                games.put(you, tttg);
                                sendMessage(buddy, "Inviting " + compname + " to play.");
                                sendMessage(competitor, "Would you like to play Tic Tac Toe with " + you
                                    + "? (ttt yes/ttt no)");
                            } else {
                                sendMessage(buddy, "Sorry, " + competitor.getName() + " isn't online.");
                            }
                        }
                    } else {
                        sendMessage(buddy, "Sorry, can't find " + compname + " to play with.");
                    }
                }
            } else {
                if (st.hasMoreTokens()) {
                    String command = st.nextToken();
                    String comp = tttg.getCompetitor(you);
                    if (command.equals("end")) {
                        sendMessage(buddy, "the game has been terminated.");
                        sendMessageUnlessRobot(getBuddy(comp), "\n" + tttg.displayMoves() + "\n" + you
                            + " ended the game.");
                        games.remove(comp);
                        games.remove(you);
                    } else if (command.equals("show")) {
                        sendMessage(buddy, "\n" + tttg.displayMoves() + "\nwaiting for " + tttg.getCurrentPlayer()
                            + " to make a move.");
                    } else if (command.equals("yes")) {
                        if ((tttg.state == tttg.REQUESTED) && (tttg.getPlayerNumber(you) == 1)) {
                            tttg.state = tttg.STARTED;
                            sendMessage(getBuddy(tttg.getCurrentPlayer()), "\n" + tttg.displayMoves() + "\nSelect "
                                + tttg.getCurrentXO() + " Move  (ttt N):");
                        } else {
                            sendMessage(buddy, "Not sure what you are saying yes to.");
                        }
                    } else if (command.equals("no")) {
                        if ((tttg.state == tttg.REQUESTED) && (tttg.getPlayerNumber(you) == 1)) {
                            sendMessage(getBuddy(comp), you + " said no to tic tac toe.");
                            games.remove(comp);
                            games.remove(you);
                        } else {
                            sendMessage(buddy, "Not sure what you are saying no to.");
                        }
                    } else {
                        if (tttg.state == tttg.STARTED) {
                            try {
                                int move = Integer.parseInt(command);

                                // make sure move is valid
                                boolean valid = tttg.playerMove(you, move);
                                if (valid) {
                                    if (comp.equals(bot.getUsername())) {
                                        tttg.computerMove();
                                    }
                                    if (tttg.gameOver()) {
                                        int w = tttg.winner();
                                        if (w != -1) {
                                            String winner = tttg.getPlayerName(w);
                                            String loser = tttg.getCompetitor(winner);
                                            sendMessageUnlessRobot(getBuddy(winner), "\n" + tttg.displayMoves()
                                                + "\nYou won!");
                                            sendMessageUnlessRobot(getBuddy(loser), "\n" + tttg.displayMoves()
                                                + "\nYou lost!");
                                        } else {
                                            sendMessage(buddy, "\n" + tttg.displayMoves() + "\nIt was a tie!");
                                            sendMessageUnlessRobot(getBuddy(comp), "\n" + tttg.displayMoves()
                                                + "\nIt was a tie!");
                                        }
                                        games.remove(comp);
                                        games.remove(you);
                                    } else {
                                        sendMessage(getBuddy(tttg.getCurrentPlayer()), "\n" + tttg.displayMoves()
                                            + "\nSelect " + tttg.getCurrentXO() + " Move (ttt N):");
                                    }
                                } else {
                                    sendMessage(buddy, "\n" + tttg.displayMoves()
                                        + "\nInvalid move. Type \"ttt show\" to see the current state of the game.");
                                }
                            } catch (Exception e) {
                                logger.severe("Error: " + command + ":" + e.getMessage());
                                sendMessage(buddy, "Unrecognized tic tac toe command: " + command);
                            }
                        } else {
                            sendMessage(buddy, "state of game not started.  sorry.");
                        }
                    }
                } else {
                    sendMessage(buddy, "no command given!");
                }
            }
        }
    }


    /**
     * this internal class keeps track of a tic tac toe game.
     */
    static class TicTacToeGame {
        int REQUESTED = 0;
        int STARTED = 1;
        String[] player = new String[2];
        char[] xo = {'x', 'o'};
        int state = 0;
        int currentTurn = 0;
        char[][] board = new char[3][3];


        /**
         * Standard constructor, inits game.
         * 
         * @param p0
         *            player 0
         * @param p1
         *            player 1
         */
        public TicTacToeGame(String p0, String p1) {
            state = REQUESTED;
            player[0] = p0;
            player[1] = p1;
            for (int i = 0; i < 9; i++) {
                board[i / 3][i % 3] = ' ';
            }
            currentTurn = rand.nextInt(2);
        }


        /**
         * get the current player
         * 
         * @return player name
         */
        public String getCurrentPlayer() {
            return player[currentTurn];
        }


        /**
         * get current x or o
         * 
         * @return x or o
         */
        public String getCurrentXO() {
            return "" + xo[currentTurn];
        }


        /**
         * get player name
         * 
         * @param p
         *            id of player
         * @return name of player
         */
        public String getPlayerName(int p) {
            return player[p];
        }


        /**
         * get player number
         * 
         * @param p
         *            name of player
         * @return player number
         */
        public int getPlayerNumber(String p) {
            if (p.equals(player[0])) {
                return 0;
            }
            return 1;
        }


        /**
         * get the competitor of the current player
         * 
         * @param p
         *            name of player
         * @return competitor
         */
        public String getCompetitor(String p) {
            if (p.equals(player[0])) {
                return player[1];
            }
            return player[0];
        }


        /**
         * Decide whether the game is over or not.
         * 
         * @return is the game over
         */
        public boolean gameOver() {
            // Either all the spaces are full, or someone won
            if (winner() != -1) {
                return true;
            }
            for (int i = 0; i < 9; i++) {
                if (board[i / 3][i % 3] == ' ') {
                    return false;
                }
            }
            return true;
        }


        /**
         * Return the winner number
         * 
         * @return the winner
         */
        public int winner() {
            int d1 = -1;
            int d2 = -1;

            for (int i = 0; i < 3; i++) {
                int v = -1;
                int h = -1;

                if (i == 0) {
                    d1 = board[i][i];
                    d2 = board[2 - i][i];
                } else {
                    if (board[i][i] != d1) {
                        d1 = -1;
                    }
                    if (board[2 - i][i] != d2) {
                        d2 = -1;
                    }
                }
                for (int j = 0; j < 3; j++) {
                    if (j == 0) {
                        v = board[i][j];
                        h = board[j][i];
                    } else {
                        if (board[i][j] != v) {
                            v = -1;
                        }
                        if (board[j][i] != h) {
                            h = -1;
                        }
                    }
                }
                if ((v != -1) && (v != ' ')) {
                    return ((v == xo[0]) ? 0 : 1);
                }
                if ((h != -1) && (h != ' ')) {
                    return ((h == xo[0]) ? 0 : 1);
                }
            }
            if ((d1 != -1) && (d1 != ' ')) {
                return ((d1 == xo[0]) ? 0 : 1);
            }
            if ((d2 != -1) && (d2 != ' ')) {
                return ((d2 == xo[0]) ? 0 : 1);
            }

            return -1;
        }


        /**
         * Computer's move. Randomly select a location.
         */
        public void computerMove() {
            if ((currentTurn == 1) && !gameOver()) {
                int n = rand.nextInt(9);
                while (board[n / 3][n % 3] != ' ') {
                    n = rand.nextInt(9);
                }
                board[n / 3][n % 3] = xo[1];
                currentTurn = 0;
            }
        }


        /**
         * handle a player's move.
         * 
         * @param playerName
         *            players name
         * @param loc
         *            location on the board
         * @return valid move
         */
        public boolean playerMove(String playerName, int loc) {
            if ((loc < 1) || (loc > 9)) {
                return false;
            }
            if (getCurrentPlayer().equalsIgnoreCase(playerName)) {
                loc = loc - 1;
                if (board[loc % 3][loc / 3] == ' ') {
                    board[loc % 3][loc / 3] = xo[currentTurn];
                    currentTurn = (currentTurn + 1) % 2;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }


        /**
         * display the possible moves.
         * 
         * @return ttt board
         */
        public String displayMoves() {
            String b = "";
            for (int i = 0; i < 3; i++) { // Y dir
                for (int j = 0; j < 3; j++) { // X dir
                    if (board[j][i] == ' ') {
                        b += ((i * 3) + (j + 1));
                    } else {
                        b += "<b>" + board[j][i] + "</b>";
                    }
                    if (j != 2) {
                        b += "|";
                    } else {
                        b += "\n";
                    }
                }
                if (i != 2) {
                    b += "-+-+-\n";
                }
            }
            return b;
        }
    }
}

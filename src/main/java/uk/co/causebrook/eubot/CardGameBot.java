package uk.co.causebrook.eubot;

import uk.co.causebrook.eubot.events.PacketEvent;
import uk.co.causebrook.eubot.events.RegexListener;
import uk.co.causebrook.eubot.packets.commands.Send;
import uk.co.causebrook.eubot.packets.events.SendEvent;
import uk.co.causebrook.eubot.packets.fields.SessionView;
import uk.co.causebrook.eubot.relay.RelayMessage;
import uk.co.causebrook.eubot.relay.RelayMessageThread;
import uk.co.causebrook.eubot.relay.SharedMessageThread;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CardGameBot extends StandardBehaviour {
    public CardGameBot(Session accountRoom) {
        super("CardGameBot", "Hi, I run card games! Type !play [game] to start one.\nCurrently supported games:\nUno");
        addMessageListener(new RegexListener("^!play Uno$", (e) -> {
            List<SessionView> players = new ArrayList<>();
            boolean[] expired = new boolean[] {false};
            e.reply("Initialising game of Uno! Reply with !join to add yourself to this game.")
                    .thenAccept(iE -> iE.addReplyListener(
                            (rE) -> {
                                if(expired[0]) return;
                                if(Pattern.matches("^!join$", rE.getContent())) {
                                    players.add(rE.getData().getSender());
                                    rE.reply("Joined!");
                                    if(players.size() == 2) rE.getSession().send(new Send("Enough players have joined to !start game.", rE.getData().getParent()));
                                } else if(Pattern.matches("^!start$", rE.getContent())) {
                                    if(players.size() > 1) {
                                        new Thread(new UnoGame(accountRoom, e.getSession(), e.getData(), players)).start();
                                        expired[0] = true;
                                    }
                                }
                            }, Duration.ofHours(1)
            ));
        }));
    }

    public class UnoGame implements Runnable {
        private Session accountRoom;
        private Session root;
        private SendEvent rootMsg;
        private List<SessionView> playerViews;
        private SharedMessageThread pool;
        private boolean gameRunning = true;
        private Duration timeoutLength = Duration.ofMinutes(3);

        private CardPile deck = new CardPile();
        private int currColor;
        private int currNum;
        private CardPile discard = new CardPile();
        private List<Player> players = new ArrayList<>();
        private int playerTurn = 0;
        private int playerDirection = 1;
        private boolean chooseColor;
        private int drawCards = 0;

        public UnoGame(Session accountRoom, Session root, SendEvent rootMsg, List<SessionView> players) {
            this.accountRoom = accountRoom;
            this.root = root;
            this.rootMsg = rootMsg;
            playerViews = players;

            for(int color = 0; color < 4; color++) {
                String r = "";
                String p = "";
                switch (color) {
                    case 0:
                        r += ":heart:";
                        p += "[Rr](?:ed)? ?";
                        break;
                    case 1:
                        r += ":yellow_heart:";
                        p += "[Yy](?:ellow)? ?";
                        break;
                    case 2:
                        r += ":green_heart:";
                        p += "[Gg](?:reen)? ?";
                        break;
                    case 3:
                        r += ":blue_heart:";
                        p += "[Bb](?:lue)? ?";
                        break;
                }
                for(int num = 0; num < 10; num++) {
                    String numR = r;
                    switch(num) {
                        case 0: numR += ":zero:";
                            break;
                        case 1: numR += ":one:";
                            break;
                        case 2: numR += ":two:";
                            break;
                        case 3: numR += ":three:";
                            break;
                        case 4: numR += ":four:";
                            break;
                        case 5: numR += ":five:";
                            break;
                        case 6: numR += ":six:";
                            break;
                        case 7: numR += ":seven:";
                            break;
                        case 8: numR += ":eight:";
                            break;
                        case 9: numR += ":nine:";
                    }
                    Pattern pattern = Pattern.compile(p + num);
                    deck.add(new Card(numR, pattern, color, num));
                    deck.add(new Card(numR, pattern, color, num));
                }
                Pattern revPattern = Pattern.compile(p + "[Rr](?:everse)?");
                deck.add(new Card(r+":leftwards_arrow_with_hook:", revPattern, color, 10));
                deck.add(new Card(r+":leftwards_arrow_with_hook:", revPattern, color, 10));

                Pattern skipPattern = Pattern.compile(p + "[Ss](?:kip)?");
                deck.add(new Card(r+":fast_forward:", skipPattern, color, 11));
                deck.add(new Card(r+":fast_forward:", skipPattern, color, 11));

                Pattern plusTwoPattern = Pattern.compile(p + "\\+2");
                deck.add(new Card(r+":koko:", plusTwoPattern, color, 12));
                deck.add(new Card(r+":koko:", plusTwoPattern, color, 12));
            }

            Pattern wildPattern = Pattern.compile("[Ww](?:ild)?");
            deck.add(new Card(":rainbow:", wildPattern, 4, 0));
            deck.add(new Card(":rainbow:", wildPattern, 4, 0));
            deck.add(new Card(":rainbow:", wildPattern, 4, 0));
            deck.add(new Card(":rainbow:", wildPattern, 4, 0));

            Pattern plusFourPattern = Pattern.compile("\\+4");
            deck.add(new Card(":1234:", plusFourPattern, 4, 1));
            deck.add(new Card(":1234:", plusFourPattern, 4, 1));
        }

        private boolean tryPlay(Card c) {
            if(c.getValue(0) == 4 ||
                    c.getValue(0) == currColor ||
                    c.getValue(1) == currNum ) {
                discard.addFirst(c);
                currColor = c.getValue(0);
                currNum = c.getValue(1);
                return true;
            } else return false;
        }

        private CardPile draw(int numCards) {
            if(deck.size() < numCards) {
                Card top = discard.removeFirst();
                deck.addAll(discard);
                discard.clear();
                discard.add(top);
                if(deck.size() < numCards) {
                    pool.getRoot().replyAs("There are not enough cards left!", "Dealer");
                    return deck.deal(1).get(0);
                } else {
                    pool.getRoot().replyAs("There are no more cards in the draw pile. Reshuffling...", "Dealer");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return deck.deal(1, numCards).get(0);
        }

        private void notifyNext() {
            players.get(playerTurn).notifyNext();
        }

        private void advanceTurn() {
            playerTurn = Math.floorMod(playerTurn + playerDirection, players.size());
        }

        public void skipPlayer() {
            advanceTurn();
            pool.getRoot().replyAs(players.get(playerTurn).getNick() + "'s turn has been skipped!", "Dealer");
            advanceTurn();
            notifyNext();
        }

        public void nextPlayer() {
            advanceTurn();
            boolean allAFK = true;
            for(Player p : players) if(!p.isAFK()) {
                allAFK = false;
                break;
            }
            if(allAFK) {
                try {
                    Thread.sleep(60*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            players.get(playerTurn).notifyNext();
        }

        public void reverseOrder() {
            if(players.size() == 2) {
                skipPlayer();
            } else {
                playerDirection *= -1;
                pool.getRoot().replyAs("The direction of play has been reversed!", "Dealer");
                nextPlayer();
            }
        }

        public String getColorStr(int color, boolean emoji, boolean str) {
            switch (color) {
                case 0: return (emoji?":heart:":"") + (str?"red":"");
                case 1: return (emoji?":yellow_heart:":"") + (str?"yellow":"");
                case 2: return (emoji?":green_heart:":"") + (str?"green":"");
                case 3: return (emoji?":blue_heart:":"") + (str?"blue":"");
            }
            throw new IllegalArgumentException("\"" + color + "\" is not a valid color id.");
        }

        public void setColor(int color, String nick) {
            currColor = color;
            currNum = -1;
            pool.getRoot().replyAs("/me set the color to "+ getColorStr(color, true, true), nick);
        }

        public void nextDraw(int numCards) {
            Player p = players.get(Math.floorMod(playerTurn + playerDirection, players.size()));
            p.mustDraw(numCards);
            pool.getRoot().replyAs(p.getNick() + " must draw " + numCards + " cards!", "Dealer");
        }

        public boolean isNext(Player p) {
            return p.equals(players.get(playerTurn));
        }

        public List<Player> getPlayers() {
            List<Player> playersOrd = new ArrayList<>();
            for(int i = 0; i < players.size(); i++) {
                int player = Math.floorMod(playerTurn + playerDirection * i, players.size());
                playersOrd.add(players.get(player));
            }
            return playersOrd;
        }

        public SharedMessageThread getPool() {
            return pool;
        }

        public int getColor() {
            return currColor;
        }

        public Card getTopCard() {
            return discard.peek();
        }

        public boolean isRunning() {
            return gameRunning;
        }

        public void setTimeoutLength(Duration timeoutLength) {
            this.timeoutLength = timeoutLength;
        }

        public Duration getTimeoutLength() {
            return timeoutLength;
        }

        public void stop() {
            gameRunning = false;
        }

        @Override
        public void run() {
            try {
                List<CompletableFuture<Session>> futPms = new ArrayList<>();
                StringBuilder namesStr = new StringBuilder();
                for(SessionView p : playerViews) {
                    futPms.add(accountRoom.initPM(p));
                    namesStr.append(" @").append(p.getName().replaceAll("\\s", ""));
                }
                List<CompletableFuture<SendEvent>> futRoots = new ArrayList<>();
                List<Session> pms = new ArrayList<>();
                for(CompletableFuture<Session> fut : futPms) {
                    Session pm = fut.get();
                    pm.setNick("CardGamesBot");
                    pm.open();
                    futRoots.add(
                            pm.send(new Send("New game of UNO with: " + namesStr + ".")).thenApply(PacketEvent::getData)
                    );
                    pms.add(pm);
                }

                List<RelayMessageThread> pmThreads = new ArrayList<>();
                for(int i = 0; i < playerViews.size(); i++) {
                    RelayMessageThread pmThread = new RelayMessageThread(pms.get(i), futRoots.get(i).get());
                    pmThreads.add(pmThread);
                    Player p = new Player(pmThread, playerViews.get(i), this);
                    players.add(p);
                }
                pmThreads.add(new RelayMessageThread(root, rootMsg));

                pool = new SharedMessageThread(pmThreads);
                pool.setFilter(Pattern.compile("^[^!]"));
                pool.start();
            } catch(InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }
            Collections.shuffle(players);
            deck.shuffle();

            while(discard.isEmpty() || discard.peek().getValue(0) == 4 || discard.peek().getValue(1) > 9) {
                discard.addFirst(deck.pop());
            }
            currColor = discard.peek().getValue(0);
            currNum = discard.peek().getValue(1);

            List<CardPile> hands = deck.deal(players.size(), 7);
            for(int i = 0; i < players.size(); i++) {
                players.get(i).giveCards(hands.get(i));
            }
            StringBuilder playersStr = new StringBuilder();
            players.forEach((p) -> playersStr.append("\n").append(p.getNick()));
            pool.getRoot().replyAs("The order of play is:" + playersStr, "Dealer");
            pool.getRoot().replyAs("/me Deals hands...", "Dealer");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // Continue
            }
            pool.getRoot().replyAs("The top card is " + discard.peek() + ".", "Dealer");
            for(Player p : players) p.printHand(p.getThread().getRoot());
            notifyNext();
        }
    }

    private class Player {
        private RelayMessageThread pm;
        private UnoGame game;
        private SessionView user;
        private String nick;
        private CardPile hand = new CardPile();
        private final Object playLock = new Object();
        private boolean chooseColor = false;
        private int mustDraw = 0;
        private boolean mustCallUno = false;
        private ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture timeout;
        private int timeoutCounter = 0;

        public Player(RelayMessageThread pm, SessionView user, UnoGame game) {
            this.pm = pm;
            this.game = game;
            this.user = user;
            this.nick = user.getName();
            pm.addMessageListener((m) -> {
                if(!m.getData().getSender().getId().equals(user.getId())) return; // Check if not sent by user
                synchronized (playLock) {
                    if(timeout != null) timeout.cancel(false);
                    timeoutCounter = 0;
                    if(game.isRunning()){
                        Matcher playCard = Pattern.compile("^!p(?:lay)? ([\\w +]+)$").matcher(m.getData().getContent());
                        Matcher drawCard = Pattern.compile("^!d(?:raw)?(?:\\s|$)").matcher(m.getData().getContent());
                        Matcher colorSet = Pattern.compile("^!c(?:olou?r)? ([Rr](?:ed)?|[Yy](?:ellow)?|[Gg](?:reen)?|[Bb](?:lue)?)$").matcher(m.getData().getContent());
                        Matcher lookHand = Pattern.compile("^!h(?:and)?(?:\\s|$)").matcher(m.getData().getContent());
                        Matcher playOrder = Pattern.compile("^!o(?:rder)?(?:\\s|$)").matcher(m.getData().getContent());
                        Matcher reqHelp = Pattern.compile("^!help(?:\\s|$)").matcher(m.getData().getContent());
                        Matcher topCard = Pattern.compile("^!t(?:op)?(?:\\s|$)").matcher(m.getData().getContent());
                        Matcher unoCall = Pattern.compile("\\bu+n+o+\\b", Pattern.CASE_INSENSITIVE).matcher(m.getData().getContent());
                        if(playCard.find()) {
                            if(game.isNext(this)) {
                                if(mustDraw == 0) {
                                    if(!chooseColor) {
                                        Card c = takeCard(playCard.group(1));
                                        if(c != null) {
                                            if(game.tryPlay(c)) {
                                                game.getPool().getRoot().replyAs("/me plays " + c + ".     [" + hand.size() + " left]", nick);
                                                if(hand.isEmpty()) {
                                                    if(!mustCallUno) {
                                                        game.getPool().getRoot().replyAs(nick + " has played all their cards.", "Dealer");
                                                        game.getPool().getRoot().replyAs(getPingNick() + " WINS!", "Dealer");
                                                        game.stop();
                                                    } else {
                                                        try {
                                                            Thread.sleep(2000);
                                                        } catch (InterruptedException e) {
                                                            //Continue
                                                        }
                                                        game.getPool().getRoot().replyAs("...but they forgot to say UNO!", "Dealer");
                                                        try {
                                                            Thread.sleep(1000);
                                                        } catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                        }
                                                        game.getPool().getRoot().replyAs("/me draws two penalty cards.", nick);
                                                        CardPile drawn = game.draw(2);
                                                        pm.getRoot().reply("You drew " + drawn + ".");
                                                        giveCards(drawn);
                                                        game.nextPlayer();
                                                    }
                                                } else {
                                                    if(hand.size() == 1) mustCallUno = true;
                                                    if(c.getValue(1) == 10) {
                                                        game.reverseOrder();
                                                    } else if(c.getValue(1) == 11) {
                                                        game.skipPlayer();
                                                    } else if(c.getValue(1) == 12) {
                                                        game.nextDraw(2);
                                                        game.nextPlayer();
                                                    } else if(c.getValue(0) == 4) {
                                                        chooseColor = true;
                                                        game.getPool().getRoot().replyAs(nick + " can now choose a new color.", "Dealer");
                                                        timeout = timeoutExecutor.schedule(() -> {
                                                            synchronized (playLock) {
                                                                game.setColor((int) (Math.random()*4), nick);
                                                                chooseColor = false;
                                                                game.nextPlayer();
                                                                pm.getRoot().replyAs(getPingNick() + ", your color selection was skipped because you took too long.", "Dealer");
                                                                timeoutCounter++;
                                                            }
                                                        }, game.getTimeoutLength().toMillis(), TimeUnit.MILLISECONDS);
                                                        if(c.getValue(1) == 1) game.nextDraw(4);
                                                    } else {
                                                        game.nextPlayer();
                                                    }
                                                }
                                            } else {
                                                giveCard(c);
                                                m.replyAs("That card does not match color or number.", "Dealer");
                                                game.getPool().getRoot().replyAs("/me tries to play " + c + ", but it does not match.", nick);
                                            }
                                        } else m.replyAs("You do not have that card.", "Dealer");
                                    } else m.replyAs("You need to choose a !color.", "Dealer");
                                } else m.replyAs("You must !draw " + mustDraw + " cards!", "Dealer");
                            } else m.replyAs("It is not your turn!", "Dealer");
                        } else if(drawCard.find()) {
                            if(game.isNext(this)) {
                                if(!chooseColor) {
                                    draw(m);
                                } else m.replyAs("You need to choose a !color.", "Dealer");
                            } else m.replyAs("It is not your turn!", "Dealer");
                        } else if(colorSet.find()) {
                            if(game.isNext(this)) {
                                if(chooseColor) {
                                    String color = colorSet.group(1);
                                    if(color.matches("^[Rr](?:ed)?$")) game.setColor(0, nick);
                                    else if(color.matches("^[Yy](?:ellow)?$")) game.setColor(1, nick);
                                    else if(color.matches("^[Gg](?:reen)?$")) game.setColor(2, nick);
                                    else if(color.matches("^[Bb](?:lue)?$")) game.setColor(3, nick);
                                    chooseColor = false;
                                    game.nextPlayer();
                                }
                            } else m.replyAs("It is not your turn!", "Dealer");
                        } else if(lookHand.find()) {
                            printHand(m);
                        } else if(playOrder.find()) {
                            List<Player> players = game.getPlayers();
                            StringBuilder playersStr = new StringBuilder();
                            for(Player p : players) playersStr.append("\n[").append(p.numCards()).append("]    ").append(p.getNick());
                            m.replyAs("The order of play is currently:" + playersStr, "Dealer");
                        } else if(topCard.find()) {
                            Card top = game.getTopCard();
                            if(top.getValue(0) == 4) {
                                m.replyAs("The top card is " + top + ". The next card must be " + game.getColorStr(game.getColor(), true, true) + ".", "Dealer");
                            } else m.replyAs("The top card is " + top + ".", "Dealer");
                        } else if(reqHelp.find()) {
                            m.reply(":small_blue_diamond:Welcome to Uno!\n" +
                                    "      This bot plays the classic card game of Uno. The golden rule is that any card you play must match the last played card's color or number.\n" +
                                    "      Some cards have special effects, and wild cards can be played as any color. The first one to run out of cards wins.\n" +
                                    "      Don't forget to shout \"Uno!\" when you've only got one card left!\n" +
                                    ":small_blue_diamond:Chat\n" +
                                    "      Any message you send in this thread will be shared with everyone, unless it starts with \"!\". Isn't that convenient?\n" +
                                    ":small_blue_diamond:Commands\n" +
                                    "      !play [card]:       Plays a card from your hand.\n" +
                                    "      !draw:              Draws a card from the deck and ends your turn.\n" +
                                    "      !hand:              Displays your current hand.\n" +
                                    "      !top:               Displays the top card in the discard pile.\n" +
                                    "      !order:             Displays the order of play, with the current player at the top.\n" +
                                    "      !color [color]:     If you've just played a wild card, this selects a color.\n" +
                                    "      !help:              Display this wonderful help text. Hello!\n" +
                                    ":small_blue_diamond:Cards\n" +
                                    "      A card can be specified by a color:\n" +
                                    "          \"Red\", \"Yellow\", \"Green\", \"Blue\"\n" +
                                    "      ...followed by a card number or type:\n" +
                                    "          :zero: - :nine:: 0 - 9\n" +
                                    "          :leftwards_arrow_with_hook:: \"Reverse\"\n" +
                                    "          :fast_forward:: \"Skip\"\n" +
                                    "          :koko:: \"+2\"\n" +
                                    "      There are also two wild cards that do not have a color:\n" +
                                    "          :rainbow:: \"Wild\"\n" +
                                    "          :1234:: \"+4\"\n" +
                                    "      You can also specify any word in this list with its first letter.\n" +
                                    ":small_blue_diamond:Examples:\n" +
                                    "      !play Blue 4\n" +
                                    "      !p red7\n" +
                                    "      !play Y1\n" +
                                    "      !play +4\n" +
                                    "      !p w\n" +
                                    "      !color Red\n" +
                                    "      !c R\n" +
                                    "      !hand\n" +
                                    "      !d");
                        }
                        if(unoCall.find()) {
                            mustCallUno = false;
                        }
                    }
                }
            });
        }

        private void draw(RelayMessage m) {
            if(mustDraw == 0) {
                CardPile drawn = game.draw(1);
                giveCards(drawn);
                m.replyAs("You drew " + drawn + ".", "Dealer");
                game.getPool().getRoot().replyAs("/me draws a card.", nick);
                game.nextPlayer();
            } else {
                CardPile drawn = game.draw(mustDraw);
                giveCards(drawn);
                m.replyAs("You drew " + drawn + ".", "Dealer");
                game.getPool().getRoot().replyAs("/me draws " + mustDraw + " cards.", nick);
                mustDraw = 0;
                game.nextPlayer();
            }
        }

        public void notifyNext() {
            pm.getRoot().replyAs(getPingNick() + ", it's your turn!", "Dealer");
            synchronized (playLock) {
                if(timeout == null || timeout.isDone()) {
                    if(timeoutCounter < 2) {
                        timeout = timeoutExecutor.schedule(() -> {
                            synchronized (playLock) {
                                draw(pm.getRoot());
                                pm.getRoot().replyAs(getPingNick() + ", your turn was skipped because you took too long.", "Dealer");
                                timeoutCounter++;
                            }
                        }, game.getTimeoutLength().toMillis(), TimeUnit.MILLISECONDS);
                    } else {
                        if(timeoutCounter == 2) {
                            game.getPool().getRoot().replyAs(getNick() + " is AFK. Their turns are being skipped.", "Dealer");
                            pm.getRoot().replyAs(getPingNick() + ", send a message to rejoin the game.", "Dealer");
                        } else game.getPool().getRoot().replyAs("/me is AFK.", nick);
                        if(mustDraw == 0) {
                            game.nextPlayer();
                        } else {
                            draw(pm.getRoot());
                        }
                        timeoutCounter++;
                    }
                }
            }

        }

        public boolean isAFK() {
            return timeoutCounter >= 2;
        }

        public void mustDraw(int numCards) {
            mustDraw = numCards;
        }

        public void giveCards(List<Card> cards) {
            this.hand.addAll(cards);
        }

        public void giveCard(Card c) {
            hand.add(c);
        }

        public boolean hasCard(String cardStr) {
            for(Card c : hand) if(c.matches(cardStr)) return true;
            return false;
        }

        public int numCards() {
            return hand.size();
        }

        public Card takeCard(String cardStr) {
            ListIterator<Card> iter = hand.listIterator();
            while(iter.hasNext()) {
                Card c = iter.next();
                if(c.matches(cardStr)){
                    iter.remove();
                    return c;
                }
            }
            return null;
        }

        public CardPile takeAll() {
            CardPile old = hand;
            hand = new CardPile();
            return old;
        }

        public void printHand(RelayMessage parent) {
            parent.replyAs("You have " + hand.size() + " cards: \n" + hand, "Dealer");
        }

        public CardPile getHand() {
            return hand;
        }

        public RelayMessageThread getThread() {
            return pm;
        }

        public String getNick() {
            return nick;
        }

        public String getPingNick() {
            return "@" + nick.replaceAll("\\s", "");
        }
    }

    public class Card {
        private final int[] values;
        private final String representation;
        private final Pattern code;

        public Card(String representation, Pattern code, int... values) {
            this.values = values;
            this.code = code;
            this.representation = representation;
        }

        public boolean matches(String cardStr) {
            return code.matcher(cardStr).matches();
        }

        public int getValue(int valId) {
            return values[valId];
        }

        @Override
        public String toString() {
            return representation;
        }
    }

    public class CardPile extends LinkedList<Card> {

        public CardPile(Card... cards) {
            super(Arrays.asList(cards));
        }

        public CardPile(List<Card> cards) {
            super(cards);
        }

        public void shuffle() {
            Collections.shuffle(this);
        }

        public List<CardPile> deal(int numPiles) {
            int[] pile = new int[] {0};
            ArrayList<CardPile> ps = new ArrayList<>();
            for(int i = 0; i < numPiles; i++) ps.add(new CardPile());
            forEach((c) -> ps.get(pile[0]++ % numPiles).add(c));
            this.clear();
            return ps;
        }

        public List<CardPile> deal(int numPiles, int cardsPerPile) {
            ArrayList<CardPile> ps = new ArrayList<>();
            for(int n = 0; n < cardsPerPile; n++) for(int pile = 0; pile < numPiles; pile++) {
                if(n == 0) ps.add(new CardPile());
                ps.get(pile).add(remove());
            }
            return ps;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            if(isEmpty()) return "";
            str.append(get(0));
            for(int i = 1; i < size(); i++) str.append("   ").append(get(i));
            return str.toString();
        }
    }
}

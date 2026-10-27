import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class GamePaint extends JPanel implements Runnable {
    public static final int WIDTH = 1000, HEIGHT = 600;
    private static final int SPAWN_X = 50, SPAWN_Y = 410;

    private int levelNumber = 1;
    private int deaths = 0;
    private boolean win = false;
    private Level level = new Level(levelNumber);
    private Image background;

    // ช่วงเปลี่ยนฉาก (ตาย / เข้าประตู) นับถอยหลังจาก WAIT_TIME ถึง 0 = เล่นปกติ
    // ครึ่งแรกจอค่อยๆ มืด -> ตรงกลางเปลี่ยนด่าน -> ครึ่งหลังจอค่อยๆ สว่าง
    private static final int WAIT_TIME = 80;
    private int waitTime = 0;
    private boolean dying = false;   // true = กำลังตาย, false = กำลังผ่านด่าน
    private boolean atDoor = false;  // เข้าประตูแล้ว ไก่หายเข้าไปรอเพื่อนในประตู
    private Player player;

    // สีไก่ตามเลขผู้เล่น (id 0 = Server) ทุกเครื่องใช้ตารางเดียวกัน เลยเห็นสีตรงกัน
    public static final String[] COLORS = {"white", "brown", "black", "gray", "golden"};

    // เล่นกับเพื่อน (network = null คือเล่นคนเดียว)
    private Network network;
    private int myId = 0;
    private Player[] friends = new Player[Network.MAX_PLAYERS];          // ไก่เพื่อนแต่ละคน ตาม id
    private ArrayList<Player> friendsHere = new ArrayList<>();          // เพื่อนที่อยู่ด่านเดียวกับเรา
    private int[] lastFriendDeaths = new int[Network.MAX_PLAYERS];      // จำนวนตายของเพื่อนรอบที่แล้ว (-1 = ยังไม่เคยเห็น)
    private boolean friendDied = false; // เริ่มใหม่เพราะเพื่อนตาย (ไว้โชว์ข้อความ)

    public GamePaint(Network network) {
        this.network = network;
        if (network != null) {
            myId = network.myId;
        }
        player = new Player(SPAWN_X, SPAWN_Y, COLORS[myId]);
        for (int i = 0; i < Network.MAX_PLAYERS; i++) {
            if (i != myId) {
                friends[i] = new Player(SPAWN_X, SPAWN_Y, COLORS[i]);
            }
            lastFriendDeaths[i] = -1;
        }
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.white); //สีพื้นหลัง (ใช้ตอนโหลดรูปไม่ได้)
        background = GameObject.loadImage("background.png");
        setFocusable(true); //รับปุ่มกด

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) player.left = true;
                if (e.getKeyCode() == KeyEvent.VK_D) player.right = true;
                if (e.getKeyCode() == KeyEvent.VK_SPACE) player.jump = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) player.left = false;
                if (e.getKeyCode() == KeyEvent.VK_D) player.right = false;
                if (e.getKeyCode() == KeyEvent.VK_SPACE) player.jump = false;
            }
        });
    }

    // เริ่มเกม สร้างเทรดใหม่มารัน run()
    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true) {
            // ส่งตำแหน่งเราไป แล้วย้ายไก่เพื่อนแต่ละคนไปตามตำแหน่งที่ได้มา
            ArrayList<Player> here = new ArrayList<>();
            if (network != null) {
                network.send(levelNumber, player.getBounds().x, player.getBounds().y, deaths, atDoor);
                for (int i = 0; i < Network.MAX_PLAYERS; i++) {
                    if (i == myId || network.friendLevel[i] == 0) continue;   // ตัวเอง หรือไม่มีคนนี้
                    friends[i].moveTo(network.friendX[i], network.friendY[i]);
                    if (network.friendLevel[i] == levelNumber) {
                        here.add(friends[i]);
                    }

                    // เพื่อนเพิ่งตายในด่านเดียวกับเรา = เราเริ่มใหม่ด้วย (ไม่นับเป็นตายของเรา ไม่งั้นจะสั่งกันตายวนไปมา)
                    if (network.friendDeaths[i] != lastFriendDeaths[i]) {
                        boolean firstTime = lastFriendDeaths[i] == -1;   // เพิ่งเห็นคนนี้ครั้งแรก ไม่ใช่ตาย
                        lastFriendDeaths[i] = network.friendDeaths[i];
                        if (!firstTime && network.friendLevel[i] == levelNumber && waitTime == 0 && !win) {
                            dying = true;
                            friendDied = true;
                            waitTime = WAIT_TIME;
                        }
                    }

                    // เพื่อนไปด่านถัดไปแล้ว (เครื่องเราช้าไปนิด หรือเราเพิ่งเข้ามาทีหลัง) = เราไปด้วย จะได้ไม่ค้างคนละด่าน
                    if (network.friendLevel[i] > levelNumber && waitTime == 0 && !win) {
                        dying = false;
                        waitTime = WAIT_TIME;
                    }
                }
            }
            friendsHere = here;

            if (waitTime > 0) {
                // กำลังเปลี่ยนฉาก เกมหยุดนิ่ง
                waitTime--;
                if (waitTime == WAIT_TIME / 2) {   // จอมืดสุดแล้ว ค่อยเปลี่ยนด่านตรงนี้
                    if (!dying) {
                        levelNumber++;
                    }
                    if (levelNumber > Level.LAST_LEVEL) {
                        win = true;
                    } else {
                        level = new Level(levelNumber);   // สร้างด่านใหม่ กับดักกลับเป็นแบบเดิม
                        player.respawn(SPAWN_X, SPAWN_Y);
                        atDoor = false;
                    }
                }
            } else if (!win) {
                if (!atDoor) {
                    player.update(level.getFloors()); //อัปเดตตัวละคร (เข้าประตูแล้วไม่ขยับ)
                }
                level.update(player, friendsHere); //เช็คกับดัก

                if (!atDoor && level.isDead(player)) {
                    deaths++;
                    dying = true;
                    friendDied = false;
                    waitTime = WAIT_TIME;
                } else if (atDoor || level.isWin(player)) {
                    atDoor = true;
                    if (canLeave()) {
                        dying = false;
                        waitTime = WAIT_TIME;
                    }
                }
            }

            repaint();          // วาดจอใหม่

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    // เล่นคนเดียว: เข้าประตูได้เลย ยกเว้นด่านเล่น 2 คน
    // เล่นกับเพื่อน: ทุกด่านต้องรอจนเพื่อนทุกคนที่ออนไลน์เข้าประตูมาด้วย
    private boolean canLeave() {
        if (network == null) return levelNumber < Level.FIRST_COOP_LEVEL;
        int online = 0;
        for (int i = 0; i < Network.MAX_PLAYERS; i++) {
            if (i == myId || network.friendLevel[i] == 0) continue;
            online++;
            if (network.friendLevel[i] != levelNumber || !network.friendAtDoor[i]) {
                return false;   // ยังมีคนไม่เข้าประตู
            }
        }
        if (levelNumber >= Level.FIRST_COOP_LEVEL && online == 0) return false;   // ด่าน 2 คน แต่ไม่มีเพื่อน
        return true;
    }

    // เพื่อนคนนี้เข้าประตูไปแล้วหรือยัง (เข้าแล้วไม่ต้องวาด)
    private boolean friendAtDoor(Player p) {
        for (int i = 0; i < Network.MAX_PLAYERS; i++) {
            if (friends[i] == p) return network.friendAtDoor[i];
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (background != null) {
            g2.drawImage(background, 0, 0, WIDTH, HEIGHT, null);   // วาดพื้นหลังก่อน ของอื่นจะได้ทับข้างบน
        }

        boolean firstHalf = waitTime > WAIT_TIME / 2;   // ยังไม่ได้เปลี่ยนด่าน

        level.draw(g2);
        for (Player f : friendsHere) {
            if (!friendAtDoor(f)) {   // เพื่อนเข้าประตูแล้วก็ไม่ต้องวาด
                f.draw(g2);
            }
        }
        if (!atDoor && !(firstHalf && !dying)) {   // เข้าประตูแล้ว ไก่หายเข้าไปในประตู
            player.draw(g2);
        }

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Level " + levelNumber + "   Deaths: " + deaths, 20, 30);
        if (network == null && levelNumber >= Level.FIRST_COOP_LEVEL && !win) {
            g2.drawString("2 PLAYERS LEVEL - need a friend!", 20, 60);
        } else if (network != null && waitTime == 0 && !win && atDoor) {
            g2.drawString("Waiting for friends at the door...", 20, 60);
        }
        if (network != null) {
            g2.drawString("You are player " + (myId + 1) + " (" + COLORS[myId] + ")   Players here: " + (friendsHere.size() + 1), 20, 90);
        }

        if (waitTime > 0) {
            // ความมืดของจอ: เริ่ม 0 -> มืดสุด 255 ตรงกลาง -> กลับเป็น 0
            int alpha = 255 - Math.abs(waitTime - WAIT_TIME / 2) * 255 / (WAIT_TIME / 2);
            if (dying) {
                g2.setColor(new Color(200, 0, 0, alpha));   // ตาย = จอแดง
            } else {
                g2.setColor(new Color(0, 0, 0, alpha));     // ผ่านด่าน = จอดำ
            }
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            if (firstHalf) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 60));
                if (dying && friendDied) {
                    g2.drawString("FRIEND DIED", 310, 250);
                } else if (dying) {
                    g2.drawString("YOU DIED", 350, 250);
                } else {
                    g2.drawString("LEVEL CLEAR!", 290, 250);
                }
            }
        }

        if (win) {
            g2.setFont(new Font("Arial", Font.BOLD, 60));
            g2.drawString("YOU WIN!", 360, 250);
        }
    }
}

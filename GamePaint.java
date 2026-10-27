import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class GamePaint extends JPanel implements Runnable {
    public static final int WIDTH = 1000, HEIGHT = 600;
    private static final int SPAWN_X = 50, SPAWN_Y = 410;

    private int levelNumber = 1;    // 0 = หน้า lobby รอเพื่อน (เฉพาะตอนเล่นกับเพื่อน)
    private int deaths = 0;
    private boolean win = false;
    private Level level = new Level(1);
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
    private Image[] icons = new Image[Network.MAX_PLAYERS];   // รูปไก่แต่ละสี ไว้โชว์ในหน้า lobby

    // เล่นกับเพื่อน (network = null คือเล่นคนเดียว)
    private Network network;
    private int myId = 0;
    private Player[] friends = new Player[Network.MAX_PLAYERS];          // ไก่เพื่อนแต่ละคน ตาม id
    private ArrayList<Player> friendsHere = new ArrayList<>();          // เพื่อนที่อยู่ด่านเดียวกับเรา
    private int[] lastFriendDeaths = new int[Network.MAX_PLAYERS];      // จำนวนตายของเพื่อนรอบที่แล้ว (-1 = ยังไม่เคยเห็น)
    private boolean friendDied = false; // เริ่มใหม่เพราะเพื่อนตาย (ไว้โชว์ข้อความ)

    // ปุ่ม: START (หน้า lobby เฉพาะคนเปิดห้อง) / PLAY AGAIN (ตอนจบเกม)
    private JButton startButton = new JButton("START");
    private JButton restartButton = new JButton("PLAY AGAIN");
    private boolean startClicked = false;     // กดปุ่มแล้ว ให้เทรดเกมเป็นคนจัดการ
    private boolean restartClicked = false;

    public GamePaint(Network network) {
        this.network = network;
        if (network != null) {
            myId = network.myId;
            levelNumber = 0;   // เล่นกับเพื่อน เริ่มที่หน้า lobby
        }
        player = new Player(SPAWN_X, SPAWN_Y, COLORS[myId]);
        for (int i = 0; i < Network.MAX_PLAYERS; i++) {
            if (i != myId) {
                friends[i] = new Player(SPAWN_X, SPAWN_Y, COLORS[i]);
            }
            lastFriendDeaths[i] = -1;
            icons[i] = GameObject.loadImage(COLORS[i] + "/frames/stand_right_0.png");
        }
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.white); //สีพื้นหลัง (ใช้ตอนโหลดรูปไม่ได้)
        background = GameObject.loadImage("background.png");
        setFocusable(true); //รับปุ่มกด

        // วางปุ่มเองตามตำแหน่ง x, y
        setLayout(null);
        startButton.setBounds(400, 470, 200, 50);
        restartButton.setBounds(400, 300, 200, 50);
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        restartButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setFocusable(false);     // กดปุ่มแล้วคีย์บอร์ดยังอยู่ที่เกม
        restartButton.setFocusable(false);
        startButton.setVisible(false);
        restartButton.setVisible(false);
        add(startButton);
        add(restartButton);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startClicked = true;
            }
        });
        restartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restartClicked = true;
            }
        });

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

    // เริ่มเล่นใหม่ตั้งแต่ด่าน 1
    private void restart() {
        levelNumber = 1;
        win = false;
        atDoor = false;
        waitTime = 0;
        level = new Level(1);
        player.respawn(SPAWN_X, SPAWN_Y);
    }

    @Override
    public void run() {
        while (true) {
            // ส่งตำแหน่งเราไป แล้วย้ายไก่เพื่อนแต่ละคนไปตามตำแหน่งที่ได้มา
            ArrayList<Player> here = new ArrayList<>();
            if (network != null) {
                network.send(levelNumber, player.getBounds().x, player.getBounds().y, deaths, atDoor);
                for (int i = 0; i < Network.MAX_PLAYERS; i++) {
                    if (i == myId || network.friendLevel[i] < 0) continue;   // ตัวเอง หรือไม่มีคนนี้
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

                    // เพื่อนไปด่านถัดไปแล้ว (หรือคนเปิดห้องกด START) = เราไปด้วย จะได้ไม่ค้างคนละด่าน
                    // (ไม่นับเพื่อนที่จบเกมแล้ว ไม่งั้นกด PLAY AGAIN แล้วจะโดนดึงกลับไปหน้าจบ)
                    if (network.friendLevel[i] > levelNumber && network.friendLevel[i] <= Level.LAST_LEVEL
                            && waitTime == 0 && !win) {
                        dying = false;
                        waitTime = WAIT_TIME;
                    }

                    // เราจบเกมแล้ว แต่เพื่อนกด PLAY AGAIN (กลับไปด่าน 1) = เราเล่นใหม่ด้วย
                    if (win && network.friendLevel[i] == 1) {
                        restartClicked = true;
                    }
                }
            }
            friendsHere = here;

            // ปุ่ม START โชว์เฉพาะคนเปิดห้อง ตอนอยู่หน้า lobby / ปุ่ม PLAY AGAIN โชว์ตอนจบเกม
            startButton.setVisible(network != null && myId == 0 && levelNumber == 0 && waitTime == 0);
            restartButton.setVisible(win);

            if (startClicked) {
                startClicked = false;
                dying = false;
                waitTime = WAIT_TIME;   // เปลี่ยนฉากจาก lobby ไปด่าน 1
            }
            if (restartClicked) {
                restartClicked = false;
                restart();
            }

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
            } else if (!win && levelNumber > 0) {
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
            if (i == myId || network.friendLevel[i] < 0) continue;
            online++;
            if (network.friendLevel[i] < levelNumber) {
                return false;   // ยังมีคนตามมาไม่ถึงด่านนี้
            }
            if (network.friendLevel[i] == levelNumber && !network.friendAtDoor[i]) {
                return false;   // ยังมีคนไม่เข้าประตู (ถ้าเพื่อนไปด่านถัดไปแล้ว ถือว่าเข้าแล้ว)
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

    // หน้า lobby: รายชื่อผู้เล่นที่เข้ามาแล้ว
    private void drawLobby(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 200));   // กล่องขาวโปร่งแสง
        g2.fillRoundRect(250, 60, 500, 480, 30, 30);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString("LOBBY", 435, 115);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        int y = 150;
        for (int i = 0; i < Network.MAX_PLAYERS; i++) {
            boolean here = (i == myId) || network.friendLevel[i] >= 0;
            if (!here) continue;
            g2.drawImage(icons[i], 300, y, 50, 50, null);
            String text = "Player " + (i + 1) + " (" + COLORS[i] + ")";
            if (i == 0) text += "  - HOST";
            if (i == myId) text += "  - YOU";
            g2.drawString(text, 370, y + 33);
            y += 60;
        }

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        if (myId == 0) {
            g2.drawString("Press START when everyone is here", 340, 455);
        } else {
            g2.drawString("Waiting for host to start...", 385, 500);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // ภาพพิกเซลไม่เบลอ
        if (background != null) {
            g2.drawImage(background, 0, 0, WIDTH, HEIGHT, null);   // วาดพื้นหลังก่อน ของอื่นจะได้ทับข้างบน
        }

        boolean firstHalf = waitTime > WAIT_TIME / 2;   // ยังไม่ได้เปลี่ยนด่าน

        if (levelNumber == 0) {
            drawLobby(g2);
        } else {
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
            g2.drawString("Level " + Math.min(levelNumber, Level.LAST_LEVEL) + "   Deaths: " + deaths, 20, 30);   // จบเกมแล้วไม่ให้ขึ้น Level 8
            if (network == null && levelNumber >= Level.FIRST_COOP_LEVEL && !win) {
                g2.drawString("2 PLAYERS LEVEL - need a friend!", 20, 60);
            } else if (network != null && waitTime == 0 && !win && atDoor) {
                g2.drawString("Waiting for friends at the door...", 20, 60);
            }
            if (network != null) {
                g2.drawString("You are player " + (myId + 1) + " (" + COLORS[myId] + ")   Players here: " + (friendsHere.size() + 1), 20, 90);
            }
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

            if (firstHalf && levelNumber > 0) {   // ออกจาก lobby ไม่ต้องโชว์ข้อความ
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
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 60));
            g2.drawString("YOU WIN!", 360, 250);
        }
    }
}

import java.awt.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.io.File;

public class Player extends GameObject {
    public boolean left, right, jump;

    private static final double GRAVITY = 0.4;
    private static final double JUMP_POWER = -10;
    private static final int WALK_SPEED = 4;

    // เฟรมไก่ (สีตามชื่อโฟลเดอร์)
    private Image standLeft, standRight;
    private Image[] walkLeft = new Image[2];
    private Image[] walkRight = new Image[2];
    private Image jumpLeft, jumpRight;

    private boolean facingRight = true;   // หันไปทางไหนอยู่
    private int animTick = 0;             // นับเฟรมไว้สลับท่าเดิน

    private double fallSpeed = 0;
    private boolean onFloor = false;

    // color = ชื่อโฟลเดอร์สีไก่ เช่น "white", "brown"
    public Player(int x, int y, String color) {
        super(x, y, 40, 40);
        standLeft = load(color, "stand_left_0.png");
        standRight = load(color, "stand_right_0.png");
        walkLeft[0] = load(color, "walk_left_0.png");
        walkLeft[1] = load(color, "walk_left_1.png");
        walkRight[0] = load(color, "walk_right_0.png");
        walkRight[1] = load(color, "walk_right_1.png");
        jumpLeft = load(color, "jump_left_0.png");
        jumpRight = load(color, "jump_right_0.png");
    }

    private Image load(String color, String name) {
        String path = color + "/frames/" + name;
        try {
            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
            // รันจากโฟลเดอร์อื่น ลองหาในโฟลเดอร์เดียวกับไฟล์ .class
            java.net.URL url = Player.class.getResource("/" + path);
            if (url != null) {
                return ImageIO.read(url);
            }
            System.out.println("Image not found: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed Upload " + name + " : " + e.getMessage());
        }
        return null;
    }

    // เลือกรูปตามทิศทางและท่าทาง
    private Image currentSprite() {
        if (!onFloor) {
            return facingRight ? jumpRight : jumpLeft;
        }
        if (left != right) {
            int frame = (animTick / 10) % 2;   // ทุก 10 รอบ สลับท่าเดิน
            return facingRight ? walkRight[frame] : walkLeft[frame];
        }
        return facingRight ? standRight : standLeft;
    }

    // ใช้กับไก่เพื่อน: ย้ายไปตำแหน่งที่ได้จากเน็ต แล้วดูจากการขยับว่าเดิน/กระโดดอยู่
    public void moveTo(int x, int y) {
        left = x < bounds.x;
        right = x > bounds.x;
        if (left) facingRight = false;
        if (right) facingRight = true;
        onFloor = (y == bounds.y);
        animTick++;
        bounds.setLocation(x, y);
    }

    // กลับไปจุดเกิด
    public void respawn(int x, int y) {
        bounds.setLocation(x, y);
        fallSpeed = 0;
    }

    public void update(ArrayList<GameFloor> floors) {
        if (left) bounds.x -= WALK_SPEED;
        if (right) bounds.x += WALK_SPEED;
        if (bounds.x < 0) bounds.x = 0;   // กันเดินหลุดจอซ้าย

        if (left && !right) facingRight = false;
        if (right && !left) facingRight = true;
        animTick++;

        if (jump && onFloor) {
            fallSpeed = JUMP_POWER;
        }

        int feetBefore = bounds.y + bounds.height;
        fallSpeed += GRAVITY;
        bounds.y += (int) fallSpeed;

        // ตกลงมาโดนพื้น = ยืนบนพื้น
        // ยืดเท้าลงไป 1px ให้ตอนยืนแตะพื้นพอดีนับว่ายืนอยู่ ไม่งั้นรูปจะกระพริบ
        Rectangle feet = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height + 1);
        onFloor = false;
        for (GameFloor f : floors) {
            if (!f.isSolid()) continue;   // สะพานที่ยังไม่โผล่ ยืนไม่ได้
            Rectangle r = f.getBounds();
            if (fallSpeed > 0 && feet.intersects(r) && feetBefore <= r.y) {
                bounds.y = r.y - bounds.height;
                fallSpeed = 0;
                onFloor = true;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        Image sprite = currentSprite();
        if (sprite != null) {
            g.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            g.setColor(Color.PINK);
            g.fill(bounds);
        }
    }
}

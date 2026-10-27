import java.awt.*;
import java.io.File;

import javax.imageio.ImageIO;

public class Spike extends GameObject {
    public static final int SIZE = 30;

    private static Image sprite_A;
    private int triggerX;      // ผู้เล่นเดินเลย x นี้เมื่อไหร่ หนามโผล่ (0 = โผล่ตั้งแต่แรก)
    private boolean active;    // false = ซ่อนอยู่ ไม่วาด ไม่ฆ่า

    public Spike(int x, int y, int triggerX) {
        super(x, y, SIZE, SIZE);
        this.triggerX = triggerX;
        active = (triggerX == 0);
        if (sprite_A == null) {
            try {
                sprite_A = ImageIO.read(new File("spike.png"));   // เปลี่ยนชื่อไฟล์ตามที่คุณตั้ง
            } catch (Exception e) {
                System.out.println("Failed Upload : " + e.getMessage());
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // หนามเป็นสามเหลี่ยม เลยให้จุดชนเล็กกว่ารูปนิดหน่อย จะได้ไม่ตายตอนแค่เฉียดมุม
    public Rectangle getHitBox() {
        return new Rectangle(bounds.x + 8, bounds.y + 10, bounds.width - 16, bounds.height - 10);
    }

    @Override
    public void update(Player player) {
        if (!active && player.getBounds().x > triggerX) {
            active = true;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (!active) return;
        if (sprite_A != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // ภาพพิกเซลไม่เบลอ
            g.drawImage(sprite_A, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            // โหลดรูปไม่ได้ ก็วาดสามเหลี่ยมแทน
            g.setColor(Color.DARK_GRAY);
            int[] xs = {bounds.x, bounds.x + bounds.width / 2, bounds.x + bounds.width};
            int[] ys = {bounds.y + bounds.height, bounds.y, bounds.y + bounds.height};
            g.fillPolygon(xs, ys, 3);
        }
    }
}

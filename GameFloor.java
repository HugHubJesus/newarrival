import java.awt.*;
import java.io.File;

import javax.imageio.ImageIO;

public class GameFloor extends GameObject {
    private static Image sprite;
    private boolean solid = true;   // false = สะพานที่ยังไม่โผล่ เหยียบไม่ได้ ไม่วาด

    public GameFloor(int x, int y, int width, int height) {
        super(x, y, width, height);
        if (sprite == null) {
            try {
                sprite = ImageIO.read(new File("ground.png"));
            } catch (Exception e) {
                System.out.println("Failed Upload ground.png : " + e.getMessage());
            }
        }
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    @Override
    public void draw(Graphics2D g) {
        if (!solid) return;
        if (sprite != null) {
            // วาดรูปพื้นเต็มขนาดจอ แต่ setClip ให้เห็นแค่ในกรอบพื้นแผ่นนี้ ลายดินจะได้ไม่ยืดเบี้ยว
            Shape oldClip = g.getClip();
            g.setClip(bounds);
            g.drawImage(sprite, 0, bounds.y, null);
            g.setClip(oldClip);
        } else {
            g.setColor(new Color(139, 69, 19)); //โหลดรูปไม่ได้ ก็ใช้สีน้ำตาลแทน
            g.fill(bounds);
        }
    }
}

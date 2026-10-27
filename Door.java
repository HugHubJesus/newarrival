import java.awt.*;
import java.io.File;

import javax.imageio.ImageIO;

// ประตูทางออก เดินชนแล้วผ่านด่าน
// กับดัก: ถ้า runToX ไม่เท่ากับ x พอผู้เล่นเข้าใกล้ ประตูจะหนีไปอยู่ที่ runToX
public class Door extends GameObject {
    private static Image sprite;
    private int runToX;

    public Door(int x, int y, int runToX) {
        super(x, y, 40, 60);
        this.runToX = runToX;
        if (sprite == null) {
            try {
                sprite = ImageIO.read(new File("door.png"));
            } catch (Exception e) {
                System.out.println("Failed Upload door.png : " + e.getMessage());
            }
        }
    }

    @Override
    public void update(Player player) {
        if (Math.abs(player.getBounds().x - bounds.x) < 120) {
            bounds.x = runToX;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        if (sprite != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // ภาพพิกเซลไม่เบลอ
            g.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            // โหลดรูปไม่ได้ ก็วาดสี่เหลี่ยมสีเขียวแทน
            g.setColor(new Color(0, 150, 0));
            g.fill(bounds);
        }
    }
}

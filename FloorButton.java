import java.awt.*;
import java.util.ArrayList;

// ปุ่มบนพื้น (ด่านเล่น 2 คน) มีคนยืนทับอยู่ = ปุ่มถูกกด
public class FloorButton extends GameObject {
    private static Image upImage, downImage;
    private boolean pressed = false;

    public FloorButton(int x, int y) {
        super(x, y, 40, 10);
        if (upImage == null) {
            upImage = GameObject.loadImage("button_up.png");
            downImage = GameObject.loadImage("button_down.png");
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    // เช็คว่าไก่ตัวเรา หรือไก่เพื่อนคนไหน ยืนทับอยู่มั้ย
    public void check(Player player, ArrayList<Player> friends) {
        pressed = player.getBounds().intersects(bounds);
        for (Player p : friends) {
            if (p.getBounds().intersects(bounds)) {
                pressed = true;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        Image img = upImage;     // ยังไม่มีคนเหยียบ = ปุ่มแดง
        if (pressed) {
            img = downImage;     // มีคนเหยียบ = ปุ่มเขียว
        }
        if (img != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // ภาพพิกเซลไม่เบลอ
            g.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, null);
        } else {
            // โหลดรูปไม่ได้ ก็วาดสี่เหลี่ยมแทน
            if (pressed) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.RED);
            }
            g.fill(bounds);
        }
    }
}

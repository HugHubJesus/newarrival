import java.awt.*;

public abstract class GameObject {
    protected Rectangle bounds;   // ตำแหน่ง + ขนาด (x, y, กว้าง, สูง)

    public GameObject(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    // กับดักแต่ละอันจะ override เอง ของที่อยู่เฉยๆ ไม่ต้องทำอะไร
    public void update(Player player) {
    }

    public abstract void draw(Graphics2D g);   // ลูกแต่ละตัววาดตัวเองไม่เหมือนกัน
}

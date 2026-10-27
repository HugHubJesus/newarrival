import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

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

    // โหลดรูป: หาจากโฟลเดอร์ที่รันเกมก่อน ถ้าไม่เจอ ลองหาในโฟลเดอร์เดียวกับไฟล์ .class
    // (เครื่องเพื่อนบางเครื่องรันจากโฟลเดอร์อื่น ถ้าหาทางเดียวจะไม่เจอรูป)
    public static Image loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
            java.net.URL url = GameObject.class.getResource("/" + path);
            if (url != null) {
                return ImageIO.read(url);
            }
            System.out.println("Image not found: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed Upload " + path + " : " + e.getMessage());
        }
        return null;
    }
}

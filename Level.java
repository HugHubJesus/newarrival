import java.awt.*;
import java.util.ArrayList;

public class Level {
    public static final int LAST_LEVEL = 7;
    public static final int FIRST_COOP_LEVEL = 6;   // ด่าน 6 ขึ้นไปต้องเล่น 2 คน
    private static final int GROUND_Y = 450;   // ขอบบนของพื้น
    private static final int GROUND_H = 150;

    private ArrayList<GameFloor> floors = new ArrayList<>();
    private ArrayList<Spike> spikes = new ArrayList<>();
    private Door door;

    // ของในด่านเล่น 2 คน: มีคนยืนบนปุ่มไหนก็ได้ = หนามหลบ + สะพานโผล่
    private ArrayList<FloorButton> buttons = new ArrayList<>();
    private ArrayList<Spike> switchSpikes = new ArrayList<>();
    private ArrayList<GameFloor> bridges = new ArrayList<>();

    // สร้างด่านตามหมายเลข ตายเมื่อไหร่ก็ new Level ใหม่ กับดักจะกลับเป็นเหมือนเดิม
    public Level(int number) {
        if (number == 1) {
            // พื้นตรงกลางดูปกติ แต่เดินไปแล้วร่วง
            floors.add(new GameFloor(0, GROUND_Y, 400, GROUND_H));
            floors.add(new FallingFloor(400, GROUND_Y, 100, GROUND_H, 400));
            floors.add(new GameFloor(500, GROUND_Y, 500, GROUND_H));
            door = new Door(920, GROUND_Y - 60, 920);
        } else if (number == 2) {
            // กระโดดข้ามหนามแรก แล้วหนามใหม่โผล่ตรงที่จะลง
            floors.add(new GameFloor(0, GROUND_Y, 1000, GROUND_H));
            spikes.add(new Spike(300, GROUND_Y - Spike.SIZE, 0));
            spikes.add(new Spike(330, GROUND_Y - Spike.SIZE, 0));
            spikes.add(new Spike(470, GROUND_Y - Spike.SIZE, 300));
            spikes.add(new Spike(500, GROUND_Y - Spike.SIZE, 300));
            door = new Door(920, GROUND_Y - 60, 920);
        } else if (number == 3) {
            // ประตูหนี แล้วพื้นก่อนถึงประตูร่วง
            floors.add(new GameFloor(0, GROUND_Y, 700, GROUND_H));
            floors.add(new FallingFloor(700, GROUND_Y, 100, GROUND_H, 690));
            floors.add(new GameFloor(800, GROUND_Y, 200, GROUND_H));
            door = new Door(500, GROUND_Y - 60, 920);
        } else if (number == 4) {
            // ประตูอยู่ใกล้นิดเดียว แต่หนีไปสุดจอ ระหว่างทางมีหนามโผล่ใส่หน้า 2 รอบ
            floors.add(new GameFloor(0, GROUND_Y, 1000, GROUND_H));
            spikes.add(new Spike(600, GROUND_Y - Spike.SIZE, 540));
            spikes.add(new Spike(860, GROUND_Y - Spike.SIZE, 780));
            door = new Door(200, GROUND_Y - 60, 920);
        } else if (number == 5) {
            // พื้นร่วงเหมือนด่าน 1 ให้ตายใจ แต่พื้นก่อนถึงประตูก็ร่วงด้วย ต้องกระโดดไปลงพื้นตรงประตู
            floors.add(new GameFloor(0, GROUND_Y, 300, GROUND_H));
            floors.add(new FallingFloor(300, GROUND_Y, 100, GROUND_H, 300));
            floors.add(new GameFloor(400, GROUND_Y, 250, GROUND_H));
            floors.add(new FallingFloor(650, GROUND_Y, 190, GROUND_H, 750));
            floors.add(new GameFloor(840, GROUND_Y, 160, GROUND_H));   // พื้นตรงประตู ไว้ยืนรอเพื่อน
            spikes.add(new Spike(520, GROUND_Y - Spike.SIZE, 0));
            door = new Door(860, GROUND_Y - 60, 860);
        } else if (number == 6) {
            // (2 คน) พรมหนามยาวเกินกระโดด คนนึงยืนกดปุ่มให้หนามหลบ อีกคนเดินข้ามไปกดปุ่มฝั่งโน้นให้
            floors.add(new GameFloor(0, GROUND_Y, 1000, GROUND_H));
            for (int x = 300; x < 700; x += Spike.SIZE) {
                Spike s = new Spike(x, GROUND_Y - Spike.SIZE, 0);
                spikes.add(s);
                switchSpikes.add(s);
            }
            buttons.add(new FloorButton(200, GROUND_Y - 10));
            buttons.add(new FloorButton(760, GROUND_Y - 10));
            spikes.add(new Spike(850, GROUND_Y - Spike.SIZE, 800));   // หนามโผล่หน้าประตู
            door = new Door(920, GROUND_Y - 60, 920);
        } else {
            // (2 คน) เหวกว้างเกินกระโดด ต้องกดปุ่มให้สะพานโผล่
            // ข้ามไปแล้วประตูหนีกลับไปฝั่งเริ่ม ต้องช่วยกันข้ามกลับอีกรอบ
            floors.add(new GameFloor(0, GROUND_Y, 300, GROUND_H));
            GameFloor bridge = new GameFloor(300, GROUND_Y, 250, 20);
            floors.add(bridge);
            bridges.add(bridge);
            floors.add(new GameFloor(550, GROUND_Y, 450, GROUND_H));
            buttons.add(new FloorButton(150, GROUND_Y - 10));
            buttons.add(new FloorButton(620, GROUND_Y - 10));
            door = new Door(900, GROUND_Y - 60, 70);
        }
    }

    public ArrayList<GameFloor> getFloors() {
        return floors;
    }

    // เช็คกับดักทุกเฟรม (friends = เพื่อนที่อยู่ในด่านเดียวกัน เล่นคนเดียวก็เป็น list ว่าง)
    public void update(Player player, ArrayList<Player> friends) {
        for (GameFloor f : floors) {
            f.update(player);
            for (Player p : friends) f.update(p);
        }
        for (Spike s : spikes) {
            s.update(player);
            for (Player p : friends) s.update(p);
        }
        door.update(player);
        for (Player p : friends) door.update(p);

        // มีปุ่มไหนถูกกดอยู่มั้ย
        boolean pressed = false;
        for (FloorButton b : buttons) {
            b.check(player, friends);
            if (b.isPressed()) pressed = true;
        }
        for (Spike s : switchSpikes) {
            s.setActive(!pressed);
        }
        for (GameFloor f : bridges) {
            f.setSolid(pressed);
        }
    }

    // ตกเหว หรือ โดนหนาม = ตาย
    public boolean isDead(Player player) {
        if (player.getBounds().y > GamePaint.HEIGHT) return true;
        for (Spike s : spikes) {
            if (s.isActive() && player.getBounds().intersects(s.getHitBox())) {
                return true;
            }
        }
        return false;
    }

    public boolean isWin(Player player) {
        return player.getBounds().intersects(door.getBounds());
    }

    public void draw(Graphics2D g) {
        for (GameFloor f : floors) {
            f.draw(g);
        }
        for (Spike s : spikes) {
            s.draw(g);
        }
        for (FloorButton b : buttons) {
            b.draw(g);
        }
        door.draw(g);
    }
}

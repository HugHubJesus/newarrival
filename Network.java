import java.io.*;
import java.net.*;

// เล่นหลายคน: เครื่อง Server เป็นตัวกลาง รับข้อความจากทุก Client แล้วส่งต่อให้คนอื่นทุกคน
// ข้อความ 1 บรรทัด = "id ด่าน x y จำนวนครั้งที่ตาย เข้าประตูแล้ว(0/1)"
public class Network {
    public static final int MAX_PLAYERS = 5;   // มีไก่ 5 สี เลยเล่นได้สูงสุด 5 คน
    private static final int PORT = 50101;

    public int myId = 0;   // Server = 0, Client ได้เลขจาก Server ตอนต่อเข้ามา
    private boolean isServer;
    private PrintWriter toServer;                                       // ใช้ตอนเป็น Client
    private PrintWriter[] toClients = new PrintWriter[MAX_PLAYERS];    // ใช้ตอนเป็น Server (ช่องละคน ตาม id)

    // ข้อมูลผู้เล่นแต่ละคน ตามเลข id (friendLevel = 0 คือไม่มีคนนี้ หรือหลุดไปแล้ว)
    public int[] friendLevel = new int[MAX_PLAYERS];
    public int[] friendX = new int[MAX_PLAYERS];
    public int[] friendY = new int[MAX_PLAYERS];
    public int[] friendDeaths = new int[MAX_PLAYERS];
    public boolean[] friendAtDoor = new boolean[MAX_PLAYERS];

    // ip = null คือเปิดห้องเป็น Server, มี ip คือเข้าห้องเป็น Client
    public Network(String ip) throws IOException {
        if (ip == null) {
            isServer = true;
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server waiting on port " + PORT + " ...");

            // Thread คอยรับคนที่ต่อเข้ามาเรื่อยๆ ได้ id 1, 2, 3, 4
            Thread acceptor = new Thread(new Runnable() {
                public void run() {
                    for (int id = 1; id < MAX_PLAYERS; id++) {
                        try {
                            Socket socket = serverSocket.accept();
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("ID " + id);            // บอก Client ว่าได้เลขอะไร
                            toClients[id] = out;
                            System.out.println("Player " + (id + 1) + " connected");
                            startReceiver(new BufferedReader(new InputStreamReader(socket.getInputStream())), id);
                        } catch (IOException e) {
                        }
                    }
                }
            });
            acceptor.start();
        } else {
            Socket socket = new Socket(ip, PORT);             // ต่อไปหา Server
            toServer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String first = in.readLine();                     // บรรทัดแรก = "ID เลข"
            myId = Integer.parseInt(first.split(" ")[1]);
            System.out.println("Connected as player " + (myId + 1));
            startReceiver(in, 0);
        }
    }

    // Thread คอยรับข้อความจากอีกฝั่งตลอดเวลา (fromId = เลขของคนที่ต่อสายนี้อยู่)
    private void startReceiver(BufferedReader in, int fromId) {
        Thread receiver = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        read(line);
                        if (isServer) {
                            sendToClients(line, fromId);    // Server ส่งต่อให้คนอื่น
                        }
                    }
                } catch (IOException e) {
                }
                System.out.println("Player " + (fromId + 1) + " left the game");
                friendLevel[fromId] = 0;
                if (isServer) {
                    toClients[fromId] = null;
                    sendToClients(fromId + " 0 0 0 0 0", fromId);   // บอกคนอื่นว่าคนนี้หลุดแล้ว
                }
            }
        });
        receiver.start();
    }

    // แยกข้อความ แล้วเก็บไว้ในช่องของคนนั้น
    private void read(String line) {
        String[] part = line.split(" ");
        int id = Integer.parseInt(part[0]);
        friendX[id] = Integer.parseInt(part[2]);
        friendY[id] = Integer.parseInt(part[3]);
        friendDeaths[id] = Integer.parseInt(part[4]);
        friendAtDoor[id] = part[5].equals("1");
        friendLevel[id] = Integer.parseInt(part[1]);
    }

    private void sendToClients(String line, int skipId) {
        for (int i = 1; i < MAX_PLAYERS; i++) {
            if (i != skipId && toClients[i] != null) {
                toClients[i].println(line);
            }
        }
    }

    public void send(int level, int x, int y, int deaths, boolean atDoor) {
        int door = 0;
        if (atDoor) door = 1;
        String line = myId + " " + level + " " + x + " " + y + " " + deaths + " " + door;
        if (isServer) {
            sendToClients(line, 0);
        } else {
            toServer.println(line);
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import src.define.define_game;
import src.playMusic;

public class game_client2 extends Application implements define_game {
    public int [][]game_map=new int[13][15];
    public static HashMap<String, Image> imageCache=new HashMap<String, Image>();//用了一个HashMap，将Img缓存，避免重复加载资源，加快显示效率
    public GridPane pane = new GridPane();

    public boolean isContinueToPlay=true;//游戏是否终止

    private int playerID;//先链接的是用户1，操纵左上角火影；后链接的是用户2,操纵右下角孙悟空

    private boolean musicOn=false;//音乐开关，通过‘M’控制
    private int sendInfo=-1;
    playMusic p=new playMusic();
    // Input and output streams from/to server
    private DataInputStream fromServer;
    private DataOutputStream toServer;

    // Host name or ip
    private String host = "localhost";

    @Override // Override the start method in the Application class
    public void start(Stage primaryStage) {
        //设置pane的背景、长宽
        pane.setStyle("-fx-background-image: url(\"src/img.png\");");
        pane.setPrefHeight(650);
        pane.setPrefWidth(750);
        pane.setAlignment(Pos.TOP_LEFT);

        //上下左右，移动;空格，放炸弹，都放在sendInfo中，等一下发到服务器
        //M,本地控制音乐开关
        pane.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case S:
                    sendInfo=TRY_DOWN;
                    break;
                case W:
                    sendInfo=TRY_UP;
                    break;
                case A:
                    sendInfo=TRY_LEFT;
                    break;
                case D:
                    sendInfo=TRY_RIGHT;
                    break;
                case SPACE:
                    sendInfo=SET_BOMB;
                    break;
                case M:
                    musicOn=!musicOn;
                    break;
            }
        });
        Scene scene = new Scene(pane);
        primaryStage.setTitle("ZJU堂"); // Set the stage title
        primaryStage.setScene(scene); // Place the scene in the stage
        primaryStage.show(); // Display the stage

        pane.requestFocus();
        // Connect to the server
        connectToServer();
    }

    private void connectToServer() {
        try {
            // Create a socket to connect to the server
            Socket socket = new Socket(host, 8000);
            // Create an input stream to receive data from the server
            fromServer = new DataInputStream(socket.getInputStream());
            // Create an output stream to send data to the server
            toServer = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*线程1，监听服务器发来的消息，然后调用receiveInfoFromServer()实现*/
        new Thread(() -> {
            try {
                // 由服务器分配自己是用户几
                playerID = fromServer.readInt();
                //用户1是先来的，所以要等一下用户2
                if(playerID==PLAYER1)fromServer.readInt();
                for(int i=0;i<13;i++){
                    for(int j=0;j<15;j++){
                        game_map[i][j]=fromServer.readInt();
                    }
                }
                //初始绘制地图
                Platform.runLater(
                        () -> {
                            draw();
                        }
                );
                while(isContinueToPlay)receiveInfoFromServer();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
        /*线程2，发送消息给服务器，在sendInfo!=-1的情况下，即有按键做出了一些操作*/
        new Thread(() -> {
            try {
                while(true){
                    if(sendInfo!=-1){
                        System.out.print(sendInfo);
                        toServer.writeInt(sendInfo);
                        sendInfo=-1;
                    }
                    Thread.sleep(40);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
        /*线程3，音乐控制*/
        new Thread(() -> {
            while(isContinueToPlay){
                if(musicOn){
                    p.play();
                    try {
                        Thread.sleep(31500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /*监听的主要实现函数*/
    private void receiveInfoFromServer() throws IOException {
        // Receive game status
        int status = fromServer.readInt();
        //收到地图改变的信息
        if(status==CHANGED){
            for(int i=0;i<13;i++){
                for(int j=0;j<15;j++){
                    game_map[i][j]=fromServer.readInt();
                }
            }
            Platform.runLater(
                    () -> {
                        draw();
                    }
            );
        }
        //收到玩家胜利的信息
        else if(status==WIN_P1||status==WIN_P2){
            isContinueToPlay=false;
            for(int i=0;i<13;i++){
                for(int j=0;j<15;j++){
                    game_map[i][j]=fromServer.readInt();
                }
            }
            Platform.runLater(
                    () -> {
                        draw_end(status);
                    }
            );
        }
    }
    //游戏结束的draw函数，其实就是先调用draw()画地图，然后添加文本显示谁胜利
    public void draw_end(int event){
        draw();
        Label label= new Label ( "WIN_P1") ;
        if(event==WIN_P2)label= new Label ( "WIN_P2") ;
        label.setFont ( Font. font ( " Times New Roman " ,
                FontWeight. BOLD , FontPosture. ITALIC , 12 )) ;
        pane.add(label,7,8);
    }
    //draw函数,根据game_map绘制界面
    public void draw(){
        Image image1 = null;
        Image image2 = null;
        int num;
        pane.getChildren().clear();
        for(int i=0;i<13;i++){
            for(int j=0;j<15;j++){
                num= game_map[i][j];
                //System.out.print(num+" ");
                //有的点需要重叠显示,image1在下，image2在上
                switch (num) {
                    case BOX:
                        image1 = getImage("src/box.png");
                        break;
                    case WALL:
                        image1 = getImage("src/wall.png");
                        break;
                    case WELL:
                        image1 = getImage("src/well.png");
                        break;
                    case PLAYER1:
                        image1 = getImage("src/p1.png");
                        break;
                    case PLAYER2:
                        image1=getImage("src/p2.png");
                        break;
                    case BOMB1:
                        image1=getImage("src/bomb1.png");
                        break;
                    case BOMB2:
                        image1=getImage("src/bomb2.png");
                        break;
                    case BOMB_P1:
                        image1=getImage("src/bomb1.png");
                        image2=getImage("src/p1.png");
                        break;
                    case BOMB_P2:
                        image1=getImage("src/bomb2.png");
                        image2=getImage("src/p2.png");
                        break;
                    case THING_MORE:
                        image1=getImage("src/thing_more.png");
                        break;
                    case THING_STRONG:
                        image1=getImage("src/thing_stronger.png");
                        break;
                    case BOOM_THING: case BOOM:
                        image1=getImage("src/boom.png");
                        break;
                    case FLOOR:
                        image1=getImage("src/floor.png");
                        break;
                    case BOOM_P1:
                        image1=getImage("src/boom.png");
                        image2=getImage("src/p1.png");
                        break;
                    case BOOM_P2:
                        image1=getImage("src/boom.png");
                        image2=getImage("src/p2.png");
                        break;
                }
                if (image1 != null) {
                    ImageView img_v1 = new ImageView(image1);
                    img_v1.setFitWidth(LENGTH0);
                    img_v1.setFitHeight(LENGTH0);
                    pane.add(img_v1, j, i);
                }
                if (image2 != null) {
                    ImageView img_v1 = new ImageView(image2);
                    img_v1.setFitWidth(LENGTH0);
                    img_v1.setFitHeight(LENGTH0);
                    pane.add(img_v1, j, i);
                }
                image1=null;
                image2=null;
            }
        }
    }
    //获取图片函数，如果图片加载过，直接从imageCache中取，否则就加载到imageCache中
    public static Image getImage(String path) {
        Image image;
        image = imageCache.get(path);
        if (image == null) {
            image = new Image(path);
            imageCache.put(path, image);
        }
        return image;
    }
    public static void main(String[] args) {
        launch(args);
    }
}


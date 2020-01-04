import src.player;
import src.define.define_game;
import java.io.*;
import java.net.*;
import java.util.Date;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/*服务器接受客户端的上下左右、放炸弹的信息进行处理*/
/* 发回的信息是地图改变、某玩家胜利
*  然后将地图数组发回去，由客户端进行显示*/
public class game_server extends Application implements define_game{
    private int sessionNo = 1; // Number a session

    public int [][]game_map=new int[13][15];//游戏地图
    public int [][]game_map_life= new int[13][15];

    @Override // Override the start method in the Application class
    public void start(Stage primaryStage) {
        TextArea taLog = new TextArea();

        // Create a scene and place it in the stage
        Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
        primaryStage.setTitle("ZJU堂_Server"); // Set the stage title
        primaryStage.setScene(scene); // Place the scene in the stage
        primaryStage.show(); // Display the stage
        init_map();
        new Thread( () -> {
            try {
                // Create a server socket
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> taLog.appendText(new Date() +
                        ": Server started at socket 8000\n"));

                // Ready to create a session for every two players
                while (true) {
                    // Connect to player 1
                    Socket player1 = serverSocket.accept();

                    Platform.runLater(() -> {
                        taLog.appendText(new Date() + ": Player 1 joined session "
                                + sessionNo + '\n');
                        taLog.appendText("Player 1's IP address" +
                                player1.getInetAddress().getHostAddress() + '\n');
                    });

                    // Notify that the player is Player 1
                    new DataOutputStream(
                            player1.getOutputStream()).writeInt(PLAYER1);

                    // Connect to player 2
                    Socket player2 = serverSocket.accept();

                    Platform.runLater(() -> {
                        taLog.appendText(new Date() +
                                ": Player 2 joined session " + sessionNo + '\n');
                        taLog.appendText("Player 2's IP address" +
                                player2.getInetAddress().getHostAddress() + '\n');
                    });

                    // Notify that the player is Player 2
                    new DataOutputStream(
                            player2.getOutputStream()).writeInt(PLAYER2);
                    new DataOutputStream(
                            player1.getOutputStream()).writeInt(PLAYER2);

                    // Display this session and increment session number
                    Platform.runLater(() ->
                            taLog.appendText(new Date() +
                                    ": Start a thread for session " + sessionNo++ + '\n'));
                    // Launch a new thread for this session of two players
                    new Thread(new HandleASession(player1, player2)).start();
                }
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    //初始化地图,从map2.txt中将数组读出来，赋值给game_map
    public void init_map() {
        File file = new File("src//map2.txt");
        InputStreamReader read = null;
        try {
            read = new InputStreamReader(new FileInputStream(file));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        BufferedReader bufferedReader = new BufferedReader(read);
        String item;
        try {
            int i = 0, j = 0;
            while ((item = bufferedReader.readLine()) != null) {
                String[] ss = item.split("\\s+");
                j = 0;
                for (String s : ss) {
                    int num = Integer.parseInt(s);
                    game_map[i][j] = num;
                    j++;
                }
                i++;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // Define the thread class for handling a new session for two players
    class HandleASession implements Runnable {
        private Socket player1;
        private Socket player2;
        player p1=new player(1,3);
        player p2=new player(11,11);
        //时间检查时用到的，老时间、新时间
        long now_time,old_time;
        //时间差
        long change_time;
        int change_time_int;

        boolean is_change=false;

        private DataInputStream fromPlayer1;
        private DataOutputStream toPlayer1;
        private DataInputStream fromPlayer2;
        private DataOutputStream toPlayer2;

        public HandleASession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        /** Implement the run() method for the thread */
        public void run() {
            //首先将网络输出输入流置好
            try {
                fromPlayer1 = new DataInputStream(
                        player1.getInputStream());
                fromPlayer2 = new DataInputStream(
                        player2.getInputStream());
                toPlayer1 = new DataOutputStream(
                        player1.getOutputStream());
                toPlayer2 = new DataOutputStream(
                        player2.getOutputStream());
                for(int ki=0;ki<13;ki++){
                    for(int kj=0;kj<15;kj++){
                        toPlayer1.writeInt(game_map[ki][kj]);
                        toPlayer2.writeInt(game_map[ki][kj]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*线程1,监听客户端1发来的信息,并作回复*/
            new Thread( () -> {
                try {
                    int in_event;
                    int pos_thing;
                    //初始发一个CHANGED消息，以生成地图
                    toPlayer1.writeInt(CHANGED);
                    for(int ki=0;ki<13;ki++){
                        for(int kj=0;kj<15;kj++){
                            toPlayer1.writeInt(game_map[ki][kj]);
                        }
                    }
                    while (true) {
                        in_event=fromPlayer1.readInt();
                        //上下左右
                        if(in_event==TRY_UP||in_event==TRY_DOWN||in_event==TRY_LEFT||in_event==TRY_RIGHT){
                            //这里先调用player的move()函数,然后根据新的着陆点的信息，决定下一步
                            if(p1.move(in_event,true)){
                                pos_thing=game_map[p1.getRow()][p1.getCol()];

                                //如果是这些东西，就倒退回去
                                //2个相反的方向相加是205;false会赋值给player.changed,表示未改变位置
                                if(pos_thing==WALL||pos_thing==BOX||pos_thing==PLAYER2||pos_thing==PLAYER1||pos_thing==UNCHANGED_FLOOR||pos_thing==BOMB1||pos_thing==BOMB_P1||pos_thing==BOMB_P2||pos_thing==BOMB2){
                                    p1.move(205-in_event,false);
                                }
                                //吃到威力道具
                                else if(pos_thing==THING_STRONG)p1.change_power(1);
                                //吃到数量道具
                                else if(pos_thing==THING_MORE)p1.change_num(1);
                                //走到boom上,挂掉
                                else if(pos_thing==BOOM||pos_thing==BOOM_THING){
                                    p1.alive=false;
                                    game_map[p1.getRow()][p1.getCol()]=BOOM_P1;
                                }
                            }
                            //如果挂掉
                            if(!p1.alive){
                                //发送谁赢的信息
                                toPlayer1.writeInt(WIN_P2);
                                toPlayer2.writeInt(WIN_P2);
                                //发送地图
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                            //没挂,就正常走动，抹去前一个位置人物信息，把人移到后一个位置
                            else if(p1.changed){
                                p1.changed=false;

                                //原来站的是FLOOR,就恢复成FLOOR
                                if(game_map[p1.getOld_row()][p1.getOld_col()]==PLAYER1)
                                    game_map[p1.getOld_row()][p1.getOld_col()]=FLOOR;
                                //原来站在炸弹上，就恢复成BOMB
                                else if(game_map[p1.getOld_row()][p1.getOld_col()]==BOMB_P1)
                                    game_map[p1.getOld_row()][p1.getOld_col()]=BOMB1;

                                //将人移动到新位置
                                game_map[p1.getRow()][p1.getCol()]=PLAYER1;
                                //发送地图改变消息
                                toPlayer1.writeInt(CHANGED);
                                toPlayer2.writeInt(CHANGED);
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                        }
                        //空格 放炸弹
                        else if(in_event==SET_BOMB){
                            //如果数量不够
                            if(p1.getNum()<=0);
                            //同一个地方不能放2次炸弹
                            else if(game_map[p1.getRow()][p1.getCol()]==PLAYER1) {
                                p1.change_num(-1);
                                game_map[p1.getRow()][p1.getCol()]=BOMB_P1;

                                //炸弹life
                                game_map_life[p1.getRow()][p1.getCol()]=2400;

                                toPlayer1.writeInt(CHANGED);
                                toPlayer2.writeInt(CHANGED);
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                        }
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }).start();

            /*线程2,监听客户端2发来的消息,并作回复*/
            //与线程1逻辑相同
            new Thread( () -> {
                try {
                    int in_event;
                    int pos_thing;
                    toPlayer2.writeInt(CHANGED);
                    for(int ki=0;ki<13;ki++){
                        for(int kj=0;kj<15;kj++){
                            toPlayer2.writeInt(game_map[ki][kj]);
                        }
                    }
                    while (true) {
                        in_event=fromPlayer2.readInt();
                        if(in_event==TRY_UP||in_event==TRY_DOWN||in_event==TRY_LEFT||in_event==TRY_RIGHT){
                            if(p2.move(in_event,true)){
                                pos_thing=game_map[p2.getRow()][p2.getCol()];
                                if(pos_thing==WALL||pos_thing==BOX||pos_thing==PLAYER2||pos_thing==PLAYER1||pos_thing==UNCHANGED_FLOOR||pos_thing==BOMB1||pos_thing==BOMB_P1||pos_thing==BOMB_P2||pos_thing==BOMB2){
                                    p2.move(205-in_event,false);
                                }
                                else if(pos_thing==THING_STRONG)p2.change_power(1);
                                else if(pos_thing==THING_MORE)p2.change_num(1);
                                else if(pos_thing==BOOM||pos_thing==BOOM_THING){
                                    p2.alive=false;
                                    game_map[p2.getRow()][p2.getCol()]=BOOM_P2;
                                }
                            }
                            if(!p2.alive){
                                toPlayer1.writeInt(WIN_P1);
                                toPlayer2.writeInt(WIN_P1);
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                            else if(p2.changed){
                                p2.changed=false;
                                if(game_map[p2.getOld_row()][p2.getOld_col()]==PLAYER2)game_map[p2.getOld_row()][p2.getOld_col()]=FLOOR;
                                else if(game_map[p2.getOld_row()][p2.getOld_col()]==BOMB_P2)game_map[p2.getOld_row()][p2.getOld_col()]=BOMB2;
                                game_map[p2.getRow()][p2.getCol()]=PLAYER2;
                                toPlayer1.writeInt(CHANGED);
                                toPlayer2.writeInt(CHANGED);
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                        }
                        else if(in_event==SET_BOMB){
                            if(p2.getNum()<=0);
                            else if(game_map[p2.getRow()][p2.getCol()]==PLAYER2) {
                                p2.change_num(-1);
                                game_map[p2.getRow()][p2.getCol()]=BOMB_P2;
                                game_map_life[p2.getRow()][p2.getCol()]=2400;
                                toPlayer1.writeInt(CHANGED);
                                toPlayer2.writeInt(CHANGED);
                                for(int ki=0;ki<13;ki++){
                                    for(int kj=0;kj<15;kj++){
                                        toPlayer1.writeInt(game_map[ki][kj]);
                                        toPlayer2.writeInt(game_map[ki][kj]);
                                    }
                                }
                            }
                        }
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }).start();

            /*线程3,时间检查，如炸弹的爆炸、boom的产生消失*/
            new Thread( () -> {
                old_time=now_time=System.currentTimeMillis();
                while(true){
                    old_time=now_time;
                    now_time=System.currentTimeMillis();
                    change_time=now_time-old_time;
                    change_time_int=(int)change_time;
                    is_change=false;
                    //一格一格检查过来
                    for(int i=0;i<13;i++){
                        for(int j=0;j<15;j++){
                            //首先要检查的肯定是life>0的，所以life<=0的就不管了
                            if(game_map_life[i][j]<=0)continue;
                            else{
                                game_map_life[i][j]-=change_time_int;
                                if(game_map_life[i][j]<=0){
                                    if(!is_change)is_change=true;
                                    //如果是炸弹的话，就调用begin_boom开始你的炸弹秀
                                    if(game_map[i][j]==BOMB1||game_map[i][j]==BOMB2||game_map[i][j]==BOMB_P2||game_map[i][j]==BOMB_P1){
                                        begin_boom(i,j);
                                    }
                                    else if(game_map[i][j]==BOOM){
                                        game_map_life[i][j]=0;
                                        game_map[i][j]=FLOOR;
                                    }
                                    //BOOM_THING在boom消失后，会调用generate_thing()随机产生道具
                                    else if(game_map[i][j]==BOOM_THING){
                                        game_map[i][j]=generate_thing();
                                        game_map_life[i][j]=0;
                                    }
                                }
                            }
                        }
                    }
                    if(is_change){
                        for(int i=0;i<13;i++) {
                            for (int j = 0; j < 15; j++) {
                                if(game_map_life[i][j]==-999)game_map_life[i][j]=200;
                            }
                        }
                        try {
                            //如果玩家都没事
                            if(p1.alive&&p2.alive){
                                toPlayer1.writeInt(CHANGED);
                                toPlayer2.writeInt(CHANGED);
                            }
                            //如果玩家挂了
                            else if(!p1.alive){
                                toPlayer1.writeInt(WIN_P2);
                                toPlayer2.writeInt(WIN_P2);
                            }
                            else {
                                toPlayer1.writeInt(WIN_P2);
                                toPlayer2.writeInt(WIN_P2);
                            }
                            //发送地图
                            for(int ki=0;ki<13;ki++){
                                for(int kj=0;kj<15;kj++){
                                    toPlayer1.writeInt(game_map[ki][kj]);
                                    toPlayer2.writeInt(game_map[ki][kj]);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        /*爆炸函数，以一个炸弹为原点，产生一个十字扫描*/
        //其中做到了递归处理爆炸，因为一个炸弹可能引发别的炸弹
        private  void begin_boom(int x, int y){
            int length=1;
            //判断一下爆炸的炸弹是谁的，是否有人站在上面
            if(game_map[x][y]==BOMB1){
                length=p1.getPower();
                p1.change_num(1);
                game_map[x][y]=BOOM;
            }
            else if(game_map[x][y]==BOMB2){
                length=p2.getPower();
                p2.change_num(1);
                game_map[x][y]=BOOM;
            }
            else if(game_map[x][y]==BOMB_P1){
                game_map[x][y]=BOOM_P1;
                length=p1.getPower();
                p1.alive=false;
            }
            else if(game_map[x][y]==BOMB_P2){
                game_map[x][y]=BOOM_P2;
                length=p2.getPower();
                p2.alive=false;
            }
            game_map_life[x][y]=-999;/*这里将life先置为-999,否则在后续时间处理中可能会将刚生成的boom也处理
                                      故先置为-999,后面再统一置为200          */

            //接着炸弹产生的boom向四个方向延伸
            //向下
            for(int i=1;i<=length;i++){
                if(i+x>12)break;
                else if(game_map[i+x][y]==WALL||game_map[i+x][y]==UNCHANGED_FLOOR)break;
                //遇到BOX,将它破坏，boom也停止延伸
                else if(game_map[i+x][y]==BOX){
                    game_map[i+x][y]=BOOM_THING;
                    game_map_life[i+x][y]=-999;
                    break;
                }
                //遇到别的炸弹产生的boom，会将它时间重置
                else if(game_map[i+x][y]==FLOOR||game_map[i+x][y]==BOOM){
                    game_map[i+x][y]=BOOM;
                    game_map_life[i+x][y]=-999;
                }
                else if(game_map[i+x][y]==BOOM_THING){
                    game_map[i+x][y]=BOOM_THING;
                    game_map_life[i+x][y]=-999;
                }
                //遇到别的炸弹，引爆
                else if(game_map[i+x][y]==BOMB1||game_map[i+x][y]==BOMB2){
                    begin_boom(i+x,y);
                    break;
                }
                //遇到玩家，将它干掉
                else if(game_map[x+i][y]==PLAYER1){
                    p1.alive=false;
                    game_map[x+i][y]=BOOM_P1;
                }
                else if(game_map[x+i][y]==PLAYER2){
                    p2.alive=false;
                    game_map[x+i][y]=BOOM_P2;
                }
            }
            //向上
            for(int i=1;i<=length;i++){
                if(x-i<0)break;
                else if(game_map[x-i][y]==WALL||game_map[x-i][y]==UNCHANGED_FLOOR)break;
                else if(game_map[x-i][y]==BOX){
                    game_map[x-i][y]=BOOM_THING;
                    game_map_life[x-i][y]=-999;
                    break;
                }
                else if(game_map[x-i][y]==FLOOR||game_map[x-i][y]==BOOM){
                    game_map[x-i][y]=BOOM;
                    game_map_life[x-i][y]=-999;
                }
                else if(game_map[x-i][y]==BOOM_THING){
                    game_map[x-i][y]=BOOM_THING;
                    game_map_life[x-i][y]=-999;
                }
                else if(game_map[x-i][y]==BOMB1||game_map[x-i][y]==BOMB2||game_map[x-i][y]==BOMB_P2||game_map[x-i][y]==BOMB_P1){
                    begin_boom(x-i,y);
                    break;
                }
                else if(game_map[x-i][y]==PLAYER1){
                    p1.alive=false;
                    game_map[x-i][y]=BOOM_P1;
                }
                else if(game_map[x-i][y]==PLAYER2){
                    p2.alive=false;
                    game_map[x-i][y]=BOOM_P2;
                }
            }
            //向左
            for(int i=1;i<=length;i++){
                if(y-i<0)break;
                else if(game_map[x][y-i]==WALL||game_map[x][y-i]==UNCHANGED_FLOOR)break;
                else if(game_map[x][y-i]==BOX){
                    game_map[x][y-i]=BOOM_THING;
                    game_map_life[x][y-i]=-999;
                    break;
                }
                else if(game_map[x][y-i]==FLOOR||game_map[x][y-i]==BOOM){
                    game_map[x][y-i]=BOOM;
                    game_map_life[x][y-i]=-999;
                }
                else if(game_map[x][y-i]==BOOM_THING){
                    game_map[x][y-i]=BOOM_THING;
                    game_map_life[x][y-i]=-999;
                }
                else if(game_map[x][y-i]==BOMB1||game_map[x][y-i]==BOMB2){
                    begin_boom(x,y-i);
                    break;
                }
                else if(game_map[x][y-i]==PLAYER1){
                    p1.alive=false;
                    game_map[x][y-i]=BOOM_P1;
                }
                else if(game_map[x][y-i]==PLAYER2){
                    p2.alive=false;
                    game_map[x][y-i]=BOOM_P2;
                }
            }
            //向右
            for(int i=1;i<=length;i++){
                if(y+i>14)break;
                else if(game_map[x][y+i]==WALL||game_map[x][y+i]==UNCHANGED_FLOOR)break;
                else if(game_map[x][y+i]==BOX){
                    game_map[x][y+i]=BOOM_THING;
                    game_map_life[x][y+i]=-999;
                    break;
                }
                else if(game_map[x][y+i]==FLOOR||game_map[x][y+i]==BOOM){
                    game_map[x][y+i]=BOOM;
                    game_map_life[x][y+i]=-999;
                }
                else if(game_map[x][y+i]==BOOM_THING){
                    game_map[x][y+i]=BOOM_THING;
                    game_map_life[x][y+i]=-999;
                }
                else if(game_map[x][y+i]==BOMB1||game_map[x][y+i]==BOMB2){
                    begin_boom(x,y+i);
                    break;
                }
                else if(game_map[x][y+i]==PLAYER1){
                    p1.alive=false;
                    game_map[x][y+i]=BOOM_P1;
                }
                else if(game_map[x][y+i]==PLAYER2){
                    p2.alive=false;
                    game_map[x][y+i]=BOOM_P2;
                }
            }
        }
        //generate 道具,概率： 加数量1/6 加威力1/6
        private int generate_thing(){
            int num;
            num=(int)(Math.random()*6);
            if(num==1)return THING_MORE;
            if(num==5)return THING_STRONG;
            return FLOOR;
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}

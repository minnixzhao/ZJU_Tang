package src;
import src.define.*;
/*用户类*/
public class player implements define_game{
    private  int power=1;   //炸弹威力
    private  int num=1;  //炸弹数量
    private  int row;     //行数
    private int col;
    private int old_row;
    private int old_col;
    public boolean alive=true; //是否活着
    public boolean changed=false; //位置是否改变
    public player(int a,int b){
        row=a;
        col=b;
    }
    public int getPower(){
        return power;
    }
    public int getNum(){
        return num;
    }
    public void change_num(int i){
        num+=i;
    }
    public void change_power(int i){
        power+=i;
    }
    public int getRow(){
        return row;
    }
    public int getCol(){
        return col;
    }
    //移动函数，主要检测一下是否越界
    public boolean move(int dir,boolean flag){
        int new_row,new_col;
        if(dir==TRY_DOWN){
            new_row=row+1;
            new_col=col;
        }
        else if(dir==TRY_UP){
            new_row=row-1;
            new_col=col;
        }
        else if(dir==TRY_LEFT){
            new_row=row;
            new_col=col-1;
        }
        else{
            new_row=row;
            new_col=col+1;
        }
        if(new_row>12||new_row<0||new_col<0||new_col>14){
            return false;
        }
        old_col=col;
        old_row=row;
        row=new_row;
        col=new_col;
        changed=flag;
        return true;
    }
    public int getOld_row(){
        return old_row;
    }
    public int getOld_col(){
        return old_col;
    }
}

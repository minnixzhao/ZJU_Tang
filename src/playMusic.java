package src;
import java.io.File;
import javafx.scene.media.AudioClip;
public class playMusic {
    public static void play()
    {
        AudioClip ac;
        ac = new AudioClip(new File("src//src//music.mp3").toURI().toString());
        ac.play();   //开始播放
        ac.setCycleCount(500);
    }
}

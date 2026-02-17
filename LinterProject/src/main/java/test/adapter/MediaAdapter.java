package test.adapter;

/**
 * Adapter: Adapts Mp3Player to MediaPlayer interface
 * This is a PERFECT example of Adapter Pattern!
 */
public class MediaAdapter implements MediaPlayer {
    private Mp3Player mp3Player;  // The Adaptee

    public MediaAdapter() {
        this.mp3Player = new Mp3Player();
    }

    @Override
    public void play(String audioType, String fileName) {
        if (audioType.equalsIgnoreCase("mp3")) {
            // Delegating to the adaptee
            mp3Player.playMp3(fileName);
        }
    }
    
    public void adjustVolume(int level) {
        // Delegate to adaptee
        // (假设 mp3Player 有这个方法)
        System.out.println("Adjusting volume through mp3Player");
    }
}

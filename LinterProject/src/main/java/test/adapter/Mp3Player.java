package test.adapter;

/**
 * Adaptee: The existing class with incompatible interface
 */
public class Mp3Player {
    public void playMp3(String fileName) {
        System.out.println("Playing mp3 file: " + fileName);
    }
}

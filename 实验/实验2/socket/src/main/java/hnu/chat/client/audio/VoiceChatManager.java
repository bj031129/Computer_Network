package hnu.chat.client.audio;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class VoiceChatManager {
    private static final String MULTICAST_IP = "230.0.0.1";
    private static final int PORT = 5567;
    private static final int BUFFER_SIZE = 4096;
    
    private final String username;
    private MulticastSocket socket;
    private InetAddress group;
    private boolean isRunning = false;
    
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    
    public VoiceChatManager(String username) {
        this.username = username;
    }
    
    public void startCall() throws Exception {
        if (isRunning) return;
        
        // Setup Audio Format
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, true);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(micInfo) || !AudioSystem.isLineSupported(speakerInfo)) {
            throw new Exception("Microphone or Speakers not supported");
        }
        
        // Open Lines
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(format);
        microphone.start();
        
        speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakers.open(format);
        speakers.start();
        
        // Setup Network
        socket = new MulticastSocket(PORT);
        group = InetAddress.getByName(MULTICAST_IP);
        socket.joinGroup(group);
        socket.setLoopbackMode(false); // Enable loopback to receive own packets (we will filter manually)
        
        isRunning = true;
        
        // Start Threads
        new Thread(this::captureAndSend).start();
        new Thread(this::receiveAndPlay).start();
        
        System.out.println("Voice chat started on " + MULTICAST_IP + ":" + PORT);
    }
    
    public void stopCall() {
        if (!isRunning) return;
        isRunning = false;
        
        try {
            if (socket != null) {
                socket.leaveGroup(group);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (microphone != null) {
            microphone.close();
        }
        if (speakers != null) {
            speakers.close();
        }
        
        System.out.println("Voice chat stopped");
    }
    
    public boolean isCalling() {
        return isRunning;
    }
    
    private void captureAndSend() {
        byte[] audioBuffer = new byte[1024];
        byte[] userBytes = username.getBytes(StandardCharsets.UTF_8);
        int userLen = userBytes.length;
        
        while (isRunning) {
            try {
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    // Packet structure: [UserLen(1)][UserBytes][AudioData]
                    byte[] packetData = new byte[1 + userLen + bytesRead];
                    packetData[0] = (byte) userLen;
                    System.arraycopy(userBytes, 0, packetData, 1, userLen);
                    System.arraycopy(audioBuffer, 0, packetData, 1 + userLen, bytesRead);
                    
                    DatagramPacket packet = new DatagramPacket(
                        packetData, 
                        packetData.length, 
                        group, 
                        PORT
                    );
                    socket.send(packet);
                }
            } catch (Exception e) {
                if (isRunning) e.printStackTrace();
            }
        }
    }
    
    private void receiveAndPlay() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (isRunning) {
            try {
                socket.receive(packet);
                
                // Parse Packet
                byte[] data = packet.getData();
                int len = packet.getLength();
                
                if (len <= 1) continue;
                
                int userLen = data[0];
                if (userLen <= 0 || userLen >= len) continue;
                
                String senderName = new String(data, 1, userLen, StandardCharsets.UTF_8);
                
                // Filter own voice
                if (senderName.equals(username)) {
                    continue;
                }
                
                // Play audio
                int audioOffset = 1 + userLen;
                int audioLen = len - audioOffset;
                
                if (audioLen > 0) {
                    speakers.write(data, audioOffset, audioLen);
                }
                
            } catch (Exception e) {
                if (isRunning) e.printStackTrace();
            }
        }
    }
}

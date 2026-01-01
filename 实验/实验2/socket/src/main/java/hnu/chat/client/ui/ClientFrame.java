package hnu.chat.client.ui;

import hnu.chat.common.Constants;
import hnu.chat.common.Message;
import hnu.chat.client.network.ClientConnection;
import hnu.chat.client.audio.VoiceChatManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.Date;
import javax.imageio.ImageIO;
import java.util.Base64;

public class ClientFrame extends JFrame {
    private JPanel contentPane;
    private JLabel state;
    private JTextArea inputArea;
    private JScrollPane textJScrollPane, inputJScrollPane;
    private JButton send, clear, addButton;
    private JPopupMenu popupMenu;
    private JTextPane textPane;
    private SimpleAttributeSet right;
    
    private String username;
    private ClientConnection connection;
    private MessagePanel messagePanel;
    private VoiceChatManager voiceChatManager;

    public ClientFrame() {
        initializeFrame();
        getUserInfo();
        initializeComponents();
        setupListeners();
        initializeConnection();
    }

    private void initializeFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setResizable(false);
        setTitle("Chat Client");
        setLocationRelativeTo(null);
        
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);
    }

    private void getUserInfo() {
        username = JOptionPane.showInputDialog(this, 
            "请输入您的昵称", 
            "用户名", 
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            ""
        ).toString();

        String ip = JOptionPane.showInputDialog(this,
            "请输入服务器IP地址",
            "服务器地址",
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            ""
        ).toString();
        
        connection = new ClientConnection(username, ip);
    }

    private void initializeComponents() {
        // 初始化按钮
        clear = new JButton("清屏");
        clear.setBounds(56, 316, 123, 29);
        contentPane.add(clear);

        send = new JButton("发送");
        send.setBounds(217, 316, 84, 29);
        contentPane.add(send);

        addButton = new JButton("+");
        addButton.setBounds(301, 316, 39, 29);
        contentPane.add(addButton);

        // 初始化弹出菜单
        popupMenu = new JPopupMenu();
        JMenuItem sendFileItem = new JMenuItem("发送文件");
        JMenuItem sendImageItem = new JMenuItem("发送图片");
        JMenuItem sendAudioItem = new JMenuItem("发送音频");
        JMenuItem sendVideoItem = new JMenuItem("发送视频");
        JMenuItem voiceCallItem = new JMenuItem("语音通话");
        
        popupMenu.add(sendFileItem);
        popupMenu.add(sendImageItem);
        popupMenu.add(sendAudioItem);
        popupMenu.add(sendVideoItem);
        popupMenu.add(voiceCallItem);

        // 初始化消息面板
        messagePanel = new MessagePanel(username);
        messagePanel.setBounds(15, 37, 364, 179);
        contentPane.add(messagePanel);

        // 初始化输入区域
        inputArea = new JTextArea();
        inputArea.setBounds(15, 231, 364, 70);
        inputJScrollPane = new JScrollPane(inputArea);
        inputJScrollPane.setBounds(15, 231, 364, 70);
        contentPane.add(inputJScrollPane);

        // 设置欢迎标签
        state = new JLabel("欢迎您，" + username);
        state.setBounds(5, 5, 384, 21);
        state.setHorizontalAlignment(JLabel.LEFT);
        contentPane.add(state);
    }

    private void setupListeners() {
        send.addActionListener(e -> sendTextMessage());
        clear.addActionListener(e -> messagePanel.clear());
        addButton.addActionListener(e -> popupMenu.show(addButton, 0, addButton.getHeight()));
        
        popupMenu.getComponent(0).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                showFileChooser();
            }
        });

        popupMenu.getComponent(1).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                showImageChooser();
            }
        });

        // 音频发送监听器
        popupMenu.getComponent(2).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                showAudioChooser();
            }
        });

        // 视频发送监听器
        popupMenu.getComponent(3).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                showVideoChooser();
            }
        });

        // 语音通话监听器
        popupMenu.getComponent(4).addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                toggleVoiceCall((JMenuItem) e.getSource());
            }
        });
    }

    private void toggleVoiceCall(JMenuItem item) {
        if (voiceChatManager == null) {
            voiceChatManager = new VoiceChatManager(username);
        }

        if (voiceChatManager.isCalling()) {
            voiceChatManager.stopCall();
            item.setText("语音通话");
            JOptionPane.showMessageDialog(this, "语音通话已结束");
        } else {
            try {
                voiceChatManager.startCall();
                item.setText("结束通话");
                JOptionPane.showMessageDialog(this, "语音通话已开始");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "无法启动语音通话: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void sendTextMessage() {
        String content = inputArea.getText();
        if (content.isEmpty()) return;
        
        Message message = new Message(username, content, Message.MessageType.TEXT);
        connection.sendMessage(message);
        messagePanel.addMessage(message);
        inputArea.setText("");
    }

    private void showImageChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg")
                    || f.getName().toLowerCase().endsWith(".png")
                    || f.getName().toLowerCase().endsWith(".gif");
            }
            public String getDescription() {
                return "Image files (*.jpg, *.png, *.gif)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sendImage(fileChooser.getSelectedFile());
        }
    }

    private void sendImage(File imageFile) {
        try {
            if (imageFile.length() > Constants.MAX_IMAGE_SIZE) {
                JOptionPane.showMessageDialog(this, "图片大小不能超过2MB");
                return;
            }

            BufferedImage originalImage = ImageIO.read(imageFile);
            BufferedImage compressedImage = ImageHandler.compressImage(originalImage);
            String base64Image = ImageHandler.imageToBase64(compressedImage);
            
            if (base64Image.length() > Constants.MAX_BASE64_SIZE) {
                JOptionPane.showMessageDialog(this, "图片太大，请选择更小的图片");
                return;
            }

            Message message = new Message(username, base64Image, Message.MessageType.IMAGE);
            message.setFileName(imageFile.getName());
            connection.sendMessage(message);
            messagePanel.addMessage(message);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送图片失败");
            ex.printStackTrace();
        }
    }

    private void showFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sendFile(fileChooser.getSelectedFile());
        }
    }

    private void sendFile(File file) {
        try {
            if (file.length() > Constants.MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(this, "文件大小不能超过10MB");
                return;
            }

            byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
            String base64File = Base64.getEncoder().encodeToString(fileData);
            
            if (base64File.length() > Constants.MAX_BASE64_SIZE) {
                JOptionPane.showMessageDialog(this, "文件太大，请选择更小的文件");
                return;
            }

            Message message = new Message(username, base64File, Message.MessageType.FILE);
            message.setFileName(file.getName());
            connection.sendMessage(message);
            messagePanel.addMessage(message);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送文件失败");
            ex.printStackTrace();
        }
    }

    private void showAudioChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                for (String format : Constants.SUPPORTED_AUDIO_FORMATS) {
                    if (name.endsWith(format)) return true;
                }
                return false;
            }
            public String getDescription() {
                return "Audio files (*.mp3, *.wav, *.aac, *.m4a)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sendMedia(fileChooser.getSelectedFile(), Message.MessageType.AUDIO);
        }
    }

    private void showVideoChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                for (String format : Constants.SUPPORTED_VIDEO_FORMATS) {
                    if (name.endsWith(format)) return true;
                }
                return false;
            }
            public String getDescription() {
                return "Video files (*.mp4, *.avi, *.mkv, *.mov)";
            }
        });
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sendMedia(fileChooser.getSelectedFile(), Message.MessageType.VIDEO);
        }
    }

    private void sendMedia(File mediaFile, Message.MessageType mediaType) {
        try {
            // 检查文件大小
            if (mediaFile.length() > Constants.MAX_MEDIA_SIZE) {
                JOptionPane.showMessageDialog(this, "媒体文件大小不能超过50MB");
                return;
            }

            // 检查文件格式
            String fileName = mediaFile.getName().toLowerCase();
            boolean isValidFormat = false;
            String[] supportedFormats = (mediaType == Message.MessageType.AUDIO) ? 
                Constants.SUPPORTED_AUDIO_FORMATS : Constants.SUPPORTED_VIDEO_FORMATS;
            
            for (String format : supportedFormats) {
                if (fileName.endsWith(format)) {
                    isValidFormat = true;
                    break;
                }
            }
            
            if (!isValidFormat) {
                JOptionPane.showMessageDialog(this, 
                    "不支持的文件格式，支持的音频格式：" + String.join(", ", Constants.SUPPORTED_AUDIO_FORMATS),
                    "格式错误",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] mediaData = java.nio.file.Files.readAllBytes(mediaFile.toPath());
            String base64Media = Base64.getEncoder().encodeToString(mediaData);

            Message message = new Message(username, base64Media, mediaType);
            message.setFileName(mediaFile.getName());
            connection.sendMessage(message);
            messagePanel.addMessage(message);
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "发送媒体文件失败: " + ex.getMessage(),
                "发送失败",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void initializeConnection() {
        try {
            connection.connect();
            connection.setMessageListener(message -> {
                SwingUtilities.invokeLater(() -> messagePanel.addMessage(message));
            });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "连接服务器失败");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ClientFrame frame = new ClientFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
} 
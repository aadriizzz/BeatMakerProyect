import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*;

public class BeatMaker {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MenuFrame());
    }
}

class MenuFrame extends JFrame {
    public MenuFrame() {
        setTitle("BeatMaker - Menu");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(2, 1));

        JButton nuevo = new JButton("Nuevo Proyecto");
        JButton cargar = new JButton("Cargar Proyecto");

        add(nuevo);
        add(cargar);

        nuevo.addActionListener(e -> {
            new EditorFrame(null);
            dispose();
        });

        cargar.addActionListener(e -> {
            new EditorFrame(loadProject());
            dispose();
        });

        setVisible(true);
    }

    private boolean[][] loadProject() {
        try {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                boolean[][] grid = (boolean[][]) in.readObject();
                in.close();
                return grid;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class EditorFrame extends JFrame {
    private boolean[][] grid = new boolean[4][16];
    private int bpm = 120;
    private boolean playing = false;
    private int currentStep = 0;
    private boolean saved = true;

    private JButton[][] buttons = new JButton[4][16];

    private Clip kick, snare, hihatOpen, hihatClosed;

    private String[] soundNames = {"Bombo", "Caja", "HiHat Abierto", "HiHat Cerrado"};

    public EditorFrame(boolean[][] loadedGrid) {
        setTitle("Editor");
        setSize(1000, 450);
        setLayout(new BorderLayout());

        if (loadedGrid != null) {
            grid = loadedGrid;
        } else {
            saved = false;
        }

        // Aviso al cerrar
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!saved) {
                    int option = JOptionPane.showConfirmDialog(
                            EditorFrame.this,
                            "Tienes cambios sin guardar. ¿Seguro que quieres salir?",
                            "Aviso",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (option == JOptionPane.YES_OPTION) {
                        dispose();
                    }
                } else {
                    dispose();
                }
            }
        });

        loadSounds();

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel labelsPanel = new JPanel(new GridLayout(4, 1));
        JPanel gridPanel = new JPanel(new GridLayout(4, 16));

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(soundNames[i], SwingConstants.CENTER);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            labelsPanel.add(label);

            for (int j = 0; j < 16; j++) {
                JButton btn = new JButton();
                int row = i, col = j;
                btn.addActionListener(e -> {
                    grid[row][col] = !grid[row][col];
                    saved = false;
                    updateButtonColor(row, col);
                });
                buttons[i][j] = btn;
                updateButtonColor(i, j);
                gridPanel.add(btn);
            }
        }

        mainPanel.add(labelsPanel, BorderLayout.WEST);
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        JPanel controls = new JPanel();

        JButton play = new JButton("Play");
        JButton stop = new JButton("Stop");
        JButton save = new JButton("Guardar");
        JButton load = new JButton("Cargar");
        JButton newProject = new JButton("Nuevo");

        Integer[] bpmOptions = {60, 80, 100, 120, 140, 160};
        JComboBox<Integer> bpmBox = new JComboBox<>(bpmOptions);
        bpmBox.setSelectedItem(bpm);

        bpmBox.addActionListener(e -> {
            bpm = (int) bpmBox.getSelectedItem();
            saved = false;
        });

        controls.add(play);
        controls.add(stop);
        controls.add(new JLabel("BPM:"));
        controls.add(bpmBox);
        controls.add(save);
        controls.add(load);
        controls.add(newProject);

        play.addActionListener(e -> playBeat());
        stop.addActionListener(e -> playing = false);

        save.addActionListener(e -> saveProject());
        load.addActionListener(e -> loadFromChooser());

        newProject.addActionListener(e -> {
            if (!saved) {
                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Tienes cambios sin guardar. ¿Crear nuevo proyecto?",
                        "Aviso",
                        JOptionPane.YES_NO_OPTION
                );
                if (option != JOptionPane.YES_OPTION) return;
            }
            new EditorFrame(null);
            dispose();
        });

        add(mainPanel, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void updateButtonColor(int i, int j) {
        if (grid[i][j]) {
            buttons[i][j].setBackground(Color.GREEN);
        } else {
            buttons[i][j].setBackground(null);
        }
    }

    private void highlightStep(int step) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 16; j++) {
                    if (j == step) {
                        buttons[i][j].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    } else {
                        buttons[i][j].setBorder(UIManager.getBorder("Button.border"));
                    }
                }
            }
        });
    }

    private void loadSounds() {
        try {
            kick = loadClip("sounds/kick.wav");
            snare = loadClip("sounds/snare.wav");
            hihatOpen = loadClip("sounds/hihat_open.wav");
            hihatClosed = loadClip("sounds/hihat_closed.wav");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Clip loadClip(String path) throws Exception {
        AudioInputStream audio = AudioSystem.getAudioInputStream(new File(path));
        Clip clip = AudioSystem.getClip();
        clip.open(audio);
        return clip;
    }

    private void playClip(Clip clip) {
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    private void playBeat() {
        playing = true;
        new Thread(() -> {
            try {
                while (playing) {
                    int delay = 60000 / bpm / 4;
                    for (int step = 0; step < 16 && playing; step++) {
                        currentStep = step;
                        highlightStep(currentStep);

                        for (int i = 0; i < 4; i++) {
                            if (grid[i][step]) {
                                switch (i) {
                                    case 0: playClip(kick); break;
                                    case 1: playClip(snare); break;
                                    case 2: playClip(hihatOpen); break;
                                    case 3: playClip(hihatClosed); break;
                                }
                            }
                        }
                        Thread.sleep(delay);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveProject() {
        try {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
                out.writeObject(grid);
                out.close();
                saved = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFromChooser() {
        try {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                grid = (boolean[][]) in.readObject();
                in.close();

                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 16; j++) {
                        updateButtonColor(i, j);
                    }
                }
                saved = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
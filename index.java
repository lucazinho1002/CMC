import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class index extends JFrame {

    public index() {
        // Configurações da Janela
        setTitle("CollectionsMinecraft");
        setSize(854, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Painel Principal com desenho do Fundo e Logo
        JPanel painelMenu = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // 1. Fundo
                ImageIcon fundoIcon = new ImageIcon("assets/fund1.png");
                Image fundoImg = fundoIcon.getImage();
                g.drawImage(fundoImg, 0, 0, getWidth(), getHeight(), this);

                // 2. Logo
                ImageIcon logoIcon = new ImageIcon("assets/logo1.png");
                Image logoImg = logoIcon.getImage();
                
                int logoLargura = 400; 
                int logoAltura = 100;
                int logoX = (getWidth() - logoLargura) / 2;
                int logoY = 40;
                
                g.drawImage(logoImg, logoX, logoY, logoLargura, logoAltura, this);
            }
        };

        painelMenu.setLayout(null);

        // Estilo dos Botões
        int botaoLargura = 300;
        int botaoAltura = 40;
        int centroX = (854 - botaoLargura) / 2;

        // Botão Singleplayer
        JButton btnSingleplayer = new JButton("Singleplayer");
        btnSingleplayer.setBounds(centroX, 180, botaoLargura, botaoAltura);
        btnSingleplayer.addActionListener(e -> abrirTela("sp"));

        // Botão Multiplayer
        JButton btnMultiplayer = new JButton("Multiplayer");
        btnMultiplayer.setBounds(centroX, 230, botaoLargura, botaoAltura);
        btnMultiplayer.addActionListener(e -> abrirTela("mp"));

        // Botão Test (Abaixo do Multiplayer)
        JButton btnTest = new JButton("Test");
        btnTest.setBounds(centroX, 280, botaoLargura, botaoAltura);
        btnTest.addActionListener(e -> abrirTela("teste"));

        // Botão Options
        JButton btnOptions = new JButton("Options...");
        btnOptions.setBounds(centroX, 340, 145, botaoAltura);
        btnOptions.addActionListener(e -> abrirTela("options"));

        // Botão Quit Game
        JButton btnQuit = new JButton("Quit Game");
        btnQuit.setBounds(centroX + 155, 340, 145, botaoAltura);
        btnQuit.addActionListener(e -> System.exit(0));

        // Adiciona os botões
        painelMenu.add(btnSingleplayer);
        painelMenu.add(btnMultiplayer);
        painelMenu.add(btnTest);
        painelMenu.add(btnOptions);
        painelMenu.add(btnQuit);

        add(painelMenu);
    }

    // Chama qualquer tela sem dar erro de "cannot find symbol" na compilação
    private void abrirTela(String nomeClasse) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class<?> cls = Class.forName(nomeClasse);
                
                // Tenta chamar o main() da classe se existir
                try {
                    java.lang.reflect.Method mainMethod = cls.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[0]);
                } catch (NoSuchMethodException e) {
                    // Se não tiver main(), cria uma nova instância da janela
                    Object instancia = cls.getDeclaredConstructor().newInstance();
                    if (instancia instanceof JFrame) {
                        ((JFrame) instancia).setVisible(true);
                    }
                }

                this.dispose(); // Esconde o menu index
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir " + nomeClasse + ": " + ex.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new index().setVisible(true);
        });
    }
}
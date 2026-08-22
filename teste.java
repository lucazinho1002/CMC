import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class teste extends JPanel {

    private static final int LARGURA_TELA = 800;
    private static final int ALTURA_TELA = 600;
    private static final int TAMANHO_BLOCO = 32;

    private int jogadorX = 100;
    private int jogadorY = 100;

    private final int LARGURA_MAPA = LARGURA_TELA / TAMANHO_BLOCO;
    private final int ALTURA_MAPA = ALTURA_TELA / TAMANHO_BLOCO;
    private final int[][] mapa = new int[LARGURA_MAPA][ALTURA_MAPA];

    // Bloco selecionado no inventário (1 = Grama, 2 = Terra, 3 = Pedra)
    private int blocoMao = 1;

    public teste() {
        // Gerar mundo básico
        for (int x = 0; x < LARGURA_MAPA; x++) {
            for (int y = 0; y < ALTURA_MAPA; y++) {
                if (y > 14) {
                    mapa[x][y] = 3; // Pedra
                } else if (y > 11) {
                    mapa[x][y] = 2; // Terra
                } else if (y == 11) {
                    mapa[x][y] = 1; // Grama
                } else {
                    mapa[x][y] = 0; // Ar
                }
            }
        }

        setFocusable(true);

        // Teclado (Movimentação e troca de bloco na mão)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int tecla = e.getKeyCode();

                if (tecla == KeyEvent.VK_A || tecla == KeyEvent.VK_LEFT) jogadorX -= 8;
                if (tecla == KeyEvent.VK_D || tecla == KeyEvent.VK_RIGHT) jogadorX += 8;
                if (tecla == KeyEvent.VK_W || tecla == KeyEvent.VK_UP) jogadorY -= 8;
                if (tecla == KeyEvent.VK_S || tecla == KeyEvent.VK_DOWN) jogadorY += 8;

                // Teclas 1, 2, 3 para trocar o bloco ativo
                if (tecla == KeyEvent.VK_1) blocoMao = 1; // Grama
                if (tecla == KeyEvent.VK_2) blocoMao = 2; // Terra
                if (tecla == KeyEvent.VK_3) blocoMao = 3; // Pedra

                repaint();
            }
        });

        // Mouse (Quebrar e Colocar blocos)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int blocoX = e.getX() / TAMANHO_BLOCO;
                int blocoY = e.getY() / TAMANHO_BLOCO;

                // Garante que o clique foi dentro da grade
                if (blocoX >= 0 && blocoX < LARGURA_MAPA && blocoY >= 0 && blocoY < ALTURA_MAPA) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Botão Esquerdo: Quebrar bloco (Virar Ar)
                        mapa[blocoX][blocoY] = 0;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // Botão Direito: Colocar bloco selecionado
                        mapa[blocoX][blocoY] = blocoMao;
                    }
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Fundo (Céu)
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, LARGURA_TELA, ALTURA_TELA);

        // Renderizar Mapa
        for (int x = 0; x < LARGURA_MAPA; x++) {
            for (int y = 0; y < ALTURA_MAPA; y++) {
                int tipo = mapa[x][y];

                if (tipo == 1) g.setColor(new Color(34, 139, 34)); // Grama
                else if (tipo == 2) g.setColor(new Color(139, 69, 19)); // Terra
                else if (tipo == 3) g.setColor(Color.GRAY); // Pedra

                if (tipo != 0) {
                    g.fillRect(x * TAMANHO_BLOCO, y * TAMANHO_BLOCO, TAMANHO_BLOCO, TAMANHO_BLOCO);
                    g.setColor(new Color(0, 0, 0, 50));
                    g.drawRect(x * TAMANHO_BLOCO, y * TAMANHO_BLOCO, TAMANHO_BLOCO, TAMANHO_BLOCO);
                }
            }
        }

        // Jogador
        g.setColor(Color.RED);
        g.fillRect(jogadorX, jogadorY, TAMANHO_BLOCO - 4, TAMANHO_BLOCO - 4);

        // HUD - Bloco Selecionado na mão
        g.setColor(Color.BLACK);
        g.drawString("Bloco Selecionado (1-Grama, 2-Terra, 3-Pedra): " + blocoMao, 10, 20);
    }

    public static void main(String[] args) {
        JFrame janela = new JFrame("CollectionsMinecraft - Pre-Alpha");
        teste jogo = new teste();

        janela.add(jogo);
        janela.setSize(LARGURA_TELA, ALTURA_TELA);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setLocationRelativeTo(null);
        janela.setResizable(false);
        janela.setVisible(true);
    }
}
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;

public class client extends JFrame {

    private String caminhoMapa;
    
    private final int LARGURA_RENDER = 320;
    private final int ALTURA_RENDER = 240;

    private BufferedImage imagem;
    private int[] pixels;

    // Posição do Jogador
    private double posX = 5.5;
    private double posY = 5.5;
    private double posZ = 1.0; 
    private double velZ = 0.0;
    private boolean noChao = true;

    // Olhar em 3D
    private double dirX = -1.0, dirY = 0.0;
    private double planeX = 0.0, planeY = 0.66;
    private double pitch = 0.0;

    // Controles
    private boolean keyW, keyS, keyA, keyD;
    private boolean inventarioAberto = false;
    private int blocoSelecionado = 1;

    // Mapa 3D
    private final int MAP_WIDTH = 24;
    private final int MAP_HEIGHT = 24;
    private final int MAP_DEPTH = 5;
    private final int[][][] mapa = new int[MAP_WIDTH][MAP_HEIGHT][MAP_DEPTH];

    private Robot robot;

    public client(String caminhoMapa) {
        this.caminhoMapa = caminhoMapa;

        setTitle("CollectionsMinecraft - 3D Engine (" + caminhoMapa + ")");
        setSize(854, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        imagem = new BufferedImage(LARGURA_RENDER, ALTURA_RENDER, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) imagem.getRaster().getDataBuffer()).getData();

        if (!carregarMapaJson()) {
            gerarMapaInicial();
            salvarMapaJson();
        }

        setCursor(getToolkit().createCustomCursor(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        PainelRender painel = new PainelRender();
        add(painel);

        // Teclado
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_W) keyW = true;
                if (code == KeyEvent.VK_S) keyS = true;
                if (code == KeyEvent.VK_A) keyA = true;
                if (code == KeyEvent.VK_D) keyD = true;

                if (code == KeyEvent.VK_SPACE && noChao) {
                    velZ = 0.20;
                    noChao = false;
                }

                if (code == KeyEvent.VK_E) inventarioAberto = !inventarioAberto;
                if (code == KeyEvent.VK_1) blocoSelecionado = 1;
                if (code == KeyEvent.VK_2) blocoSelecionado = 2;
                if (code == KeyEvent.VK_3) blocoSelecionado = 3;

                if (code == KeyEvent.VK_ESCAPE) {
                    salvarMapaJson();
                    try {
                        new ProcessBuilder("java", "sp.java").start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    dispose();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_W) keyW = false;
                if (code == KeyEvent.VK_S) keyS = false;
                if (code == KeyEvent.VK_A) keyA = false;
                if (code == KeyEvent.VK_D) keyD = false;
            }
        });

        // Mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (inventarioAberto) return;

                int centroX = getX() + getWidth() / 2;
                int centroY = getY() + getHeight() / 2;

                int dx = e.getXOnScreen() - centroX;
                int dy = e.getYOnScreen() - centroY;

                if (dx != 0 || dy != 0) {
                    double rotSpeed = dx * -0.003;
                    double oldDirX = dirX;
                    dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
                    dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);

                    double oldPlaneX = planeX;
                    planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
                    planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);

                    pitch -= dy * 0.8;
                    if (pitch > 120) pitch = 120;
                    if (pitch < -120) pitch = -120;

                    robot.mouseMove(centroX, centroY);
                }
            }
        });

        // Clique
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (inventarioAberto) return;

                int[] alvo = raycastMira3D();
                if (alvo != null) {
                    int bx = alvo[0], by = alvo[1], bz = alvo[2];
                    int nx = alvo[3], ny = alvo[4], nz = alvo[5];

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        if (bz >= 0 && bz < MAP_DEPTH) {
                            mapa[bx][by][bz] = 0;
                            salvarMapaJson();
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT && nz >= 0 && nz < MAP_DEPTH) {
                            mapa[nx][ny][nz] = blocoSelecionado;
                            salvarMapaJson();
                        }
                    }
                }
            }
        });

        Timer timer = new Timer(16, e -> {
            atualizarJogador();
            renderizar3D();
            painel.repaint();
        });
        timer.start();
    }

    private void gerarMapaInicial() {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (x == 0 || x == MAP_WIDTH - 1 || y == 0 || y == MAP_HEIGHT - 1) {
                    mapa[x][y][0] = 3;
                } else {
                    mapa[x][y][0] = 0;
                }
            }
        }
    }

    private void atualizarJogador() {
        if (inventarioAberto) return;

        double moveSpeed = 0.08;

        int px = (int) posX;
        int py = (int) posY;
        int alturaBlocoChao = 0;

        for (int z = MAP_DEPTH - 1; z >= 0; z--) {
            if (px >= 0 && px < MAP_WIDTH && py >= 0 && py < MAP_HEIGHT) {
                if (mapa[px][py][z] > 0) {
                    alturaBlocoChao = z + 1;
                    break;
                }
            }
        }

        double chaoReal = alturaBlocoChao + 1.0;

        if (!noChao) {
            posZ += velZ;
            velZ -= 0.015;

            if (posZ <= chaoReal) {
                posZ = chaoReal;
                velZ = 0.0;
                noChao = true;
            }
        } else {
            if (posZ > chaoReal) {
                noChao = false;
            } else {
                posZ = chaoReal;
            }
        }

        double nextX = posX;
        double nextY = posY;

        if (keyW) { nextX += dirX * moveSpeed; nextY += dirY * moveSpeed; }
        if (keyS) { nextX -= dirX * moveSpeed; nextY -= dirY * moveSpeed; }
        if (keyA) { nextX -= planeX * moveSpeed; nextY -= planeY * moveSpeed; }
        if (keyD) { nextX += planeX * moveSpeed; nextY += planeY * moveSpeed; }

        int tx = (int) nextX;
        int ty = (int) nextY;
        int tz = (int) (posZ - 0.5);

        if (tx >= 0 && tx < MAP_WIDTH && ty >= 0 && ty < MAP_HEIGHT) {
            if (tz < 0 || tz >= MAP_DEPTH || mapa[tx][ty][tz] == 0) {
                posX = nextX;
                posY = nextY;
            }
        }
    }

    private void renderizar3D() {
        int corCeu = 0x78B4FF;
        int horizonte = (int) (ALTURA_RENDER / 2 + pitch);

        // Limpa a tela com o Céu
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = corCeu;
        }

        // Desenha APENAS o chão (Abaixo do horizonte)
        for (int y = Math.max(0, horizonte); y < ALTURA_RENDER; y++) {
            int p = y - horizonte;
            if (p == 0) continue;

            double rowDistance = posZ * ALTURA_RENDER / (2.0 * p);

            double rayDirX0 = dirX - planeX;
            double rayDirY0 = dirY - planeY;
            double rayDirX1 = dirX + planeX;
            double rayDirY1 = dirY + planeY;

            double floorStepX = rowDistance * (rayDirX1 - rayDirX0) / LARGURA_RENDER;
            double floorStepY = rowDistance * (rayDirY1 - rayDirY0) / LARGURA_RENDER;

            double floorX = posX + rowDistance * rayDirX0;
            double floorY = posY + rowDistance * rayDirY0;

            for (int x = 0; x < LARGURA_RENDER; ++x) {
                int cellX = (int) (floorX);
                int cellY = (int) (floorY);

                if (cellX >= 0 && cellX < MAP_WIDTH && cellY >= 0 && cellY < MAP_HEIGHT) {
                    int tipoChao = mapa[cellX][cellY][0];
                    int corChao = 0x338833; // Grama

                    if (tipoChao == 2) corChao = 0x9B5523; // Terra
                    else if (tipoChao == 3) corChao = 0xAAAAAA; // Pedra

                    pixels[y * LARGURA_RENDER + x] = corChao;
                }

                floorX += floorStepX;
                floorY += floorStepY;
            }
        }

        // Desenha Paredes dos Blocos
        for (int z = 0; z < MAP_DEPTH; z++) {
            for (int x = 0; x < LARGURA_RENDER; x++) {
                double cameraX = 2 * x / (double) LARGURA_RENDER - 1;
                double rayDirX = dirX + planeX * cameraX;
                double rayDirY = dirY + planeY * cameraX;

                int mapX = (int) posX;
                int mapY = (int) posY;

                double deltaDistX = Math.abs(1 / rayDirX);
                double deltaDistY = Math.abs(1 / rayDirY);

                double sideDistX, sideDistY;
                int stepX, stepY;
                boolean hit = false;
                int side = 0;

                if (rayDirX < 0) { stepX = -1; sideDistX = (posX - mapX) * deltaDistX; }
                else { stepX = 1; sideDistX = (mapX + 1.0 - posX) * deltaDistX; }

                if (rayDirY < 0) { stepY = -1; sideDistY = (posY - mapY) * deltaDistY; }
                else { stepY = 1; sideDistY = (mapY + 1.0 - posY) * deltaDistY; }

                while (!hit) {
                    if (sideDistX < sideDistY) {
                        sideDistX += deltaDistX;
                        mapX += stepX;
                        side = 0;
                    } else {
                        sideDistY += deltaDistY;
                        mapY += stepY;
                        side = 1;
                    }

                    if (mapX >= 0 && mapX < MAP_WIDTH && mapY >= 0 && mapY < MAP_HEIGHT) {
                        if (mapa[mapX][mapY][z] > 0) hit = true;
                    } else {
                        break;
                    }
                }

                if (hit) {
                    double perpWallDist;
                    if (side == 0) perpWallDist = (mapX - posX + (1 - stepX) / 2) / rayDirX;
                    else perpWallDist = (mapY - posY + (1 - stepY) / 2) / rayDirY;

                    int lineHeight = (int) (ALTURA_RENDER / perpWallDist);
                    int posZOffset = (int) (((posZ - z) * ALTURA_RENDER) / perpWallDist);

                    int drawStart = -lineHeight / 2 + horizonte + posZOffset;
                    if (drawStart < 0) drawStart = 0;

                    int drawEnd = lineHeight / 2 + horizonte + posZOffset;
                    if (drawEnd >= ALTURA_RENDER) drawEnd = ALTURA_RENDER - 1;

                    int tipo = mapa[mapX][mapY][z];
                    int corBloco = 0x888888;
                    if (tipo == 1) corBloco = 0x228B22;
                    else if (tipo == 2) corBloco = 0x8B4513;

                    if (side == 1) {
                        corBloco = (corBloco >> 1) & 0x7F7F7F;
                    }

                    for (int y = drawStart; y <= drawEnd; y++) {
                        pixels[y * LARGURA_RENDER + x] = corBloco;
                    }
                }
            }
        }
    }

    private int[] raycastMira3D() {
        double currX = posX;
        double currY = posY;
        double currZ = posZ;

        int lastX = (int) currX;
        int lastY = (int) currY;
        int lastZ = (int) currZ;

        for (int i = 0; i < 50; i++) {
            currX += dirX * 0.1;
            currY += dirY * 0.1;
            currZ += (pitch / 120.0) * 0.1;

            int bx = (int) currX;
            int by = (int) currY;
            int bz = (int) currZ;

            if (bx >= 0 && bx < MAP_WIDTH && by >= 0 && by < MAP_HEIGHT && bz >= 0 && bz < MAP_DEPTH) {
                if (mapa[bx][by][bz] > 0) {
                    return new int[]{bx, by, bz, lastX, lastY, lastZ};
                }
            }
            lastX = bx; lastY = by; lastZ = bz;
        }
        return null;
    }

    private boolean carregarMapaJson() {
        File file = new File(caminhoMapa);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = reader.readLine()) != null) {
                sb.append(linha);
            }
            String conteudo = sb.toString();
            int inicioGrid = conteudo.indexOf("[");
            int fimGrid = conteudo.lastIndexOf("]");

            if (inicioGrid != -1 && fimGrid != -1) {
                String apenasGrid = conteudo.substring(inicioGrid, fimGrid + 1);
                apenasGrid = apenasGrid.replaceAll("[^0-9,]", " ");
                String[] numeros = apenasGrid.trim().split("\\s+");

                int idx = 0;
                for (int x = 0; x < MAP_WIDTH; x++) {
                    for (int y = 0; y < MAP_HEIGHT; y++) {
                        if (idx < numeros.length && !numeros[idx].isEmpty()) {
                            mapa[x][y][0] = Integer.parseInt(numeros[idx]);
                            idx++;
                        }
                    }
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private void salvarMapaJson() {
        File file = new File(caminhoMapa);
        File pasta = file.getParentFile();
        if (pasta != null && !pasta.exists()) pasta.mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("{\n");
            writer.write("  \"grid\": [\n");
            for (int x = 0; x < MAP_WIDTH; x++) {
                writer.write("    [");
                for (int y = 0; y < MAP_HEIGHT; y++) {
                    writer.write(mapa[x][y][0] + (y < MAP_HEIGHT - 1 ? ", " : ""));
                }
                writer.write("]" + (x < MAP_WIDTH - 1 ? ",\n" : "\n"));
            }
            writer.write("  ]\n");
            writer.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class PainelRender extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.drawImage(imagem, 0, 0, getWidth(), getHeight(), null);

            // Mira (+)
            g.setColor(Color.WHITE);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g.drawLine(cx - 10, cy, cx + 10, cy);
            g.drawLine(cx, cy - 10, cx, cy + 10);

            // Hotbar
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(getWidth() / 2 - 120, getHeight() - 50, 240, 40);
            g.setColor(Color.WHITE);
            String nomeBloco = (blocoSelecionado == 1) ? "Grama" : (blocoSelecionado == 2) ? "Terra" : "Pedra";
            g.drawString("Bloco Ativo [1-3]: " + nomeBloco, getWidth() / 2 - 90, getHeight() - 25);

            // Inventário (E)
            if (inventarioAberto) {
                g.setColor(new Color(0, 0, 0, 220));
                g.fillRect(200, 100, 454, 200);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.drawString("INVENTÁRIO (Teclas 1, 2 ou 3)", 240, 140);

                g.setColor(new Color(34, 139, 34)); g.fillRect(250, 180, 50, 50);
                g.setColor(new Color(139, 69, 19)); g.fillRect(330, 180, 50, 50);
                g.setColor(Color.GRAY);            g.fillRect(410, 180, 50, 50);
            }
        }
    }

    public static void main(String[] args) {
        String mapa = (args.length > 0) ? args[0] : "maps/Mundo/map.json";
        SwingUtilities.invokeLater(() -> new client(mapa).setVisible(true));
    }
}
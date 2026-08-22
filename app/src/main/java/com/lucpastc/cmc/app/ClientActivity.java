package com.lucpastc.cmc.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClientActivity extends AppCompatActivity {

    private String caminhoMapa;

    private final int LARGURA_RENDER = 320;
    private final int ALTURA_RENDER = 240;

    private Bitmap imagem;
    private int[] pixels;

    private double posX = 5.5;
    private double posY = 5.5;
    private double posZ = 1.0;
    private double velZ = 0.0;
    private boolean noChao = true;

    private double dirX = -1.0, dirY = 0.0;
    private double planeX = 0.0, planeY = 0.66;
    private double pitch = 0.0;

    private boolean keyW, keyS, keyA, keyD;
    private boolean inventarioAberto = false;
    private int blocoSelecionado = 1;

    private final int MAP_WIDTH = 24;
    private final int MAP_HEIGHT = 24;
    private final int MAP_DEPTH = 5;
    private final int[][][] mapa = new int[MAP_WIDTH][MAP_HEIGHT][MAP_DEPTH];

    private float lastTouchX, lastTouchY;
    private PainelRender painelRender;
    private Handler loopHandler = new Handler();
    private Runnable loopRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        caminhoMapa = getIntent().getStringExtra("caminhoMapa");
        if (caminhoMapa == null) {
            caminhoMapa = new File(getExternalFilesDir(null), "maps/Mundo/map.json").getAbsolutePath();
        }

        imagem = Bitmap.createBitmap(LARGURA_RENDER, ALTURA_RENDER, Bitmap.Config.ARGB_8888);
        pixels = new int[LARGURA_RENDER * ALTURA_RENDER];

        if (!carregarMapaJson()) {
            gerarMapaInicial();
            salvarMapaJson();
        }

        FrameLayout layoutPrincipal = new FrameLayout(this);
        painelRender = new PainelRender(this);
        layoutPrincipal.addView(painelRender);

        View controlesOverlay = getLayoutInflater().inflate(R.layout.overlay_controles, null);
        layoutPrincipal.addView(controlesOverlay);

        setContentView(layoutPrincipal);

        configurarBotoesTouch(controlesOverlay);

        loopRunnable = new Runnable() {
            @Override
            public void run() {
                atualizarJogador();
                renderizar3D();
                painelRender.invalidate();
                loopHandler.postDelayed(this, 16);
            }
        };
        loopHandler.post(loopRunnable);
    }

    private void configurarBotoesTouch(View root) {
        configurarTouchBotao(root.findViewById(R.id.btnW), val -> keyW = val);
        configurarTouchBotao(root.findViewById(R.id.btnS), val -> keyS = val);
        configurarTouchBotao(root.findViewById(R.id.btnA), val -> keyA = val);
        configurarTouchBotao(root.findViewById(R.id.btnD), val -> keyD = val);

        Button btnPular = root.findViewById(R.id.btnPular);
        btnPular.setOnClickListener(v -> {
            if (noChao) {
                velZ = 0.20;
                noChao = false;
            }
        });

        Button btnQuebrar = root.findViewById(R.id.btnQuebrar);
        btnQuebrar.setOnClickListener(v -> acaoQuebrar());

        Button btnColocar = root.findViewById(R.id.btnColocar);
        btnColocar.setOnClickListener(v -> acaoColocar());

        Button btnInventario = root.findViewById(R.id.btnInventario);
        btnInventario.setOnClickListener(v -> inventarioAberto = !inventarioAberto);

        Button btnBloco1 = root.findViewById(R.id.btnBloco1);
        Button btnBloco2 = root.findViewById(R.id.btnBloco2);
        Button btnBloco3 = root.findViewById(R.id.btnBloco3);

        btnBloco1.setOnClickListener(v -> blocoSelecionado = 1);
        btnBloco2.setOnClickListener(v -> blocoSelecionado = 2);
        btnBloco3.setOnClickListener(v -> blocoSelecionado = 3);

        Button btnSair = root.findViewById(R.id.btnSair);
        btnSair.setOnClickListener(v -> {
            salvarMapaJson();
            loopHandler.removeCallbacks(loopRunnable);
            Intent intent = new Intent(ClientActivity.this, SpActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private interface BotaoState {
        void onChange(boolean pressionado);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarTouchBotao(View btn, BotaoState state) {
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    state.onChange(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    state.onChange(false);
                    return true;
            }
            return false;
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (inventarioAberto) return super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                double rotSpeed = dx * -0.005;
                double oldDirX = dirX;
                dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
                dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);

                double oldPlaneX = planeX;
                planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
                planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);

                pitch -= dy * 0.8;
                if (pitch > 120) pitch = 120;
                if (pitch < -120) pitch = -120;

                lastTouchX = x;
                lastTouchY = y;
                break;
        }
        return true;
    }

    private void acaoQuebrar() {
        if (inventarioAberto) return;
        int[] alvo = raycastMira3D();
        if (alvo != null) {
            int bx = alvo[0], by = alvo[1], bz = alvo[2];
            if (bz >= 0 && bz < MAP_DEPTH) {
                mapa[bx][by][bz] = 0;
                salvarMapaJson();
            }
        }
    }

    private void acaoColocar() {
        if (inventarioAberto) return;
        int[] alvo = raycastMira3D();
        if (alvo != null) {
            int nx = alvo[3], ny = alvo[4], nz = alvo[5];
            if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT && nz >= 0 && nz < MAP_DEPTH) {
                mapa[nx][ny][nz] = blocoSelecionado;
                salvarMapaJson();
            }
        }
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
        int corCeu = 0xFF78B4FF;
        int horizonte = (int) (ALTURA_RENDER / 2 + pitch);

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = corCeu;
        }

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
                    int corChao = 0xFF338833;

                    if (tipoChao == 2) corChao = 0xFF9B5523;
                    else if (tipoChao == 3) corChao = 0xFFAAAAAA;

                    pixels[y * LARGURA_RENDER + x] = corChao;
                }

                floorX += floorStepX;
                floorY += floorStepY;
            }
        }

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
                    int corBloco = 0xFF888888;
                    if (tipo == 1) corBloco = 0xFF228B22;
                    else if (tipo == 2) corBloco = 0xFF8B4513;

                    if (side == 1) {
                        corBloco = (corBloco & 0xFF000000) | ((corBloco >> 1) & 0x007F7F7F);
                    }

                    for (int y = drawStart; y <= drawEnd; y++) {
                        pixels[y * LARGURA_RENDER + x] = corBloco;
                    }
                }
            }
        }
        imagem.setPixels(pixels, 0, LARGURA_RENDER, 0, 0, LARGURA_RENDER, ALTURA_RENDER);
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

    private class PainelRender extends View {
        private Paint paint = new Paint();

        public PainelRender(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawBitmap(imagem, null, new android.graphics.Rect(0, 0, getWidth(), getHeight()), paint);

            // Mira (+)
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(3);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            canvas.drawLine(cx - 20, cy, cx + 20, cy, paint);
            canvas.drawLine(cx, cy - 20, cx, cy + 20, paint);

            // Hotbar
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(getWidth() / 2f - 200, getHeight() - 100, getWidth() / 2f + 200, getHeight() - 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(36);
            String nomeBloco = (blocoSelecionado == 1) ? "Grama" : (blocoSelecionado == 2) ? "Terra" : "Pedra";
            canvas.drawText("Bloco Ativo [1-3]: " + nomeBloco, getWidth() / 2f - 180, getHeight() - 45, paint);

            // Inventário (E)
            if (inventarioAberto) {
                paint.setColor(Color.argb(220, 0, 0, 0));
                canvas.drawRect(100, 100, getWidth() - 100, getHeight() - 100, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(40);
                canvas.drawText("INVENTÁRIO (Toque nos botões 1, 2 ou 3)", 140, 160, paint);

                paint.setColor(Color.rgb(34, 139, 34));
                canvas.drawRect(200, 220, 300, 320, paint);

                paint.setColor(Color.rgb(139, 69, 19));
                canvas.drawRect(340, 220, 440, 320, paint);

                paint.setColor(Color.GRAY);
                canvas.drawRect(480, 220, 580, 320, paint);
            }
        }
    }
}
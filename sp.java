import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class sp extends JFrame {

    private DefaultListModel<String> listaModel;
    private JList<String> listaMundos;

    public sp() {
        setTitle("CollectionsMinecraft - Selecionar Mundo");
        setSize(854, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Selecione um Mundo", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        painelPrincipal.add(titulo, BorderLayout.NORTH);

        listaModel = new DefaultListModel<>();
        listaMundos = new JList<>(listaModel);
        listaMundos.setFont(new Font("Arial", Font.PLAIN, 18));
        
        carregarMundos();

        JScrollPane scrollPane = new JScrollPane(listaMundos);
        painelPrincipal.add(scrollPane, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout());

        JButton btnJogar = new JButton("Jogar no Mundo Selecionado");
        JButton btnCriar = new JButton("Criar Novo Mundo");
        JButton btnVoltar = new JButton("Voltar ao Menu");

        btnJogar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCriar.setFont(new Font("Arial", Font.BOLD, 14));
        btnVoltar.setFont(new Font("Arial", Font.BOLD, 14));

        btnJogar.addActionListener(e -> jogarMundo());
        btnCriar.addActionListener(e -> criarNovoMundo());
        btnVoltar.addActionListener(e -> voltarMenu());

        painelBotoes.add(btnJogar);
        painelBotoes.add(btnCriar);
        painelBotoes.add(btnVoltar);

        painelPrincipal.add(painelBotoes, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void carregarMundos() {
        listaModel.clear();
        File pastaMaps = new File("maps");
        if (!pastaMaps.exists()) {
            pastaMaps.mkdirs();
        }

        File[] mundos = pastaMaps.listFiles(File::isDirectory);
        if (mundos != null) {
            for (File mundo : mundos) {
                listaModel.addElement(mundo.getName());
            }
        }
    }

    private void criarNovoMundo() {
        String nomeMundo = JOptionPane.showInputDialog(this, "Digite o nome do novo mundo:", "Criar Mundo", JOptionPane.QUESTION_MESSAGE);

        if (nomeMundo != null && !nomeMundo.trim().isEmpty()) {
            nomeMundo = nomeMundo.trim();
            File pastaNovoMundo = new File("maps/" + nomeMundo);

            if (pastaNovoMundo.exists()) {
                JOptionPane.showMessageDialog(this, "Já existe um mundo com esse nome!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pastaNovoMundo.mkdirs()) {
                criarMapJson(pastaNovoMundo);
                carregarMundos();
                listaMundos.setSelectedValue(nomeMundo, true);
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível criar a pasta do mundo.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Gera o map.json compatível com a matriz 24x24 do client.java
    private void criarMapJson(File pastaMundo) {
        File arquivoMap = new File(pastaMundo, "map.json");
        int tamX = 24;
        int tamY = 24;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoMap))) {
            writer.write("{\n");
            writer.write("  \"grid\": [\n");

            for (int x = 0; x < tamX; x++) {
                writer.write("    [");
                for (int y = 0; y < tamY; y++) {
                    // Paredes de pedra (3) nos limites do mapa, espaço vazio (0) no meio
                    int tipoBloco = (x == 0 || x == tamX - 1 || y == 0 || y == tamY - 1) ? 3 : 0;
                    writer.write(tipoBloco + (y < tamY - 1 ? ", " : ""));
                }
                writer.write("]" + (x < tamX - 1 ? ",\n" : "\n"));
            }

            writer.write("  ]\n");
            writer.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void jogarMundo() {
        String mundoSelecionado = listaMundos.getSelectedValue();
        if (mundoSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um mundo para jogar!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String caminhoMapa = "maps/" + mundoSelecionado + "/map.json";

        try {
            new ProcessBuilder("java", "client.java", caminhoMapa).start();
            dispose();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao iniciar o client.java!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void voltarMenu() {
        try {
            new ProcessBuilder("java", "menu.java").start();
            dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new sp().setVisible(true));
    }
}
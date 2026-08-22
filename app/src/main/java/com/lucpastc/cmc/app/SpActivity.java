package com.lucpastc.cmc.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SpActivity extends AppCompatActivity {

    private ArrayList<String> listaMundos;
    private ArrayAdapter<String> adapter;
    private ListView listViewMundos;
    private String mundoSelecionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp);

        listViewMundos = findViewById(R.id.listViewMundos);
        Button btnJogar = findViewById(R.id.btnJogar);
        Button btnCriar = findViewById(R.id.btnCriar);
        Button btnVoltar = findViewById(R.id.btnVoltar);

        listaMundos = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, listaMundos);
        listViewMundos.setAdapter(adapter);
        listViewMundos.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listViewMundos.setOnItemClickListener((parent, view, position, id) ->
                mundoSelecionado = listaMundos.get(position)
        );

        carregarMundos();

        btnJogar.setOnClickListener(v -> jogarMundo());
        btnCriar.setOnClickListener(v -> criarNovoMundo());
        btnVoltar.setOnClickListener(v -> voltarMenu());
    }

    private void carregarMundos() {
        listaMundos.clear();
        File pastaMaps = new File(getExternalFilesDir(null), "maps");
        if (!pastaMaps.exists()) {
            pastaMaps.mkdirs();
        }

        File[] mundos = pastaMaps.listFiles(File::isDirectory);
        if (mundos != null) {
            for (File mundo : mundos) {
                listaMundos.add(mundo.getName());
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void criarNovoMundo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Criar Mundo");
        builder.setMessage("Digite o nome do novo mundo:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String nomeMundo = input.getText().toString().trim();
            if (!nomeMundo.isEmpty()) {
                File pastaNovoMundo = new File(getExternalFilesDir(null), "maps/" + nomeMundo);

                if (pastaNovoMundo.exists()) {
                    Toast.makeText(this, "Já existe um mundo com esse nome!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pastaNovoMundo.mkdirs()) {
                    criarMapJson(pastaNovoMundo);
                    carregarMundos();
                } else {
                    Toast.makeText(this, "Não foi possível criar a pasta do mundo.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

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
        if (mundoSelecionado == null) {
            Toast.makeText(this, "Selecione um mundo para jogar!", Toast.LENGTH_SHORT).show();
            return;
        }

        String caminhoMapa = new File(getExternalFilesDir(null), "maps/" + mundoSelecionado + "/map.json").getAbsolutePath();

        Intent intent = new Intent(SpActivity.this, ClientActivity.class);
        intent.putExtra("caminhoMapa", caminhoMapa);
        startActivity(intent);
        finish();
    }

    private void voltarMenu() {
        Intent intent = new Intent(SpActivity.this, IndexActivity.class);
        startActivity(intent);
        finish();
    }
}
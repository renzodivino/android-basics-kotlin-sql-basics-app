package com.example.sqlbasics

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: ArrayAdapter<String>
    private var listaClientes: List<Cliente> = emptyList()
    private var clienteEmEdicao: Cliente? = null

    private lateinit var etNome: EditText
    private lateinit var etTelefone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etSenha: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnLimpar: Button
    private lateinit var lvClientes: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        inicializarViews()
        configurarListView()
        configurarBotoes()
        carregarLista()
    }

    private fun inicializarViews() {
        etNome = findViewById(R.id.etNome)
        etTelefone = findViewById(R.id.etTelefone)
        etEmail = findViewById(R.id.etEmail)
        etSenha = findViewById(R.id.etSenha)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnLimpar = findViewById(R.id.btnLimpar)
        lvClientes = findViewById(R.id.lvClientes)
    }

    private fun configurarListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList())
        lvClientes.adapter = adapter


        lvClientes.setOnItemClickListener { _, _, position, _ ->
            val cliente = listaClientes[position]
            preencherCamposParaEdicao(cliente)
        }

            lvClientes.setOnItemLongClickListener { _, _, position, _ ->
            val cliente = listaClientes[position]
            confirmarExclusao(cliente)
            true
        }
    }

    private fun configurarBotoes() {
        btnSalvar.setOnClickListener { salvarOuAtualizar() }
        btnLimpar.setOnClickListener { limparFormulario() }
    }


    private fun carregarLista() {
        lifecycleScope.launch {
            listaClientes = db.clienteDao().listarTodos()

            val dadosFormatados = listaClientes.map { cliente ->
                "${cliente.id} - ${cliente.nome} | ${cliente.email}"
            }

            adapter.clear()
            adapter.addAll(dadosFormatados)
            adapter.notifyDataSetChanged()
        }
    }

    private fun salvarOuAtualizar() {
        val nome = etNome.text.toString().trim()
        val telefone = etTelefone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val senha = etSenha.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nome e E-mail são obrigatórios!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                if (clienteEmEdicao == null) {
                    val novoCliente = Cliente(
                        nome = nome,
                        telefone = telefone,
                        email = email,
                        senha = senha
                    )
                    db.clienteDao().inserir(novoCliente)
                    Toast.makeText(this@MainActivity, "Cliente inserido!", Toast.LENGTH_SHORT).show()
                } else {
                    // Operação UPDATE
                    val clienteAtualizado = clienteEmEdicao!!.copy(
                        nome = nome,
                        telefone = telefone,
                        email = email,
                        senha = senha
                    )
                    db.clienteDao().atualizar(clienteAtualizado)
                    Toast.makeText(this@MainActivity, "Cliente atualizado!", Toast.LENGTH_SHORT).show()
                }

                limparFormulario()
                carregarLista()
            } catch (e: Exception) {
                // Muito útil em aula para demonstrar a violação de chave única (email duplicado)
                Toast.makeText(this@MainActivity, "Erro SQLite: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmarExclusao(cliente: Cliente) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Cliente")
            .setMessage("Deseja realmente excluir o cliente ${cliente.nome}?")
            .setPositiveButton("Sim") { _, _ ->
                lifecycleScope.launch {
                    db.clienteDao().deletar(cliente)
                    Toast.makeText(this@MainActivity, "Cliente removido!", Toast.LENGTH_SHORT).show()
                    limparFormulario()
                    carregarLista()
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun preencherCamposParaEdicao(cliente: Cliente) {
        clienteEmEdicao = cliente
        etNome.setText(cliente.nome)
        etTelefone.setText(cliente.telefone)
        etEmail.setText(cliente.email)
        etSenha.setText(cliente.senha)
        btnSalvar.text = "Atualizar"
    }

    private fun limparFormulario() {
        clienteEmEdicao = null
        etNome.text.clear()
        etTelefone.text.clear()
        etEmail.text.clear()
        etSenha.text.clear()
        btnSalvar.text = "Salvar"
    }
}
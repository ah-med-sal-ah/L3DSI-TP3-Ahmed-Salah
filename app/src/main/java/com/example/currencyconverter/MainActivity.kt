package com.example.currencyconverter
import android.widget.Toast
import java.util.Locale
import android.graphics.Paint
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.currencyconverter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // View Binding : donne accès à toutes les vues de activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Taux de change fixes, exprimés pour 1 EUR (utilisés à l'étape 6)
    private val taux = mapOf(
        "EUR" to 1.0, "USD" to 1.17, "TND" to 3.40,
        "GBP" to 0.87, "JPY" to 172.50, "CAD" to 1.62
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // On gonfle le layout via le binding au lieu de setContentView(R.layout...)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurerSpinners()
        souligner()
        configurerClavier()
        configurerBoutonConvertir()
        configurerBoutonInverser()
    }

    /** Remplit les deux Spinner avec la liste R.array.devises. */
    private fun configurerSpinners() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.devises,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spDeviseSource.adapter = adapter
        binding.spDeviseCible.adapter = adapter

        // Sélections par défaut : source = USD (index 0), cible = EUR (index 1)
        binding.spDeviseSource.setSelection(0)
        binding.spDeviseCible.setSelection(1)
    }

    /** Souligne le montant et le résultat (impossible en XML). */
    private fun souligner() {
        binding.tvMontant.paintFlags =
            binding.tvMontant.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvResultat.paintFlags =
            binding.tvResultat.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }
    /** Associe chaque touche du clavier à son action. */
    private fun configurerClavier() {
        // Touches 0 à 9 et "." : chacune envoie son propre texte à ajouterCaractere
        val touches = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnPoint
        )
        touches.forEach { bouton ->
            bouton.setOnClickListener { ajouterCaractere(bouton.text.toString()) }
        }

        // Touche C : remet le montant à 0 et vide le résultat
        binding.btnC.setOnClickListener { effacer() }
    }

    /** Ajoute un caractère (chiffre ou point) à la fin du montant, en respectant les règles. */
    private fun ajouterCaractere(c: String) {
        val montantActuel = binding.tvMontant.text.toString()

        // Règle : un seul point décimal, un 2e appui sur "." est ignoré
        if (c == "." && montantActuel.contains(".")) return

        // Règle : 10 caractères maximum
        if (montantActuel.length >= 10) return

        // Le montant initial "0" est remplacé par le premier chiffre tapé
        // (sauf pour ".", qui donne "0.")
        val nouveauMontant = if (montantActuel == "0" && c != ".") c else montantActuel + c

        binding.tvMontant.text = nouveauMontant
    }

    /** Remet le montant à "0" et vide le résultat. */
    private fun effacer() {
        binding.tvMontant.text = getString(R.string.montant_initial)
        binding.tvResultat.text = ""
    }
    /** Associe le clic du bouton Convertir à la conversion. */
    private fun configurerBoutonConvertir() {
        binding.btnConvertir.setOnClickListener { lancerConversion() }
    }

    /** Lit le montant et les devises, vérifie la saisie, puis affiche le résultat. */
    private fun lancerConversion() {
        // toDoubleOrNull accepte aussi "5." ; il renvoie null si le texte est invalide
        val montant = binding.tvMontant.text.toString().toDoubleOrNull() ?: 0.0

        // Règle : montant à 0 -> Toast d'erreur, pas de conversion
        if (montant == 0.0) {
            Toast.makeText(this, getString(R.string.erreur_montant_vide), Toast.LENGTH_SHORT).show()
            return
        }

        val source = binding.spDeviseSource.selectedItem.toString()
        val cible = binding.spDeviseCible.selectedItem.toString()

        val resultat = convertir(montant, source, cible)

        // Locale.US : force le point décimal (sinon "102,56" sur un téléphone en français)
        binding.tvResultat.text = String.format(Locale.US, "%.2f", resultat)
    }

    /**
     * Convertit un montant d'une devise à une autre en passant par l'euro (devise pivot).
     * montant en EUR = montant / taux(source)
     * résultat       = montant en EUR * taux(cible)
     */
    private fun convertir(montant: Double, source: String, cible: String): Double {
        val tauxSource = taux[source] ?: 1.0
        val tauxCible = taux[cible] ?: 1.0

        val montantEnEuro = montant / tauxSource
        return montantEnEuro * tauxCible
    }
    /** Associe le clic du bouton ⇅ à l'inversion des devises. */
    private fun configurerBoutonInverser() {
        binding.btnInverser.setOnClickListener { inverserDevises() }
    }

    /** Échange les devises source et cible, puis relance la conversion si un résultat est affiché. */
    private fun inverserDevises() {
        // On mémorise les deux positions AVANT de les modifier
        val positionSource = binding.spDeviseSource.selectedItemPosition
        val positionCible = binding.spDeviseCible.selectedItemPosition

        binding.spDeviseSource.setSelection(positionCible)
        binding.spDeviseCible.setSelection(positionSource)

        // Si un résultat est déjà affiché, on le recalcule avec les devises inversées
        if (binding.tvResultat.text.isNotEmpty()) {
            lancerConversion()
        }
    }
}
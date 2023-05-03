package cr.ac.una.gps

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.lifecycle.lifecycleScope
import cr.ac.una.gps.adapter.PoligonoAdapter
import cr.ac.una.gps.dao.PoligonoDao
import cr.ac.una.gps.db.AppDatabase
import cr.ac.una.gps.entity.Poligono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ConfigAreaFragment : Fragment() {
    lateinit var button: Button
    lateinit var latitudText: EditText
    lateinit var longitudText: EditText
    lateinit var poligonoDao: PoligonoDao
    lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_config_area, container, false)

        poligonoDao = AppDatabase.getInstance(requireContext()).poligonoDao()

        button = view.findViewById(R.id.button)
        latitudText = view.findViewById(R.id.editTextNumberDecimal)
        longitudText = view.findViewById(R.id.editTextNumberDecimal2)
        listView = view.findViewById(R.id.listPoligonos)

        button.isEnabled = false

        // Add a text watcher to the EditText to enable/disable the button
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isFilled =
                    !latitudText.text.isNullOrBlank() && !longitudText.text.isNullOrBlank()
                button.isEnabled = isFilled
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        latitudText.addTextChangedListener(textWatcher)
        longitudText.addTextChangedListener(textWatcher)

        loadListItems()

        button.setOnClickListener {
            val latitud = latitudText.text.toString().toDouble()
            val longitud = longitudText.text.toString().toDouble()

            val poligono = Poligono(
                id = null,
                latitud = latitud,
                longitud = longitud
            )

            insertPoligono(poligono)

            latitudText.setText("")
            longitudText.setText("")

            loadListItems()

            val imm =
                activity?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        return view
    }

    fun loadListItems() {
        lifecycleScope.launch {
            val poligonos = withContext(Dispatchers.IO) {

                poligonoDao.getAll()


            }

            val adapter = PoligonoAdapter(requireContext(), poligonos, this@ConfigAreaFragment)
            listView.adapter = adapter

        }
    }

    private fun insertPoligono(poligono: Poligono) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                poligonoDao.insert(poligono)
            }
        }
    }

    companion object {

    }
}
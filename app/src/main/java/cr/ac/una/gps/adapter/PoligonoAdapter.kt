package cr.ac.una.gps.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.ImageButton
import cr.ac.una.gps.entity.Poligono
import cr.ac.una.gps.R
import cr.ac.una.gps.db.AppDatabase
import androidx.lifecycle.lifecycleScope
import cr.ac.una.gps.ConfigAreaFragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PoligonoAdapter(
    context: Context,
    poligonos: List<Poligono>,
    private val fragment: ConfigAreaFragment
) :
    ArrayAdapter<Poligono>(context, 0, poligonos) {

    val poligonoDao = AppDatabase.getInstance(context).poligonoDao()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        }

        val poligono = getItem(position)

        val idTextView = view!!.findViewById<TextView>(R.id.id)
        val latitudTextView = view.findViewById<TextView>(R.id.latitud)
        val longitudTextView = view.findViewById<TextView>(R.id.longitud)
        val deleteButton = view.findViewById<ImageButton>(R.id.imageButton)


        deleteButton.setOnClickListener {
            Log.d("PoligonoAdapter", "Poligono eliminado: ${poligono?.id}")
            deleteById(poligono?.id)
            fragment.loadListItems()
            notifyDataSetChanged()
        }

        idTextView.text = poligono!!.id.toString()
        latitudTextView.text = poligono.latitud.toString()
        longitudTextView.text = poligono.longitud.toString()

        return view
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteById(id: Long?) {
        if (id != null) {
            GlobalScope.launch(Dispatchers.IO) {
                poligonoDao.deleteById(id)
            }
        }
    }
}
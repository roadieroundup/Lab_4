package cr.ac.una.gps


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil
import cr.ac.una.gps.dao.PoligonoDao
import cr.ac.una.gps.dao.UbicacionDao
import cr.ac.una.gps.db.AppDatabase
import cr.ac.una.gps.entity.Ubicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsFragment : Fragment() {
    private lateinit var map: GoogleMap

    private lateinit var locationReceiver: BroadcastReceiver
    private lateinit var ubicacionDao: UbicacionDao

    //lunes 24
//    private lateinit var polygon: Polygon
    private var polygon: Polygon? = null

    //
    private lateinit var poligonoDao: PoligonoDao


    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        //init polygon
        createPolygon {
            polygon = it
        }
        loadMarkers(googleMap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ubicacionDao = AppDatabase.getInstance(requireContext()).ubicacionDao()
        poligonoDao = AppDatabase.getInstance(requireContext()).poligonoDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_maps, container, false)

//        var ubicaciones: List<Ubicacion> = listOf()
        val floating = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)

        floating.setOnClickListener {

            showDatePickerDialog()
        }

        return view


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        iniciaServicio()
//        ubicacionDao = AppDatabase.getInstance(this).ubicacionDao()
        //poner en el mapa


        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val latitud = intent?.getDoubleExtra("latitud", 0.0) ?: 0.0
                val longitud = intent?.getDoubleExtra("longitud", 0.0) ?: 0.0
                println(latitud.toString() + "    " + longitud)
                Log.d("insert", latitud.toString() + "    " + longitud)

                val isInsidePolygon = isLocationInsidePolygon(LatLng(latitud, longitud))

                val entity = Ubicacion(
                    id = null,
                    latitud = latitud,
                    longitud = longitud,
                    fecha = Date(),
                    area_restringida = isInsidePolygon
                )

                insertEntity(entity)

                if (isInsidePolygon) {
                    map.addMarker(
                        MarkerOptions().position(LatLng(latitud, longitud)).title("💀")
                            .icon(BitmapDescriptorFactory.fromAsset("marker.png"))
                    )
                    makeCall()

                } else {
                    map.addMarker(
                        MarkerOptions().position(LatLng(latitud, longitud)).title("Marker")
                    )
                }

                map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitud, longitud)))

            }
        }
        context?.registerReceiver(locationReceiver, IntentFilter("ubicacionActualizada"))

    }

    private fun requestPermission() {
        val permission = Manifest.permission.CALL_PHONE
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 2)
        } else {
            makeCall()
        }
    }

    private fun makeCall() {
        val phoneNumber = ConfigTelFragment.phoneNum

        if (phoneNumber != "") {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(intent)
            } else {
                requestPermission()
            }
        } else {
            Toast.makeText(requireContext(), "AGREGUE UN NUMERO DE EMERGENCIA", Toast.LENGTH_LONG)
                .show()
        }

    }


    private fun clearAllButPolygon() {
        map.clear()
        createPolygon {
            polygon = it
        }
    }

    private fun loadMarkersUsingDate(datePiked: Long) {

        lifecycleScope.launch {
            val ubicaciones = withContext(Dispatchers.IO) {
                ubicacionDao.getUbicacionesByFecha(datePiked)
            }
            ubicaciones.forEach {
                map.addMarker(
                    if (it.area_restringida) {
                        MarkerOptions().position(LatLng(it.latitud, it.longitud)).title("💀")
                            .icon(BitmapDescriptorFactory.fromAsset("marker.png"))
                    } else {
                        MarkerOptions().position(LatLng(it.latitud, it.longitud)).title("Marker")

                    }
                )
            }
        }
    }

    private fun showDatePickerDialog() {
        Log.d("datePicked", "enter showDatePickerDialog")
        val datePicker = DatePickerFragment(
            listener = { day, month, year -> onDateSelected(day, month, year) },
            cancelListener = { onDateCanceled() }
        )

        datePicker.show(requireActivity().supportFragmentManager, "datePicker")
    }

    private fun onDateSelected(day: Int, month: Int, year: Int) {
        // get timestamp from date
        clearAllButPolygon()
        val calendar = Calendar.getInstance()
        Log.d("date", "$day $month $year")
        calendar.set(year, month, day, 0, 0, 0) // Set time to midnight
        calendar.set(Calendar.MILLISECOND, 0) // Set milliseconds to 0
        datePicked = calendar.timeInMillis
        Log.d("datePicked", "user clicked ok dp: $datePicked")
        loadMarkersUsingDate(datePicked)
    }

    private fun onDateCanceled() {
        Log.d("datePicked", "user clicked cancel")
        loadMarkers(map)
//        datePiked = 0
    }


    override fun onResume() {
        super.onResume()
        createPolygon {
            polygon = it
        }
        // Registrar el receptor para recibir actualizaciones de ubicación
        context?.registerReceiver(locationReceiver, IntentFilter("ubicacionActualizada"))
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el receptor al pausar el fragmento
        context?.unregisterReceiver(locationReceiver)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        iniciaServicio()
                    }
                } else {
                    // Location permission denied
                }
            }

            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Phone call permission granted
                    // Call the phone number
                    makeCall()
                } else {
                    // Phone call permission denied
                }
            }
        }
    }

    private fun iniciaServicio() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            val intent = Intent(context, LocationService::class.java)
            context?.startService(intent)
        }
    }

    private fun insertEntity(entity: Ubicacion) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                ubicacionDao.insert(entity)
            }
        }
    }

    private fun loadMarkers(googleMap: GoogleMap) {

        lifecycleScope.launch {
            val ubicaciones = withContext(Dispatchers.IO) {
                ubicacionDao.getAll()
            }
            ubicaciones.forEach {
                googleMap.addMarker(
                    if (it.area_restringida) {
                        MarkerOptions().position(LatLng(it.latitud, it.longitud)).title("Marker")
                            .icon(BitmapDescriptorFactory.fromAsset("marker.png"))
                    } else {
                        MarkerOptions().position(LatLng(it.latitud, it.longitud)).title("Marker")
                    }
                )
                Log.d("getAll", it.latitud.toString() + "    " + it.longitud)
            }
        }
    }


    private fun createPolygon(callback: (Polygon) -> Unit) {

        val polygonOptions = PolygonOptions()

        lifecycleScope.launch {
            val puntosPoligono = withContext(Dispatchers.IO) {
                poligonoDao.getAll()
            }
            puntosPoligono.forEach {
                polygonOptions.add(LatLng(it.latitud, it.longitud))
                Log.d("getAllPoly", it.latitud.toString() + "    " + it.longitud)
            }

            polygonOptions.strokeColor(Color.RED)

            if (polygonOptions.points.size != 0) {
                val polygon = map.addPolygon(polygonOptions)
                callback(polygon)
            } else {
                Toast.makeText(context, "No existe un area restringida", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun isLocationInsidePolygon(location: LatLng): Boolean {
        return if (polygon != null) {
            PolyUtil.containsLocation(location, polygon!!.points, true)
        } else {
            false
        }
    }

    companion object {
        var datePicked: Long = 0
    }
}
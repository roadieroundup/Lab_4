package cr.ac.una.gps

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText


class ConfigTelFragment : Fragment() {

    lateinit var editText: EditText
    lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_config_tel, container, false)
        editText = view.findViewById(R.id.editTextPhone)
        button = view.findViewById(R.id.buttonSave)

        button.isEnabled = false

        // Add a text watcher to the EditText to enable/disable the button
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isFilled =
                    !editText.text.isNullOrBlank()
                button.isEnabled = isFilled
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        editText.addTextChangedListener(textWatcher)

        button.setOnClickListener {
            phoneNum = editText.text.toString()
            Log.d("ConfigTelFragment", "Phone number: $phoneNum")
            //CLOSE KEYBOARD
            val imm =
                activity?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

        }


        return view
    }

    companion object {
        var phoneNum: String = ""
    }
}
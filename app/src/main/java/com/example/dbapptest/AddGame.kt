package com.example.dbapptest

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_add_game.*
import kotlinx.android.synthetic.main.activity_game_view.*
import kotlinx.android.synthetic.main.tile.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.Intent
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class AddGame : AppCompatActivity() {

    var originalName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_game)
        setSupportActionBar(AddGame_Toolbar)
        val actionBar = supportActionBar
        actionBar?.title = "Add/Edit Game"
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val cal = Calendar.getInstance()
        val editText: EditText = findViewById(R.id.DateInput)
        val date = (object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateLabel(editText,cal)
            }
        })

        editText.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                DatePickerDialog(this@AddGame, date, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
        })

        if(intent.getStringExtra("CALLTYPE")!! == "Edit") {
            NameInput.setText(intent.getStringExtra("NAME"))
            originalName = NameInput.text.toString()
            PlatformInput.setText(intent.getStringExtra("PLATFORM"))
            DateInput.setText(intent.getStringExtra("DATE"))
            SeriesInput.setText(intent.getStringExtra("SERIES"))
            GenreInput.setText(intent.getStringExtra("GENRE"))
            CoverInput.setText(intent.getStringExtra("COVER"))
            DevInput.setText(intent.getStringExtra("DEV"))
            PublisherInput.setText(intent.getStringExtra("PUB"))
        }
    }

    private fun updateLabel(text: EditText,cal: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        text.setText(sdf.format(cal.time))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.sendButton -> {
                val client = OkHttpClient()
                val credential = intent.getStringExtra("Credentials")!!
                doAsync {
                    if(intent.getStringExtra("CALLTYPE")!! == "Edit") {
                        val removeBody = FormBody.Builder()
                            .add("game",originalName!!)
                            .build()

                        val removeRequest = Request.Builder()
                            .url(resources.getString(R.string.Server_Address) + "/RemoveGame")
                            .post(removeBody)
                            .header("Authorization",credential)
                            .build()

                        client.newCall(removeRequest).execute()
                    }

                    val formBody = FormBody.Builder()
                        .add("game",NameInput.text.toString())
                        .add("platform",PlatformInput.text.toString())
                        .add("release_date",DateInput.text.toString())
                        .add("series",SeriesInput.text.toString())
                        .add("genre",GenreInput.text.toString())
                        .add("cover_art",CoverInput.text.toString())
                        .add("developer",DevInput.text.toString())
                        .add("publisher",PublisherInput.text.toString())
                        .build()

                    val request = Request.Builder()
                        .url(resources.getString(R.string.Server_Address) + "/AddGame")
                        .post(formBody)
                        .header("Authorization",credential)
                        .build()

                    client.newCall(request).execute()

                    uiThread{
                        val returnIntent = Intent()
                        setResult(Activity.RESULT_OK, returnIntent)
                        this@AddGame.finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

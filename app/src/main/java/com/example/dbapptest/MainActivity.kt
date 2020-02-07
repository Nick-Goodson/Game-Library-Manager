package com.example.dbapptest

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.drm.DrmStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.android.synthetic.main.nav_header.view.*
import kotlinx.android.synthetic.main.tile.*
import kotlinx.android.synthetic.main.tile.view.*
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.JavaNetCookieJar
import org.jetbrains.anko.*
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL
import java.sql.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var client = OkHttpClient()
    private val REFRESHCODE = 1
    private var user = ""
    private var pass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        val drawerToggle:ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        ){}

        drawerToggle.isDrawerIndicatorEnabled = true
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        //Get user and pass from intent
        onLogin()

        //Nav Drawer options selected
        navigationView.setNavigationItemSelectedListener {
            val item = toolbar.menu.findItem(R.id.spinner)
            val spinner = item.actionView
            val spin:Spinner = spinner.findViewById(R.id.spinner)
            val pos = resources.getStringArray(R.array.options_array)
            spin.setSelection(pos.indexOf(it.title))
            drawer_layout.closeDrawers()
            true
        }

        fab.setOnClickListener {
            val intent = Intent(this, AddGame::class.java)
            intent.putExtra("Credentials",Credentials.basic(user,pass))
            intent.putExtra("CALLTYPE","Add")
            startActivityForResult(intent,REFRESHCODE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_layout, menu)
        val item = menu.findItem(R.id.spinner)
        val spinner = item.actionView
        val spin:Spinner = spinner.findViewById(R.id.spinner)
        val adapter = ArrayAdapter.createFromResource(this,R.array.options_array,android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        Username_header?.text = user
        spin.adapter = adapter
        //Spinner options selected
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                navigationView.menu.getItem(position).isChecked = true
                populateView(parent?.getItemAtPosition(position).toString())
            }
        }
        return true
    }

    //Toolbar options
    override fun onOptionsItemSelected(item:MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                populateView(navigationView.checkedItem?.title.toString())
            }
            R.id.menu_sort_date -> {
                populateView(navigationView.checkedItem?.title.toString(),"Date")
            }
            R.id.menu_sort_name -> {
                populateView(navigationView.checkedItem?.title.toString(),"Name")
            }
            R.id.menu_sort_rating -> {
                populateView(navigationView.checkedItem?.title.toString(),"Rating")
            }
            R.id.search -> {
                val alert = AlertDialog.Builder(this@MainActivity).create()
                alert.setTitle("Search")

                val searchEdit = EditText(this)
                searchEdit.hint = "Game Title"
                alert.setView(searchEdit)

                alert.setButton(AlertDialog.BUTTON_POSITIVE,"OK", object : DialogInterface.OnClickListener {
                    override fun onClick(
                        dialog: DialogInterface?,
                        which: Int
                    ) {
                        populateView(navigationView.checkedItem?.title.toString(),"Name",searchEdit.text.toString())
                    }
                })
                alert.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel", object : DialogInterface.OnClickListener {
                    override fun onClick(
                        dialog: DialogInterface?,
                        which: Int
                    ) {
                        dialog?.cancel()
                    }
                })

                alert.show()
            }
        }
        return true
    }

    private fun populateView(selection: String, sortType: String = "Name",searchString: String = "") {

        tiles.removeAllViews()

        val layoutInflater: LayoutInflater = LayoutInflater.from(this)

        val credential = Credentials.basic(user,pass)
        val formBody = when(selection) {
            "All Games" -> {
                FormBody.Builder()
                    .add("lowerLimit","0")
                    .add("upperLimit","100")
                    .add("sortType",sortType)
                    .add("searchString",searchString)
                    .build() }
            else -> {
                FormBody.Builder()
                    .add("lowerLimit","0")
                    .add("upperLimit","100")
                    .add("sortType",sortType)
                    .add("searchString",searchString)
                    .add("username", user)
                    .build()
            }
        }

        val request = when(selection) {
            "Library" -> {
                Request.Builder()
                    .url(resources.getString(R.string.Server_Address) + "/GetLibrary")
                    .post(formBody)
                    .header("Authorization", credential)
                    .build()
            }
            "Favorites" -> {
                Request.Builder()
                    .url(resources.getString(R.string.Server_Address) + "/GetFavorites")
                    .post(formBody)
                    .header("Authorization", credential)
                    .build()
            }
            "Wishlist" -> {
                Request.Builder()
                    .url(resources.getString(R.string.Server_Address) + "/GetWishlist")
                    .post(formBody)
                    .header("Authorization", credential)
                    .build()
            }
            else -> {
                Request.Builder()
                    .url(resources.getString(R.string.Server_Address) + "/GetAllGames")
                    .post(formBody)
                    .build()
            }
        }

        doAsync {

            val response = client.newCall(request).execute().body()?.string()

            uiThread {

                val map = ObjectMapper().registerModule(KotlinModule())
                val test = map.readValue(response, TestModel::class.java)

                if(test === null) {
                    return@uiThread
                }

                for(dataSet in test.Data) {

                    if(dataSet.isEmpty()) {
                        break
                    }
                    val view: View = layoutInflater.inflate(
                        R.layout.tile,
                        tiles,
                        false
                    )
                    val title = view.title_text
                    val devText = view.dev_text
                    val platText = view.plat_text
                    if(selection == "Library" || selection == "Favorites") {
                        platText.text = dataSet[11]
                        if(dataSet[9] != null) {
                            view.ratingBar.rating = dataSet[9].toFloat()
                        }
                    }
                    else {
                        platText.text = ""
                        view.ratingBar.setIsIndicator(true)
                        if(dataSet[7] != null) {
                            view.ratingBar.rating = dataSet[7].toFloat()
                        }
                        else {
                            view.ratingBar.isVisible = false
                        }
                    }
                    title.text = dataSet[0]
                    devText.text = dataSet[6]

                    //Game title click action
                    title.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(it: View) {
                            val intent = Intent(this@MainActivity, GameView::class.java)
                            intent.putExtra("GAME_NAME", title.text)
                            intent.putExtra("USERNAME", user)
                            intent.putExtra("PASSWORD", pass)
                            startActivityForResult(intent,REFRESHCODE)
                        }
                    })

                    //Ratings Bar click action
                    view.ratingBar.onRatingBarChangeListener = (object : RatingBar.OnRatingBarChangeListener {
                        override fun onRatingChanged(
                            ratingBar: RatingBar?,
                            rating: Float,
                            fromUser: Boolean
                        ) {
                            if(fromUser) {
                                val rateBody = FormBody.Builder()
                                    .add("rating",rating.toInt().toString())
                                    .add("username",user)
                                    .add("platform",platText.text.toString())
                                    .add("game",title.text.toString())
                                    .build()

                                val rateRequest = Request.Builder()
                                    .url(resources.getString(R.string.Server_Address) + "/RateGame")
                                    .post(rateBody)
                                    .header("Authorization",credential)
                                    .build()

                                doAsync {
                                    client.newCall(rateRequest).execute()
                                }

                            }
                        }
                    })

                    //More button click action
                    view.more_button.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View?) {
                            val popup = PopupMenu(this@MainActivity,v)
                            popup.menuInflater.inflate(R.menu.game_popup, popup.menu)
                            if(selection == "Library" || selection == "Favorites") {
                                popup.menu.getItem(0).title = "Remove From Library"
                                popup.menu.getItem(0).setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        doAsync {
                                            val removeForm = FormBody.Builder()
                                                .add("username",user)
                                                .add("game",view.title_text.text.toString())
                                                .add("platform",view.plat_text.text.toString())
                                                .build()

                                            val removeRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/RemoveFromLibrary")
                                                .post(removeForm)
                                                .header("Authorization",credential)
                                                .build()

                                            client.newCall(removeRequest).execute()
                                            uiThread {
                                                populateView(selection)
                                            }
                                        }
                                        return true
                                    }
                                })
                            }
                            else if((selection == "All Games")|| selection == "Wishlist") {
                                popup.menu.getItem(0).title = "Add To Library"
                                popup.menu.getItem(0).setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        val alert = AlertDialog.Builder(this@MainActivity).create()
                                        alert.setTitle("Platform")
                                        val spinnerArray = ArrayList<String>()

                                        doAsync{
                                            val platformForm = FormBody.Builder()
                                                .add("name",view.title_text.text.toString())
                                                .build()

                                            val platformRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/GetPlatform")
                                                .post(platformForm)
                                                .build()

                                            val result = client.newCall(platformRequest).execute().body()?.string()

                                            uiThread{

                                                val plats = map.readValue(result, TestModel::class.java)

                                                for(plat in plats.Data) {
                                                    spinnerArray.add(plat[1])
                                                }

                                                val spinner = Spinner(this@MainActivity)
                                                val spinnerArrayAdapter = ArrayAdapter<String>(this@MainActivity,android.R.layout.simple_spinner_dropdown_item, spinnerArray)
                                                spinner.setAdapter(spinnerArrayAdapter)
                                                spinner.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT))
                                                spinner.setSelection(0)
                                                alert.setView(spinner)

                                                alert.setButton(AlertDialog.BUTTON_POSITIVE,"OK", object : DialogInterface.OnClickListener {
                                                    override fun onClick(
                                                        dialog: DialogInterface?,
                                                        which: Int
                                                    ) {
                                                        doAsync {
                                                            val addForm = FormBody.Builder()
                                                                .add("username",user)
                                                                .add("game",view.title_text.text.toString())
                                                                .add("platform",spinner.selectedItem.toString())
                                                                .build()

                                                            val addRequest = Request.Builder()
                                                                .url(resources.getString(R.string.Server_Address) + "/AddToLibrary")
                                                                .post(addForm)
                                                                .header("Authorization",credential)
                                                                .build()

                                                            client.newCall(addRequest).execute()

                                                            if(selection == "Wishlist") {
                                                                val removeForm = FormBody.Builder()
                                                                    .add("username",user)
                                                                    .add("game",view.title_text.text.toString())
                                                                    .build()

                                                                val removeRequest = Request.Builder()
                                                                    .url(resources.getString(R.string.Server_Address) + "/RemoveFromWishlist")
                                                                    .post(removeForm)
                                                                    .header("Authorization",credential)
                                                                    .build()

                                                                client.newCall(removeRequest).execute()
                                                            }
                                                            uiThread {
                                                                populateView(selection)
                                                            }
                                                        }
                                                    }
                                                })
                                                alert.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel", object : DialogInterface.OnClickListener {
                                                    override fun onClick(
                                                        dialog: DialogInterface?,
                                                        which: Int
                                                    ) {
                                                        dialog?.cancel()
                                                    }
                                                })
                                                alert.show()
                                            }
                                        }
                                        return true
                                    }
                                })
                            }
                            if(selection == "All Games") {
                                popup.menu.getItem(1).title = "Add to Wishlist"
                                popup.menu.getItem(1).setOnMenuItemClickListener(object: MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        doAsync {
                                            val addForm = FormBody.Builder()
                                                .add("username",user)
                                                .add("game",view.title_text.text.toString())
                                                .build()

                                            val addRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/AddToWishlist")
                                                .post(addForm)
                                                .header("Authorization",credential)
                                                .build()

                                            client.newCall(addRequest).execute()
                                        }
                                        return true
                                    }
                                })
                            }
                            else if(selection == "Library") {
                                popup.menu.getItem(1).title = "Add to Favorites"
                                popup.menu.getItem(1).setOnMenuItemClickListener(object: MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        doAsync {
                                            val addForm = FormBody.Builder()
                                                .add("username",user)
                                                .add("game",view.title_text.text.toString())
                                                .add("platform",view.plat_text.text.toString())
                                                .build()

                                            val addRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/AddToFavorites")
                                                .post(addForm)
                                                .header("Authorization",credential)
                                                .build()

                                            client.newCall(addRequest).execute()
                                        }
                                        return true
                                    }
                                })
                            }
                            else if(selection == "Wishlist") {
                                popup.menu.getItem(1).title = "Remove from Wishlist"
                                popup.menu.getItem(1).setOnMenuItemClickListener(object: MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        doAsync {
                                            val removeForm = FormBody.Builder()
                                                .add("username",user)
                                                .add("game",view.title_text.text.toString())
                                                .build()

                                            val removeRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/RemoveFromWishlist")
                                                .post(removeForm)
                                                .header("Authorization",credential)
                                                .build()

                                            client.newCall(removeRequest).execute()
                                        }
                                        return true
                                    }
                                })
                            }
                            else if(selection == "Favorites") {
                                popup.menu.getItem(1).title = "Remove from Favorites"
                                popup.menu.getItem(1).setOnMenuItemClickListener(object: MenuItem.OnMenuItemClickListener {
                                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                                        doAsync {
                                            val removeForm = FormBody.Builder()
                                                .add("username",user)
                                                .add("game",view.title_text.text.toString())
                                                .add("platform",view.plat_text.text.toString())
                                                .build()

                                            val removeRequest = Request.Builder()
                                                .url(resources.getString(R.string.Server_Address) + "/RemoveFromFavorites")
                                                .post(removeForm)
                                                .header("Authorization",credential)
                                                .build()

                                            client.newCall(removeRequest).execute()
                                        }
                                        return true
                                    }
                                })
                            }
                            popup.show()

                        }
                    })

                    //Load image for tile
                    Picasso.get().load(dataSet[3]).into(view.cov_art)
                    //Add tile to linearLayout
                    tiles.addView(view)
                }
            }
        }
    }

    private fun onLogin() {
        user = intent.getStringExtra("USERNAME")!!
        pass = intent.getStringExtra("PASSWORD")!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REFRESHCODE -> {
                populateView(navigationView.checkedItem?.title.toString())
            }
        }
    }
}


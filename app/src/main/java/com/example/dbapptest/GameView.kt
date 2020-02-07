package com.example.dbapptest

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_game_view.*
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.squareup.picasso.Picasso
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.sql.*
import java.util.*


class GameView : AppCompatActivity() {
    var coverURL: String? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_view)
        setSupportActionBar(GameView_Toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val gameName = intent.getStringExtra("GAME_NAME")!!
        val user = intent.getStringExtra("USERNAME")!!
        val pass = intent.getStringExtra("PASSWORD")!!
        val credential = Credentials.basic(user,pass)


        doAsync {

            val formBody = FormBody.Builder()
                .add("name",gameName)
                .build()

            var request = Request.Builder()
                .url(resources.getString(R.string.Server_Address) + "/GetGame")
                .post(formBody)
                .build()

            val gameInfo = client.newCall(request).execute().body()?.string()

            request = Request.Builder()
                .url(resources.getString(R.string.Server_Address) + "/GetDeveloper")
                .post(formBody)
                .build()

            val devInfo = client.newCall(request).execute().body()?.string()

            request = Request.Builder()
                .url(resources.getString(R.string.Server_Address) + "/GetPublisher")
                .post(formBody)
                .build()

            val pubInfo = client.newCall(request).execute().body()?.string()

            request = Request.Builder()
                .url(resources.getString(R.string.Server_Address) + "/GetPlatform")
                .post(formBody)
                .build()

            val platInfo = client.newCall(request).execute().body()?.string()

            uiThread {

                val map = ObjectMapper().registerModule(KotlinModule())
                val gameObject = map.readValue(gameInfo, TestModel::class.java)
                val devObject = map.readValue(devInfo, TestModel::class.java)
                val pubObject = map.readValue(pubInfo, TestModel::class.java)
                val platObject = map.readValue(platInfo, TestModel::class.java)

                for(game in gameObject.Data){
                    coverURL = game[3]
                    Picasso.get().load(game[3]).into(it.cover_imageView)
                    it.game_title.text = game[0]
                    it.release_date_view.text = "Release Date: " + game[1]
                    it.game_series.text = "Series: " + game[2]
                    it.game_genre.text = "Genre: " + game[4]
                }

                var devText = "Developer(s): "
                var pubText = "Publisher(s): "
                var platText = "Platform(s): "

                for(dev in devObject.Data) {
                    devText += dev[1] + ","
                }
                devText = devText.dropLast(1)
                it.game_dev.text = devText

                for(pub in pubObject.Data) {
                    pubText += pub[1] + ","
                }
                pubText = pubText.dropLast(1)
                it.game_pub.text = pubText

                for(plat in platObject.Data) {
                    platText += plat[1] + ","
                }
                platText = platText.dropLast(1)
                it.game_platforms.text = platText

            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.GameEdit -> {
                val AddIntent = Intent(this, AddGame::class.java)
                AddIntent.putExtra("NAME", game_title.text.toString())
                AddIntent.putExtra("PLATFORM", game_platforms.text.toString().substring(13))
                AddIntent.putExtra("DATE",release_date_view.text.toString().substring(14))
                AddIntent.putExtra("SERIES",game_series.text.toString().substring(8))
                AddIntent.putExtra("GENRE",game_genre.text.toString().substring(7))
                AddIntent.putExtra("COVER",coverURL)
                AddIntent.putExtra("DEV",game_dev.text.toString().substring(14))
                AddIntent.putExtra("PUB",game_pub.text.toString().substring(14))
                AddIntent.putExtra("CALLTYPE","Edit")
                AddIntent.putExtra("Credentials",Credentials.basic(intent.getStringExtra("USERNAME")!!,intent.getStringExtra("PASSWORD")!!))
                startActivity(AddIntent)
                val returnIntent = Intent()
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
            R.id.GameRemove -> {
                doAsync {
                    val removeBody = FormBody.Builder()
                        .add("game",game_title.text.toString())
                        .build()

                    val removeRequest = Request.Builder()
                        .url(resources.getString(R.string.Server_Address) + "/RemoveGame")
                        .post(removeBody)
                        .header("Authorization",Credentials.basic(intent.getStringExtra("USERNAME")!!,intent.getStringExtra("PASSWORD")!!))
                        .build()

                    client.newCall(removeRequest).execute()
                    uiThread {
                        val returnIntent = Intent()
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.game_toolbar_layout, menu)
        return true
    }
}

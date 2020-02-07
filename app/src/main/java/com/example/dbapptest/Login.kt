package com.example.dbapptest

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class Login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val client = OkHttpClient()

        LoginButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                val credential = Credentials.basic(UsernameInput.text.toString(),PasswordInput.text.toString())
                doAsync {

                    val request = Request.Builder()
                        .url(resources.getString(R.string.Server_Address) + "/AuthenticateUser")
                        .header("Authorization", credential)
                        .build()

                    val response = client.newCall(request).execute()
                    uiThread {
                        if(response.code() == 200) {
                            val resultIntent = Intent(this@Login,MainActivity::class.java)
                            resultIntent.putExtra("USERNAME",UsernameInput.text.toString())
                            resultIntent.putExtra("PASSWORD",PasswordInput.text.toString())
                            startActivity(resultIntent)
                            finish()
                        }
                        else {
                            Toast.makeText(this@Login,"Login Failed!",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })

        SignupButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                doAsync {
                    val formBody = FormBody.Builder()
                        .add("username",UsernameInput.text.toString())
                        .add("password",PasswordInput.text.toString())
                        .build()

                    val request = Request.Builder()
                        .url(resources.getString(R.string.Server_Address) + "/CreateUser")
                        .post(formBody)
                        .build()

                    val response = client.newCall(request).execute()
                    uiThread{
                        if (response.code() == 200) {
                            Toast.makeText(this@Login,"Successfully Created Account!",Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(this@Login,"Account Creation Failed!",Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
        })
    }
}

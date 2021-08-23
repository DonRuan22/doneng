package com.path.doneng

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.path.doneng.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var sendBtn: FloatingActionButton
    private val USER = 0
    private val BOT = 1
    private var macInfo = ""
    private var adsButton = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        MobileAds.initialize(this, "ca-app-pub-5890212856743526~3876779336")

        val mAdView = findViewById<AdView> (R.id.adView)
        val adRequest = AdRequest.Builder (). build ()
        mAdView.loadAd (adRequest)

        val mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-5890212856743526/3085322284"

        mInterstitialAd.loadAd(AdRequest.Builder().build())

        if (getSupportActionBar() != null)
            getSupportActionBar()?.hide()

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        macInfo = wifiManager.connectionInfo.macAddress
        val chatScrollView = findViewById<NestedScrollView>(R.id.chatScrollView)

        editText = findViewById(R.id.edittext_chatbox)
        chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        sendBtn = findViewById(R.id.send_button)
        sendBtn.setOnClickListener{
            if (adsButton % 10 == 0) {
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }
            }
            mInterstitialAd.loadAd(AdRequest.Builder().build())
            Log.d("TAG", "ads"+ adsButton % 10)
            adsButton += 1
            sendMessage()
        }


    }

    fun sendMessage(){
        val msg:String = editText.text.toString().trim()
        val date = Date(System.currentTimeMillis())
        val okHttpClient = OkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://doneng-edml6m2f3a-uc.a.run.app/webhooks/rest/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val userMessage = UserMessage()
        if(msg.trim().isEmpty())
            Toast.makeText(this,"Please enter your query",Toast.LENGTH_SHORT).show()
        else {
            Log.e("MSg","message: $msg")
            editText.setText("")
            if(macInfo.equals("02:00:00:00:00:00"))
                macInfo = getMacAddr()
            userMessage.UserMessage(macInfo,msg)
            showTextView(msg,USER,date.toString())

        }
        val messageSender = retrofit.create(MessageSender::class.java)
        val response =
            messageSender.sendMessage(userMessage)

        response.enqueue(object : Callback<List<BotResponse>> {
            override fun onResponse(call: Call<List<BotResponse>>, response: Response<List<BotResponse>>) {
                if (response.body() == null || response.body()!!.size == 0) {
                    val botMessage = "Sorry didn't understand"
                    showTextView(botMessage,BOT,date.toString())
                } else {
                    val botResponse = response.body()!![0]
                    showTextView(botResponse.text,BOT,date.toString())
                }
            }

            override fun onFailure(call: Call<List<BotResponse>>, t: Throwable) {
                val botMessage = "Check your network connection"
                showTextView(botMessage,BOT,date.toString())
                t.printStackTrace()
                Toast.makeText(this@MainActivity, "" + t.message, Toast.LENGTH_SHORT).show()
            }
        })

    }
    fun showTextView(message:String,type:Int,date:String){
        var frameLayout: FrameLayout? = null
        val linearLayout = findViewById<LinearLayout>(R.id.chat_layout)
        when(type){
            USER -> { frameLayout = getUserLayout()
            }
            BOT ->{frameLayout = getBotLayout()
            }
            else->{
                frameLayout = getBotLayout()
            }
        }
        frameLayout?.isFocusableInTouchMode = true
        linearLayout.addView(frameLayout)
        val messageTextView = frameLayout?.findViewById<TextView>(R.id.chat_msg)
        messageTextView?.setText(message)
        frameLayout?.requestFocus()
        editText.requestFocus()
        val currentDateTime = Date(System.currentTimeMillis())
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val providedDate = currentDate
        var time = ""
        if(currentDate.equals(providedDate)) {
            val timeFormat = SimpleDateFormat(
                "hh:mm aa",
                Locale.ENGLISH
            )
            time = timeFormat.format(currentDateTime)
        }else{
            val dateTimeFormat = SimpleDateFormat(
                "dd-MM-yy hh:mm aa",
                Locale.ENGLISH
            )
            time = dateTimeFormat.format(currentDate)
        }
        val timeTextView = frameLayout?.findViewById<TextView>(R.id.message_time)
        timeTextView?.setText(time.toString())


    }
    fun getUserLayout(): FrameLayout? {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        return inflater.inflate(R.layout.user_message_box, null) as FrameLayout?
    }

    fun getBotLayout(): FrameLayout? {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        return inflater.inflate(R.layout.bot_message_box, null) as FrameLayout?
    }

    //getting mac address from mobile
    private fun getMacAddr(): String {
        try {
            val all: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.getName().equals("wlan0", ignoreCase = true)) continue
                val macBytes: ByteArray = nif.getHardwareAddress() ?: return ""
                val res1 = StringBuilder()
                for (b in macBytes) {
                    // res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:", b))
                }
                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
            //handle exception
        }
        return ""
    }
}
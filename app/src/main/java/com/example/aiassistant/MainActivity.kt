package com.example.aipersonalassistant

import android.app.Activity
import android.app.VoiceInteractor
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.telecom.Call
import android.view.View
import android.view.WindowInsetsAnimation
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import com.example.aiassistant.R
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity(dispatchMode: Int) : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvResponse: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSpeak: Button
    private lateinit var textToSpeech: TextToSpeech

    private val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY" // 🔴 Replace with your actual key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResponse = findViewById(R.id.tvResponse)
        progressBar = findViewById(R.id.progressBar)
        btnSpeak = findViewById(R.id.btnSpeak)

        textToSpeech = TextToSpeech(this, this) // 🔊 Initialize Text-to-Speech

        btnSpeak.setOnClickListener {
            startVoiceRecognition()
        }
    }

    // 🎤 Step 1: Start Voice Recognition
    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        try {
            startActivityForResult(intent, 100)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not supported!", Toast.LENGTH_SHORT).show()
        }
    }

    // 🎤 Step 2: Get the Speech Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userText = result?.get(0) ?: "Hello!"
            tvResponse.text = "You: $userText"
            askAI(userText) // 🔥 Send the question to AI
        }
    }

    // 🤖 Step 3: Send Text to OpenAI API
    @OptIn(ExperimentalFoundationApi::class)
    private fun askAI(question: String) {
        progressBar.visibility = View.VISIBLE

        val client = OkHttpClient()
        val json = JSONObject()
        json.put("model", "gpt-3.5-turbo")
        json.put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", question)))

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"), json.toString()
        )

        val request = VoiceInteractor.Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(@RequiresApi(Build.VERSION_CODES.R)
        object : WindowInsetsAnimation.Callback(dispatchMode) {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed to connect to AI!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body()?.string()
                val aiResponse = JSONObject(responseData ?: "{}")
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optString("message", "Sorry, I couldn't understand.")

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvResponse.text = "AI: $aiResponse"
                    if (aiResponse != null) {
                        speak(aiResponse)
                    } // 🔊 Speak out the AI response
                }
            }
        })
    }

    // 🔊 Step 4: Speak the AI's Answer
    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        }
    }

    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
    }
}

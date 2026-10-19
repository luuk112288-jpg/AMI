package com.ami.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ami.music.ui.AmiApp
import com.ami.music.ui.AmiViewModel
import com.ami.music.ui.theme.AmiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AmiTheme { val vm: AmiViewModel = viewModel(); AmiApp(vm) } }
    }
}

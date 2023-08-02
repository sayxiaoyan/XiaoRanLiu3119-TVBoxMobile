package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.fragment.MyFragment
import com.github.tvbox.osc.ui.fragment.UserFragment

class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        val fragment = MyFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment)
            .commit()
    }
}
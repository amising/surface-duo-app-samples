/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.device.display.samples.twonote.model.DataProvider
import com.microsoft.device.dualscreen.layout.ScreenHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_toolbar)



        if (!ScreenHelper.isDualMode(this)) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.single_screen_container_id,
                    ItemsListFragment(),
                    null
                )
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.dual_screen_start_container_id,
                    ItemsListFragment(),
                    null
                ).commit()
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.dual_screen_end_container_id,
                    NoteFragment(),
                    null
                ).commit()
        }
    }
}
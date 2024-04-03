package com.example.demotaskview

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorizontalPager(state = rememberPagerState {
                2
            }) {
                when (it) {
                    0 -> {
                        Row(
                            modifier = Modifier
                                .background(color = Color.Red)
                                .fillMaxSize()
                                .padding(22.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        Toast
                                            .makeText(
                                                this@MainActivity,
                                                "ClickBar",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .fillMaxHeight()
                                    .background(color = Color.Green)
                                    .padding(4.dp)
                            )

                            NavigationWidgets(
                                modifier = Modifier
                                    .weight(2f)
                                    .clip(RoundedCornerShape(50.dp))
                                    .fillMaxHeight()
                                    .background(color = Color.Blue)
                            )



                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .fillMaxHeight()
                                    .background(color = Color.Green)
                                    .padding(4.dp),
                            )
                        }
                    }

                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .background(color = Color.Yellow)
                        )
                    }
                }
            }

        }
    }

}
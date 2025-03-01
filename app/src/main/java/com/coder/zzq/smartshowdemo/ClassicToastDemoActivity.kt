package com.coder.zzq.smartshowdemo

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coder.vincent.series.common_lib.resourceToColor
import com.coder.vincent.smart_toast.SmartToast
import com.coder.vincent.smart_toast.TOAST_ICON_POSITION_LEFT
import com.coder.vincent.smart_toast.TOAST_ICON_POSITION_RIGHT
import com.coder.zzq.smartshowdemo.databinding.ActivityClassicToastDemoBinding


class ClassicToastDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ActivityClassicToastDemoBinding.inflate(layoutInflater).apply {
                showToast.setOnClickListener {
                    val bgColor = when (bgColorGroup.checkedRadioButtonId) {
                        R.id.bg_color_one -> R.color.colorPrimary.resourceToColor()
                        R.id.bg_color_two -> R.color.colorAccent.resourceToColor()
                        else -> Color.parseColor("#cc000000")
                    }
                    val icon = if (iconGroup.checkedRadioButtonId == R.id.with_icon)
                        R.drawable.ic_smart_toast_emotion_success
                    else
                        0
                    val iconPosition =
                        if (iconPositionGroup.checkedRadioButtonId == R.id.icon_position_left) TOAST_ICON_POSITION_LEFT else TOAST_ICON_POSITION_RIGHT
                    SmartToast.classic()
                        .config()
                        .backgroundColor(bgColor)
                        .iconResource(icon)
                        .iconSizeDp(20f)
                        .iconPosition(iconPosition)
                        .apply()
                        .apply {
                            when (positionGroup.checkedRadioButtonId) {
                                R.id.position_top -> showAtTop("classic toast")
                                R.id.position_center -> showInCenter("classic toast")
                                else -> show("classic toast")
                            }
                        }
                }
            }.root
        )
    }
}
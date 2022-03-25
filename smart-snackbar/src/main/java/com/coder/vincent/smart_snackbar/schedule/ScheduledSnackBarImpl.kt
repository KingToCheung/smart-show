package com.coder.vincent.smart_snackbar.schedule

import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.get
import com.coder.vincent.series.common_lib.bean.TextStyle
import com.coder.vincent.series.common_lib.resourceToDrawable
import com.coder.vincent.smart_snackbar.SNACK_BAR_ICON_POSITION_LEFT
import com.coder.vincent.smart_snackbar.SNACK_BAR_ICON_POSITION_RIGHT
import com.coder.vincent.smart_snackbar.SnackBarConfig
import com.coder.vincent.smart_snackbar.SnackBar
import kotlin.math.max
import kotlin.math.roundToInt

internal class ScheduledSnackBarImpl(private var config: SnackBarConfig) : ScheduledSnackBar {
    private val bar: SnackBar =
        SnackBar.make(config.targetParent, config.message, config.duration.value,config.style,config.position)
    private val messageView: TextView
    private val actionView: Button

    init {
        bar.view.background.mutate()
        val parent = (bar.view as ViewGroup)[0] as ViewGroup
        messageView = parent[0] as TextView
        messageView.let {
            (it.layoutParams as LinearLayout.LayoutParams).weight = 0f
            messageView.gravity = Gravity.CENTER_VERTICAL
            it.tag = TextStyle(it.textColors.defaultColor, it.textSize, it.paint.isFakeBoldText)
        }
        val spaceLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spaceLp.weight = 1f
        parent.addView(Space(parent.context), 1, spaceLp)
        actionView = parent[2] as Button
        actionView.let {
            it.tag = TextStyle(it.textColors.defaultColor, it.textSize, it.paint.isFakeBoldText)
        }
        setupBarStyle()
    }

    private fun setupBarStyle() {
        bar.setAction(config.actionLabel, config.actionReaction)
        bar.animationMode = config.animationMode.value
        DrawableCompat.setTint(bar.view.background, config.backgroundColor)
        messageView.setTextColor(config.messageColor ?: (messageView.tag as TextStyle).color)
        messageView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            config.messageSize ?: (messageView.tag as TextStyle).size
        )
        val iconDrawable: Drawable? = config.icon?.resourceToDrawable()?.apply {
            val iconSize = calculateIconSize(this)
            setBounds(0, 0, iconSize, iconSize)
        }
        val leftIcon: Drawable? =
            if (config.iconPosition == SNACK_BAR_ICON_POSITION_LEFT) iconDrawable else null
        val rightIcon: Drawable? =
            if (config.iconPosition == SNACK_BAR_ICON_POSITION_RIGHT) iconDrawable else null
        messageView.setCompoundDrawables(leftIcon, null, rightIcon, null)
        messageView.compoundDrawablePadding = config.iconPadding
        messageView.paint.isFakeBoldText = config.messageBold ?: (messageView.tag as TextStyle).bold
        actionView.setTextColor(config.actionLabelColor ?: (actionView.tag as TextStyle).color)
        actionView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            config.actionLabelSize ?: (actionView.tag as TextStyle).size
        )
        actionView.paint.isFakeBoldText =
            config.actionLabelBold ?: (actionView.tag as TextStyle).bold
    }

    private fun calculateIconSize(iconDrawable: Drawable): Int =
        when {
            config.iconSize > 0 -> config.iconSize
            iconDrawable.intrinsicWidth > 0 && iconDrawable.intrinsicHeight > 0 ->
                max(iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)
            else -> {
                (messageView.textSize * 1.4).roundToInt()
            }
        }

    override fun config(): SnackBarConfig = config

    override fun applyNewConfig(config: SnackBarConfig): ScheduledSnackBar {
        this.config = config
        bar.setText(config.message)
        bar.duration = config.duration.value
        setupBarStyle()
        return this
    }

    override fun show() {
        bar.show()
    }

    override fun dismiss() {
        if (bar.isShown){
            bar.dismiss()
        }
    }

    override fun isShowing() = bar.isShown
    override fun isDismissedByGesture(): Boolean = bar.view.visibility == View.GONE
}
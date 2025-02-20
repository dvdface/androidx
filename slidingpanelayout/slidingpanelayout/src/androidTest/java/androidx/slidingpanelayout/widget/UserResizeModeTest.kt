/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.slidingpanelayout.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.get
import androidx.slidingpanelayout.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

private val Exactly100Px = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserResizeModeTest {
    @Test
    fun layoutWithUserResizeEnabled() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        val leftPane = spl[0]
        val rightPane = spl[1]

        assertWithMessage("leftPane width").that(leftPane.width).isEqualTo(50)
        assertWithMessage("rightPane width").that(rightPane.width).isEqualTo(50)
        assertWithMessage("isSlideable").that(spl.isSlideable).isFalse()
        assertWithMessage("SlidingPaneLayout width").that(spl.width).isEqualTo(100)
        assertWithMessage("SlidingPaneLayout height").that(spl.height).isEqualTo(100)

        spl.onTouchEvent(downEvent(50f, 50f))
        spl.onTouchEvent(moveEvent(25f, 50f))
        assertWithMessage("divider dragging").that(spl.isDividerDragging).isTrue()
        spl.onTouchEvent(upEvent(25f, 50f))
        assertWithMessage("visualDividerPosition")
            .that(spl.visualDividerPosition)
            .isEqualTo(30)
        assertWithMessage("SlidingPaneLayout.isLayoutRequested")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
    }

    @Test
    fun dividerClickListenerInvoked() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)

        var wasClicked = false
        spl.setOnUserResizingDividerClickListener { wasClicked = true }

        spl.onTouchEvent(downEvent(50f, 50f))
        spl.onTouchEvent(upEvent(50f, 50f))

        assertWithMessage("click listener invoked").that(wasClicked).isTrue()
    }

    @Test
    fun savedInstanceStateRestoredSameSize() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val sourceSpl = createTestSpl(context).apply {
            id = R.id.slidingPaneLayout
        }

        assertWithMessage("initial splitDividerPosition")
            .that(sourceSpl.splitDividerPosition)
            .isEqualTo(SlidingPaneLayout.SPLIT_DIVIDER_POSITION_AUTO)
        sourceSpl.splitDividerPosition = 35
        assertWithMessage("modified splitDividerPosition")
            .that(sourceSpl.splitDividerPosition)
            .isEqualTo(35)

        val savedState = SparseArray<Parcelable>()
        sourceSpl.saveHierarchyState(savedState)

        val destSpl = createTestSpl(context).apply {
            id = R.id.slidingPaneLayout
        }
        destSpl.restoreHierarchyState(savedState)

        assertWithMessage("restored splitDividerPosition")
            .that(destSpl.splitDividerPosition)
            .isEqualTo(35)
    }

    @Test
    fun visualDividerPositionClipsChildren() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.splitDividerPosition = 35
        spl.drawToBitmap()
        assertWithMessage("left child clip")
            .that((spl[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(0, 0, 35, 100))
        spl.splitDividerPosition = 65
        spl.measureAndLayoutForTest()
        spl.drawToBitmap()
        assertWithMessage("right child clip")
            .that(((spl[1] as ViewGroup)[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(0, 0, 35, 100))
    }

    @Test
    fun splitDividerPositionAffectsLayout() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        assertWithMessage("layout requested after SlidingPaneLayout creation")
            .that(spl.isLayoutRequested)
            .isFalse()
        spl.splitDividerPosition = 35
        assertWithMessage("layout requested by splitDividerPosition change")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("first child expected width")
            .that(spl[0].width)
            .isEqualTo(35)
        assertWithMessage("second child expected width")
            .that(spl[1].width)
            .isEqualTo(65)

        spl.splitDividerPosition = 70
        assertWithMessage("layout requested by splitDividerPosition change (2)")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("first child expected width")
            .that(spl[0].width)
            .isEqualTo(70)
        assertWithMessage("second child expected width")
            .that(spl[1].width)
            .isEqualTo(30)
    }
}

private fun View.drawToBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

private fun createTestSpl(context: Context): SlidingPaneLayout = SlidingPaneLayout(context).apply {
    addView(
        TestPaneView(context).apply {
            minimumWidth = 30
            layoutParams = SlidingPaneLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            ).apply { weight = 1f }
        }
    )
    addView(
        TestPaneView(context).apply {
            minimumWidth = 30
            layoutParams = SlidingPaneLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            ).apply { weight = 1f }
        }
    )
    isUserResizingEnabled = true
    isOverlappingEnabled = false
    setUserResizingDividerDrawable(TestDividerDrawable())
    measureAndLayoutForTest()
}

private fun View.measureAndLayoutForTest() {
    measure(Exactly100Px, Exactly100Px)
    layout(0, 0, measuredWidth, measuredHeight)
}

private class TestDividerDrawable(
    private val intrinsicWidth: Int = 10,
    private val intrinsicHeight: Int = 20
) : Drawable() {

    override fun draw(canvas: Canvas) {}
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth(): Int = intrinsicWidth
    override fun getIntrinsicHeight(): Int = intrinsicHeight
}

private class TestPaneView(context: Context) : View(context) {
    val clipBoundsAtLastDraw = Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        setMeasuredDimension(
            when (widthMode) {
                MeasureSpec.EXACTLY -> widthSize
                MeasureSpec.AT_MOST -> suggestedMinimumWidth.coerceAtMost(widthSize)
                MeasureSpec.UNSPECIFIED -> suggestedMinimumWidth
                else -> error("bad width mode $widthMode")
            },
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> suggestedMinimumHeight.coerceAtMost(heightSize)
                MeasureSpec.UNSPECIFIED -> suggestedMinimumHeight
                else -> error("bad width mode $heightMode")
            }
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.getClipBounds(clipBoundsAtLastDraw)
    }
}

/**
 * Create a test [MotionEvent]; this will have bogus time values, no history
 */
private fun motionEvent(
    action: Int,
    x: Float,
    y: Float,
) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

private fun downEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_DOWN, x, y)
private fun moveEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_MOVE, x, y)
private fun upEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_UP, x, y)
private fun cancelEvent() = motionEvent(MotionEvent.ACTION_CANCEL, 0f, 0f)

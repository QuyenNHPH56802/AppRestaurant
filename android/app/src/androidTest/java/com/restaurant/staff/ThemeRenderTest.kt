package com.restaurant.staff

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restaurant.staff.ui.theme.RestaurantStaffTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PHASE 10 instrumented Compose smoke test. Runs on an emulator.
 * Verifies that the app's theme + a label render.
 */
@RunWith(AndroidJUnit4::class)
class ThemeRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeRendersText() {
        composeTestRule.setContent {
            RestaurantStaffTheme {
                Surface { Text("Restaurant Staff") }
            }
        }
        composeTestRule.onNodeWithText("Restaurant Staff").assertIsDisplayed()
    }
}
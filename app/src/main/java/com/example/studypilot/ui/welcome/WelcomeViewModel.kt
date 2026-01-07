package com.example.studypilot.ui.welcome

import androidx.lifecycle.ViewModel
import com.example.studypilot.R

data class OnboardingSlide(val imageRes: Int, val description: String)

class WelcomeViewModel : ViewModel() {
    val slides = listOf(
        OnboardingSlide(R.drawable.image_1, "Create smart, personalized study plans."),
        OnboardingSlide(R.drawable.image_2, "Balance your studies and well-being."),
        OnboardingSlide(R.drawable.image_3, "Achieve your academic goals with focus.")
    )
}

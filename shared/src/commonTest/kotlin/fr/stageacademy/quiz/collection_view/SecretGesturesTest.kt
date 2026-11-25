package fr.stageacademy.quiz.collection_view

import androidx.compose.ui.geometry.Offset
import fr.stageacademy.quiz.challenge_view.ChallengeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecretGesturesTest {
    @Test
    fun theReticleRequiresAClosedLoop() {
        val loop = listOf(
            Offset(80f, 20f), Offset(120f, 28f), Offset(140f, 60f), Offset(122f, 90f),
            Offset(80f, 100f), Offset(40f, 90f), Offset(20f, 60f), Offset(40f, 28f), Offset(80f, 20f),
        )
        assertTrue(isSecretCircle(loop, 48f))
        assertFalse(isSecretCircle(loop.take(7), 48f))
        assertFalse(isSecretCircle(loop.map { Offset(it.x, 60f) }, 48f))
    }

    @Test
    fun fourEvenTapsDoNotUnlockTheRhythm() {
        val rhythm = SecretRhythm()
        for (time in listOf(0L, 250L, 500L, 750L)) {
            assertFalse(rhythm.tap(time))
        }
        assertFalse(rhythm.tap(3_000L))
        assertFalse(rhythm.tap(3_250L))
        assertFalse(rhythm.tap(4_100L))
        assertTrue(rhythm.tap(4_350L))
    }

    @Test
    fun theClutchOnlyHasAThreeSecondWindowInChrono() {
        assertNull(SecretDiscovery.forTimedAnswer(ChallengeMode.Timer, 4))
        assertNull(SecretDiscovery.forTimedAnswer(ChallengeMode.Timer, 0))
        assertNull(SecretDiscovery.forTimedAnswer(ChallengeMode.Surprise, 2))
        assertEquals(QuizSecret.Clutch, SecretDiscovery.forTimedAnswer(ChallengeMode.Timer, 3))
    }
}

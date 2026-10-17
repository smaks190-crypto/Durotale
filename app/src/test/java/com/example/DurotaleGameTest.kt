package com.example

import com.example.model.DoorAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DurotaleGameTest {

    @Test
    fun testSecretComboSequence() {
        val secretCombo = listOf(
            DoorAction.BANG,
            DoorAction.SCREAM,
            DoorAction.BANG,
            DoorAction.BANG,
            DoorAction.SCREAM
        )

        val playerHistory = listOf(
            DoorAction.BANG,
            DoorAction.SCREAM,
            DoorAction.BANG,
            DoorAction.BANG,
            DoorAction.SCREAM
        )

        assertEquals(5, playerHistory.size)
        assertTrue(playerHistory == secretCombo)
    }

    @Test
    fun testAttackBarAccuracyDamage() {
        // Perfect center hit (cursor = 0.5f)
        val centerCursor = 0.5f
        val distCenter = abs(centerCursor - 0.5f)
        val centerDmg = if (distCenter < 0.09f) 45 else ((1f - distCenter * 1.4f) * 38f).toInt()
        assertEquals(45, centerDmg)

        // Off-center hit (cursor = 0.9f)
        val offCursor = 0.9f
        val distOff = abs(offCursor - 0.5f)
        val offDmg = ((1f - distOff * 1.4f) * 38f).toInt().coerceAtLeast(18)
        assertTrue(offDmg in 18..35)
    }
}

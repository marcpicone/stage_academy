package fr.stageacademy.quiz.profile_view

import fr.stageacademy.quiz.leaderboard_view.DesktopLeaderboardStore
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopStorageTest {
    @Test
    fun onlyASavedCropBecomesTheProfilePhoto() {
        val directory = Files.createTempDirectory("quiz-profile-test").toFile()
        try {
            val picture = File(directory, "picture.png")
            val image = BufferedImage(160, 80, BufferedImage.TYPE_INT_RGB)
            val graphics = image.createGraphics()
            try {
                graphics.color = Color.RED
                graphics.fillRect(0, 0, 80, 80)
                graphics.color = Color.BLUE
                graphics.fillRect(80, 0, 80, 80)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(image, "png", picture)
            ProfileStore(directory).save("Camille", null)
            val photos = DesktopPhotoFiles(directory)
            val imported = photos.importPhoto(picture)
            val draft = photos.saveCrop(imported, PhotoCrop(zoom = 2f, centerX = 0.25f))
            assertNull(assertNotNull(ProfileStore(directory).load()).photo)

            ProfileStore(directory).save(" Camille ", draft)
            photos.discardDraft(draft)
            val saved = assertNotNull(ProfileStore(directory).load())
            assertEquals("Camille", saved.name)
            val preview = photos.loadPreview(assertNotNull(saved.photo))
            assertEquals(preview.width, preview.height)
            val color = Color(preview.getRGB(preview.width / 2, preview.height / 2))
            assertEquals(true, color.red > 200 && color.blue < 50)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun aCompletedRoundCountsOnceAfterRelaunch() {
        val directory = Files.createTempDirectory("quiz-leaderboard-test").toFile()
        try {
            DesktopLeaderboardStore(directory).record("first", "Camille", 2)
            DesktopLeaderboardStore(directory).record("first", "Camille", 2)
            DesktopLeaderboardStore(directory).record("second", "camille", 3)
            val player = DesktopLeaderboardStore(directory).rankings().single()
            assertEquals("camille", player.name.lowercase())
            assertEquals(5L, player.points)
        } finally {
            directory.deleteRecursively()
        }
    }
}

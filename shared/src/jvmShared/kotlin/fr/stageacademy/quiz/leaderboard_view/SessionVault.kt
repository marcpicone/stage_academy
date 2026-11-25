package fr.stageacademy.quiz.leaderboard_view

internal interface SessionVault {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

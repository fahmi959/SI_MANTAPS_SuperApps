package com.presensisiswainformatikabyfahmi.rpp

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int
)

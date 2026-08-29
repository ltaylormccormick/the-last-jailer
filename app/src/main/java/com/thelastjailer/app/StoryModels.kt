package com.thelastjailer.app

data class Scene(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<Choice>
)

data class Choice(
    val label: String,
    val next: String,
    val courage: Int = 0,
    val honour: Int = 0,
    val trophy: String? = null
)

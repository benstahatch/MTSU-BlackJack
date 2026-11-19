package com.mtsu.table21.ui.admin

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font

internal object AdminScreenStyle {
    fun root(titleText: String, onBack: () -> Unit, content: List<javafx.scene.Node>): VBox {
        val title = Label(titleText).apply {
            font = Font.font("Arial", 25.0)
            textFill = Color.web("#F4D27A")
        }
        val back = Button("← Back to Admin Panel").apply {
            style = buttonStyle
            setOnAction { onBack() }
        }
        return VBox(14.0).apply {
            children.add(title)
            children.addAll(content)
            children.add(back)
            alignment = Pos.TOP_LEFT
            padding = Insets(20.0)
            style = "-fx-background-color: #0A4937; -fx-background-radius: 10;"
        }
    }

    fun label(text: String) = Label(text).apply {
        textFill = Color.WHITE
        font = Font.font("Arial", 14.0)
        isWrapText = true
    }

    fun button(text: String) = Button(text).apply { style = buttonStyle }

    private const val buttonStyle = "-fx-background-color: #E6C66C; -fx-text-fill: #063D2B; -fx-font-weight: bold; -fx-background-radius: 7; -fx-cursor: hand;"
}

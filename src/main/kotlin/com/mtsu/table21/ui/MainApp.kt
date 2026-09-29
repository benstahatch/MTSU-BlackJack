package com.mtsu.table21.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class MainApp : Application() {

    override fun start(stage: Stage) {
    	val login=LoginView()
        val scene = Scene(login.getView(), 800.0, 600.0)
 
        stage.title = "Table21"
        stage.scene = scene 
        stage.show()
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}

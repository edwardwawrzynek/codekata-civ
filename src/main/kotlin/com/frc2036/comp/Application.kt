package com.frc2036.comp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import com.frc2036.comp.game.*

@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    val m = GameMap.generateRandom(32)
    println(m)
    runApplication<Application>(*args)
}
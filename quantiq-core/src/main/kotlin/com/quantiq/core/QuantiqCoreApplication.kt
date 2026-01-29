package com.quantiq.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QuantiqCoreApplication

fun main(args: Array<String>) {
    runApplication<QuantiqCoreApplication>(*args)
}

package com.quantiq.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.slf4j.LoggerFactory

@SpringBootApplication
class QuantiqCoreApplication

private val logger = LoggerFactory.getLogger(QuantiqCoreApplication::class.java)

fun main(args: Array<String>) {
    logger.info("Starting Quantiq Core Application...")
    runApplication<QuantiqCoreApplication>(*args)
}

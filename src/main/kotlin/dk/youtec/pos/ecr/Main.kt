@file:JvmName("Main")

package dk.youtec.pos.ecr

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

suspend fun main(args: Array<String>) = coroutineScope {
    println("Point Finland ECR controller!")

    val amount = args.firstOrNull()?.toIntOrNull() ?: 0
    val delaySeconds = args.getOrNull(1)?.toInt() ?: 30

    val controller = PointFinlandEcrController()
    controller.initiatePurchase(amount)

    delay(delaySeconds * 1000L)

    controller.sendStop()

    delay(2000)

    controller.sendCancel()

    delay(2000)
    controller.close()

    println("Done!")
}
@file:JvmName("Main")

package com.mobilepay.pos.ecr

fun main(args: Array<String>) {
    println("Point Finland ECR controller!")

    val amount = args.firstOrNull()?.toIntOrNull() ?: 0

    val controller = PointFinlandEcrController()
    controller.initiatePurchase(amount)

    Thread.sleep(20000)

    controller.sendStop()

    Thread.sleep(2000)

    controller.sendCancel()

    Thread.sleep(2000)
    controller.close()

    println("Done!")
}
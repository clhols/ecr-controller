package dk.youtec.pos.ecr

import org.junit.jupiter.api.Test

class PointFinlandEcrControllerTest {

    @Test
    fun initiatePurchase() {
        val controller = PointFinlandEcrController()
        controller.initiatePurchase(100)

        Thread.sleep(30000)

        controller.sendStop()

        Thread.sleep(2000)

        controller.sendCancel()

        Thread.sleep(2000)

        controller.close()
    }
}
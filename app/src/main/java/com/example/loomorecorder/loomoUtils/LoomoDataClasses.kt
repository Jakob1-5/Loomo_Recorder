package com.example.loomorecorder.loomoUtils

    data class EnableDrive( //
        val drive : Boolean
    )

    data class EnableVision( //
        val depth : Boolean,
        val color : Boolean,
        val colorSmall : Boolean
    )

    data class Head( //
        var pitch :Float, // Head pitch
        var yaw : Float, // Head Yaw
        var li : Int? = null // Head light mode 0-13
    )

    data class Velocity( //
        val v : Float, // Linear Velocity
        val av : Float // Angular velocity
    )

    data class Position(
        val x : Float, // X direction absolute movement
        val y : Float, // Y direction absolute movement
        var th: Float? = null,
        var add: Boolean = false,
        var vls: Boolean = false
    )

    data class PositionArray(
        val x: FloatArray,
        val y: FloatArray,
        var th: FloatArray? = null,
        var add: Boolean = false,
        var vls: Boolean = false
    )

    data class Speak(
        val length : Int, // Length Of string to come
        var pitch : Float = 1.0F, // Pitch of the voice
        var que : Int = 0, // Should the speaker be qued
        var string: String = ""
    )

    data class Volume(
        val v : Double
    )

    data class SensSurroundings(
        val IR_Left : Int,
        val IR_Right : Int,
        val UltraSonic : Int
    )

    data class SensWheelSpeed(
        val SpeedLeft : Float,
        val SpeedRight : Float
    )

    data class SensHeadPoseWorld(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensHeadPoseJoint(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensBaseImu(
        val pitch : Float,
        val roll : Float,
        val yaw : Float
    )

    data class SensBaseTick(
        val left : Int,
        val right : Int
    )

    data class SensPose2D(
        val x : Float,
        val y : Float,
        val theta : Float,
        val linearVelocity : Float,
        val angularVelocity: Float
    )

    data class ImageResponse(
        var size : Int = 0,
        var width : Int = 0,
        var height : Int = 0
    )

    data class CamTf(
        var tx: Float,
        var ty: Float,
        var tz: Float,
        var roll: Float,
        var pitch: Float,
        var yaw: Float
    )

    data class AllSensors(
        val surroundings :SensSurroundings,
        val pose2D : SensPose2D,
        val baseTick :SensBaseTick,
        val wheelSpeed :SensWheelSpeed,
        val headPoseWorld :SensHeadPoseWorld,
        val headPoseJoint :SensHeadPoseJoint,
        val baseImu :SensBaseImu,
        val fishEyeTf: CamTf,
        val colorTf : CamTf,
        val depthTf : CamTf,
        val timeStamp: Long
    )
    {
        override fun toString(): String {
            return """
                All sensors:
                $surroundings
                $wheelSpeed
                $headPoseWorld
                $headPoseJoint
                $baseImu
                $baseTick
                $pose2D
                $fishEyeTf
                $colorTf
                $depthTf
                Timestamp = $timeStamp
            """.trimIndent()
        }
        companion object{
            fun getStringArrayHeader(): Array<String> {
                return arrayOf(
                    "timeStamp",
                    "surroundings_IR_Left",
                    "surroundings_IR_Right",
                    "surroundings_UltraSonic",
                    "pose2D_x",
                    "pose2D_y",
                    "pose2D_theta",
                    "pose2D_linearVelocity",
                    "pose2D_angularVelocity",
                    "baseTick_left",
                    "baseTick_right",
                    "wheelSpeed_SpeedLeft",
                    "wheelSpeed_SpeedRight",
                    "headPoseWorld_roll",
                    "headPoseWorld_pitch",
                    "headPoseWorld_yaw",
                    "headPoseJoint_roll",
                    "headPoseJoint_pitch",
                    "headPoseJoint_yaw",
                    "baseImu_roll",
                    "baseImu_pitch",
                    "baseImu_yaw",
                    "fishEyeTf_tx",
                    "fishEyeTf_ty",
                    "fishEyeTf_tz",
                    "fishEyeTf_roll",
                    "fishEyeTf_pitch",
                    "fishEyeTf_yaw",
                    "colorTf_tx",
                    "colorTf_ty",
                    "colorTf_tz",
                    "colorTf_roll",
                    "colorTf_pitch",
                    "colorTf_yaw",
                    "depthTf_tx",
                    "depthTf_ty",
                    "depthTf_tz",
                    "depthTf_roll",
                    "depthTf_pitch",
                    "depthTf_yaw"
                )
            }
        }
        fun toStringArray(): Array<String> {
            return arrayOf(
                timeStamp.toString(),
                surroundings.IR_Left.toString(),
                surroundings.IR_Right.toString(),
                surroundings.UltraSonic.toString(),
                pose2D.x.toString(),
                pose2D.y.toString(),
                pose2D.theta.toString(),
                pose2D.linearVelocity.toString(),
                pose2D.angularVelocity.toString(),
                baseTick.left.toString(),
                baseTick.right.toString(),
                wheelSpeed.SpeedLeft.toString(),
                wheelSpeed.SpeedRight.toString(),
                headPoseWorld.roll.toString(),
                headPoseWorld.pitch.toString(),
                headPoseWorld.yaw.toString(),
                headPoseJoint.roll.toString(),
                headPoseJoint.pitch.toString(),
                headPoseJoint.yaw.toString(),
                baseImu.roll.toString(),
                baseImu.pitch.toString(),
                baseImu.yaw.toString(),
                fishEyeTf.tx.toString(),
                fishEyeTf.ty.toString(),
                fishEyeTf.tz.toString(),
                fishEyeTf.roll.toString(),
                fishEyeTf.pitch.toString(),
                fishEyeTf.yaw.toString(),
                colorTf.tx.toString(),
                colorTf.ty.toString(),
                colorTf.tz.toString(),
                colorTf.roll.toString(),
                colorTf.pitch.toString(),
                colorTf.yaw.toString(),
                depthTf.tx.toString(),
                depthTf.ty.toString(),
                depthTf.tz.toString(),
                depthTf.roll.toString(),
                depthTf.pitch.toString(),
                depthTf.yaw.toString()
            )
        }
    }
//}
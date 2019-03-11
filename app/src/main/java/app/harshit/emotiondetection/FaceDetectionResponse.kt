package app.harshit.emotiondetection

import android.graphics.Rect

class FaceDetectionResponse(
    val responses: ArrayList<FaceResponse> = arrayListOf()
)

class FaceResponse(
    val faceAnnotations: ArrayList<FaceDetectionFace> = arrayListOf()
)

class FaceDetectionFace(
    val boundingPoly: BoundingPoly,
    val joyLikelihood: Emotion,
    val sorrowLikelihood: Emotion,
    val angerLikelihood: Emotion,
    val surpriseLikelihood: Emotion,
    val blurredLikelihood: Emotion,
    val headwearLikelihood: Emotion
)

class BoundingPoly(
    val vertices: ArrayList<BoundingPolyVertices>
)

class BoundingPolyVertices(
    val x: Int,
    val y: Int
)

fun getRectFromVertices(vertices: List<BoundingPolyVertices>): Rect {
    val x1 = vertices[0].x
    val x2 = vertices[1].x
    val y1 = vertices[2].y
    val y2 = vertices[1].y

    return Rect(x1, y1, x2, y2)
}

enum class Emotion(val emotion: String) {
    VERY_LIKELY("VERY_LIKELY"),
    LIKELY("LIKELY"),
    POSSIBLE("POSSIBLE"),
    UNLIKELY("UNLIKELY"),
    VERY_UNLIKELY("VERY_UNLIKELY"),
    UNKNOWN("UNKNOWN")
}
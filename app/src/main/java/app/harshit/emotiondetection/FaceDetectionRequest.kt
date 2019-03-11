package app.harshit.emotiondetection

class FaceDetectionRequest(
    val requests: ArrayList<Request>
)

class Request(
    val image: RequestImage,
    val features: ArrayList<RequestFeature> = arrayListOf(RequestFeature())
)

class RequestImage(
    val content: String
)

class RequestFeature(
    val type: String = "FACE_DETECTION",
    val maxResults: Int = 5
)

package com.reco1l.osu.beatmaplisting

/**
 * Defines an action to be performed on a mirror API.
 */
open class BeatmapMirrorAction<RequestModel>(

    /**
     * The action API endpoint.
     */
    val request: RequestModel

)

class BeatmapMirrorActionWithResponse<RequestModel, ResponseModel>(

    request: RequestModel,

    /**
     * The action response mapping.
     */
    val response: ResponseModel

) : BeatmapMirrorAction<RequestModel>(request)

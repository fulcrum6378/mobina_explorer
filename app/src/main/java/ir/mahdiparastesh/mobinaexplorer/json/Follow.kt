package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("unused")
class Follow( // Both following and followers
    val next_max_id: String? = null,
    val users: Array<User>,
    val big_list: Boolean,
    val page_size: Float,
    status: String
) : Rest(status)

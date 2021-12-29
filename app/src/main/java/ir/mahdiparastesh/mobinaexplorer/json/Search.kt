package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("unused")
class Search(
    val places: Array<HashMap<String, *>>,
    val hashtags: Array<HashMap<String, *>>,
    val rank_token: String,
    val has_more: Boolean,
    val users: Array<ItemUser>,
    status: String
): Rest(status) {

    class ItemUser(val position: Float, val user: User) {
        class Sort : Comparator<ItemUser> {
            override fun compare(a: ItemUser, b: ItemUser) = a.position.toInt() - b.position.toInt()
        }
    }
}

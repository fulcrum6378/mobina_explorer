package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("unused")
open class Rest(val status: String) {

    class User(
        val pk: String,
        val username: String,
        val full_name: String,
        val is_private: Boolean,
        val profile_pic_url: String,
        val profile_pic_id: String,
        val is_verified: Boolean,
        val follow_friction_type: Float,
        val has_anonymous_profile_picture: Boolean,
        val has_highlight_reels: Boolean,
        val account_badges: Array<HashMap<String, *>>,
        val friendship_status: HashMap<String, *>,
        val latest_reel_media: Float,
        val should_show_category: Boolean
    )

    open class GraphQLResponse(val data: GraphQL, status: String) : Rest(status)

    class Friendships(val friendships: HashMap<String, Friendship>, status: String) : Rest(status)

    class Friendship(
        val following: Boolean,
        val incoming_request: Boolean,
        val is_bestie: Boolean,
        val is_private: Boolean,
        val is_restricted: Boolean,
        val outgoing_request: Boolean,
        val is_feed_favorite: Boolean,
    )
}

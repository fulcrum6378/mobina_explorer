package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("SpellCheckingInspection")
open class Rest {
    @Suppress("unused")
    lateinit var status: String

    class User(
        //val account_badges: Array<Map<String, *>>,
        //val account_type: Float?,
        //val auto_expand_chaining: Boolean?,
        //val biography: String?,
        //val can_be_reported_as_fraud: Boolean?,
        //val creator_shopping_info: Map<String, *>?,
        //val external_url: String?,
        //val fbid_v2: Double,
        //val follow_friction_type: Float?,
        //val follower_count: Float?,
        //val following_count: Float?,
        //val following_tag_count: Float?,
        val friendship_status: Friendship?,
        val full_name: String?,
        //val has_anonymous_profile_picture: Boolean,
        //val has_guides: Boolean?,
        //val has_highlight_reels: Boolean,
        //val has_unseen_besties_media: Boolean?,
        //val has_videos: Boolean?,
        //val hd_profile_pic_versions: Array<Candidate>?,
        //val hd_profile_pic_url_info: Candidate?,
        //val highlight_reshare_disabled: Boolean?,
        //val include_direct_blacklist_status: Boolean?,
        //val interop_messaging_user_fbid: String?,
        //val is_bestie: Boolean?,
        //val is_business: Boolean?,
        //val is_call_to_action_enabled: Boolean?, // actually nullable
        //val is_favorite: Boolean?,
        //val is_interest_account: Boolean?,
        //val is_memorialized: Boolean?,
        //val is_potential_business: Boolean?,
        val is_private: Boolean,
        //val is_using_unified_inbox_for_direct: Boolean?,
        //val is_verified: Boolean,
        //val latest_reel_media: Double,
        //val media_count: Float?,
        //val mutual_followers_count: Float?,
        //val open_external_url_with_in_app_browser: Boolean?,
        val pk: String,
        //val primary_profile_link_type: Float?,
        //val professional_conversion_suggested_account_type: Float?,
        //val profile_context: String?,
        //val profile_context_facepile_users: Array<Any>?,
        //val profile_context_links_with_user_ids: Array<Any>?,
        //val profile_pic_url: String,
        //val profile_pic_id: String,
        //val pronouns: Array<Any>?,
        //val reel_auto_archive: String?,
        //val request_contact_enabled: Boolean?,
        //val should_show_category: Boolean,
        //val show_account_transparency_details: Boolean?,
        //val show_fb_link_on_profile: Boolean?,
        //val show_post_insights_entry_point: Boolean?,
        //val total_ar_effects: Float?,
        //val total_igtv_videos: Float?,
        val username: String,
        //val usertags_count: Float?,
        //val wa_addressable: Any?,// Double or Boolean
        //val wa_eligibility: Double?
    )

    class Follow(
        // Both following and followers
        val next_max_id: String? = null,
        val users: Array<User>,
        //val big_list: Boolean,
        //val page_size: Double,
    ) : Rest()

    class Friendships(val friendship_statuses: Map<String, Friendship>) : Rest()

    class Friendship(
        val following: Boolean,
        //val incoming_request: Boolean,
        //val is_bestie: Boolean,
        //val is_private: Boolean,
        //val is_restricted: Boolean,
        //val outgoing_request: Boolean,
        //val is_feed_favorite: Boolean,
    )

    class Search(
        //val places: Array<Map<String, *>>,
        //val hashtags: Array<Map<String, *>>,
        //val rank_token: String,
        //val has_more: Boolean,
        val users: Array<ItemUser>,
    ) : Rest() {

        class ItemUser(val position: Float, val user: User) {
            class Sort : Comparator<ItemUser> {
                override fun compare(a: ItemUser, b: ItemUser) =
                    a.position.toInt() - b.position.toInt()
            }
        }
    }

    class ProfileInfo(val user: User) : Rest()

    //open class Candidate(val width: Float, val height: Float, val url: String)
}

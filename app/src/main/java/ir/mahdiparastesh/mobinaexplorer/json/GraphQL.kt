package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("unused", "SpellCheckingInspection")
class GraphQL(val user: User) {
    class User(
        val biography: String?,
        val blocked_by_viewer: Boolean?,
        val business_address_json: Any?,
        val business_category_name: Any?,
        val business_contact_method: Any?,
        val business_email: Any?,
        val business_phone_number: Any?,
        val category_enum: Any?,
        val category_name: Any?,
        val connected_fb_page: Any?,
        val country_block: Boolean?,
        val edge_felix_video_timeline: HashMap<String?, *>?,
        val edge_follow: HashMap<String?, *>?,
        val edge_followed_by: HashMap<String?, *>?,
        val edge_media_collections: HashMap<String?, *>?,
        val edge_mutual_followed_by: HashMap<String?, *>?,
        val edge_owner_to_timeline_media: TimelineMedia?,
        val edge_saved_media: HashMap<String?, *>?,
        val external_url: Any?,
        val external_url_linkshimmed: Any?,
        val fbid: String?,
        val followed_by_viewer: Boolean?,
        val follows_viewer: Boolean?,
        val full_name: String?,
        val has_ar_effects: Boolean?,
        val has_blocked_viewer: Boolean?,
        val has_channel: Boolean?,
        val has_clips: Boolean?,
        val has_guides: Boolean?,
        val has_requested_viewer: Boolean?,
        val hide_like_and_view_counts: Boolean?,
        val highlight_reel_count: Float?,
        val id: String?, // The same as Rest.User.pk
        val is_business_account: Boolean?,
        val is_embeds_disabled: Boolean?,
        val is_joined_recently: Boolean?,
        val is_private: Boolean?,
        val is_professional_account: Boolean?,
        val is_verified: Boolean?,
        val overall_category_name: Any?,
        val profile_pic_url: Any?,
        val profile_pic_url_hd: String?,
        val pronouns: Array<Any>?,
        val requested_by_viewer: Boolean?,
        val restricted_by_viewer: Boolean?,
        val should_show_category: Boolean?,
        val should_show_public_contacts: Boolean?,
        val username: String?,
    )

    class TimelineMedia(val page_info: PageInfo, val count: Float, val edges: Array<EdgePost>)

    class PageInfo(val has_next_page: Boolean, val end_cursor: String)

    class EdgePost(val node: Post)

    class Post(
        val __typename: String,
        val accessibility_caption: Any?,
        val clips_music_attribution_info: Any?,
        val coauthor_producers: Array<Any>,
        val comments_disabled: Boolean,
        val dash_info: HashMap<String, *>,
        val display_url: String,
        val dimensions: HashMap<String, Float>,
        val edge_liked_by: HashMap<String, Float>,
        val edge_media_preview_like: HashMap<String, *>,
        val edge_media_to_caption: EdgesCaption,
        val edge_media_to_comment: HashMap<String, *>,
        val edge_media_to_tagged_user: EdgeTaggedUsers,
        val fact_check_information: Any?,
        val fact_check_overall_rating: Any?,
        val felix_profile_grid_crop: Any?,
        val gating_info: Any?,
        val has_audio: Boolean,
        val has_upcoming_event: Boolean,
        val id: String,
        val is_video: Boolean,
        val location: Any?,
        val media_overlay_info: Any?,
        val media_preview: Any?,
        val owner: HashMap<String, String>,
        val product_type: String,
        val sharing_friction_info: HashMap<String, *>,
        val shortcode: String,
        val taken_at_timestamp: Double,
        val thumbnail_resources: Array<ThumbRes>,
        val thumbnail_src: String,
        val tracking_token: String,
        val video_url: String,
        val video_view_count: Float,
    )

    class EdgesCaption(val edges: Array<EdgeCaption>)

    class EdgeCaption(val node: Caption)

    class Caption(val text: String)

    class EdgeTaggedUsers(val node: Array<Any>)

    class ThumbRes(val src: String, val config_width: Float, val config_height: Float)
}

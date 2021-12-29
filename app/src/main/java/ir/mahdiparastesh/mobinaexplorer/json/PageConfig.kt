package ir.mahdiparastesh.mobinaexplorer.json

@Suppress("unused", "SpellCheckingInspection")
class PageConfig(
    val browser_push_pub_key: String,
    val bundle_variant: String,
    val cache_schema_version: Float,
    val config: HashMap<String, *>,
    val connection_quality_rating: String,
    val consent_dialog_config: HashMap<String, *>,
    val country_code: String,
    val deployment_stage: String,
    val device_id: String,
    val encryption: HashMap<String, *>,
    val entry_data: EntryData,
    val is_dev: Boolean,
    val is_whitelisted_crawl_bot: Boolean,
    val frontend_env: String,
    val hostname: String,
    val knobx: HashMap<String, *>,
    val language_code: String,
    val locale: String,
    val mid_pct: Float,
    val nonce: String,
    val platform: String,
    val privacy_flow_trigger: Any?,
    val rollout_hash: String,
    val server_checks: HashMap<String, *>,
    val signal_collection_config: HashMap<String, *>,
    val to_cache: HashMap<String, *>,
    val www_routing_config: HashMap<String, *>,
    val zero_data: HashMap<String, *>,
) {
    class EntryData(val ProfilePage: Array<ProfilePage>)

    class ProfilePage(
        val always_show_message_button_to_pro_account: Boolean,
        val graphql: GraphQL,
        val logging_page_id: String,
        val profile_pic_edit_sync_props: HashMap<String, *>,
        val seo_category_infos: Array<Array<String>>,
        val show_follow_dialog: Boolean,
        val show_suggested_profiles: Boolean,
        val show_view_shop: Boolean,
        val toast_content_on_load: Any?,
    )
}

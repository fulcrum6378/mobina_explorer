package ir.mahdiparastesh.mobinaexplorer.view

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.DecimalFormat
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.mobinaexplorer.Fetcher
import ir.mahdiparastesh.mobinaexplorer.Panel
import ir.mahdiparastesh.mobinaexplorer.R
import ir.mahdiparastesh.mobinaexplorer.databinding.ListUserBinding

class ListUser(private val c: Panel) : RecyclerView.Adapter<ListUser.ViewHolder>() {
    class ViewHolder(val b: ListUserBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ListUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: ViewHolder, i: Int) {
        if (c.candidature == null) return

        val scr = if (c.candidature!![i].score != -1f)
            DecimalFormat("#.##").format(c.candidature!![i].score * 100f) + "%"
        else "nominal"
        h.b.name.text = "${i + 1}. ${c.candidature!![i].nominee?.name} ($scr)"
        val where = when {
            c.candidature!![i].where == "P" -> "avatar"
            c.candidature!![i].where == "PT" -> "bio"
            c.candidature!![i].where.startsWith("T_") -> "post ${
                try {
                    c.candidature!![i].where.substring(2).toInt() + 1
                } catch (ignored: NumberFormatException) {
                    c.candidature!![i].where.substring(2)
                }
            } -> caption"
            else -> {
                val pp = c.candidature!![i].where.split("_")
                "post ${pp[0].toInt() + 1} -> slide ${pp[1].toInt() + 1}"
            }
        }
        h.b.user.text = "${c.candidature!![i].nominee?.user} -> $where"

        h.b.root.alpha = if (c.candidature!![i].rejected) Panel.DISABLED_ALPHA else 1f
        h.b.root.setOnClickListener {
            val uri = Uri.parse(
                Fetcher.Type.PROFILE.url.format(c.candidature!![h.layoutPosition].nominee?.user)
            )
            c.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        h.b.root.setOnLongClickListener { v ->
            PopupMenu(ContextThemeWrapper(c, R.style.Theme_MobinaExplorer), v).apply {
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.cmReject -> {
                            UiWork(
                                c, Panel.Action.REJECT, c.candidature!![h.layoutPosition]
                            ).start()
                            true
                        }
                        R.id.cmAccept -> {
                            UiWork(
                                c, Panel.Action.ACCEPT, c.candidature!![h.layoutPosition]
                            ).start()
                            true
                        }
                        else -> false
                    }
                }
                inflate(
                    if (!c.candidature!![h.layoutPosition].rejected) R.menu.can_normal
                    else R.menu.can_rejected
                )
                show()
            }
            true
        }
    }

    override fun getItemCount() = c.candidature!!.size
}

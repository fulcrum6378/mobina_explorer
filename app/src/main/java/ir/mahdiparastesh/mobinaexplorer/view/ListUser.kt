package ir.mahdiparastesh.mobinaexplorer.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.CountDownTimer
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.mobinaexplorer.Crawler
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
        val spot = c.candidature!![i].where
        val where = when {
            spot == "P" -> "avatar"
            spot == "PT" -> "bio"
            spot.startsWith("T_") -> "post ${
                try {
                    spot.substring(2).toInt() + 1
                } catch (ignored: NumberFormatException) {
                    spot.substring(2)
                }
            } -> caption"
            else -> {
                val pp = spot.split("_")
                "post ${pp[0].toInt() + 1} -> slide ${pp[1].toInt() + 1}"
            }
        }
        h.b.user.text = "${c.candidature!![i].nominee?.user} -> $where"

        h.b.root.alpha = if (c.candidature!![i].rejected) Panel.DISABLED_ALPHA else 1f
        h.b.root.setOnClickListener { v ->
            PopupMenu(ContextThemeWrapper(c, R.style.Theme_MobinaExplorer), v).apply {
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.cmInstagram -> {
                            UiTools.openProfile(c, c.candidature!![h.layoutPosition].nominee!!.user)
                            true; }
                        R.id.cmInstaTools -> {
                            c.startActivity(Intent().apply {
                                component = ComponentName(
                                    "ir.mahdiparastesh.instatools",
                                    "ir.mahdiparastesh.instatools.Viewer"
                                )
                                putExtra(
                                    "EXTRA_USER", c.candidature!![h.layoutPosition].nominee!!.user
                                )
                                putExtra(
                                    "EXTRA_ID", c.candidature!![h.layoutPosition].id.toString()
                                )
                            })// c.finish()
                            true
                        }
                        R.id.cmRepair -> {
                            Crawler.handler?.let { handler ->
                                handler.obtainMessage(
                                    Crawler.HANDLE_REQ_REPAIR,
                                    c.candidature!![h.layoutPosition].nominee
                                ).sendToTarget()
                                val wait = 5000L
                                object : CountDownTimer(wait, wait) {
                                    override fun onTick(millisUntilFinished: Long) {}
                                    override fun onFinish() {
                                        Panel.handler?.obtainMessage(Panel.Action.REFRESH.ordinal)
                                            ?.sendToTarget()
                                    }
                                }.start()
                            }; true; }
                        else -> false
                    }
                }
                inflate(R.menu.candidate)
                show()
            }
        }
        h.b.root.setOnLongClickListener {
            UiWork(
                c, if (!c.candidature!![h.layoutPosition].rejected) Panel.Action.REJECT
                else Panel.Action.ACCEPT, c.candidature!![h.layoutPosition]
            ).start()
            true
        }
    }

    override fun getItemCount() = c.candidature!!.size
}

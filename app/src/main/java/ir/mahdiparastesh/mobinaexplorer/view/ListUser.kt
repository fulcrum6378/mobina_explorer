package ir.mahdiparastesh.mobinaexplorer.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.icu.text.DecimalFormat
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
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
        if (c.m.candidature == null) return

        val scr = if (c.m.candidature!![i].score != -1f)
            DecimalFormat("#.##").format(c.m.candidature!![i].score * 100f) + "%"
        else "nominal"
        h.b.name.text = "${i + 1}. ${c.m.candidature!![i].nominee?.name} ($scr)"
        val scope = c.m.candidature!![i].scope
        val where = when {
            scope == "P" -> "avatar"
            scope == "PT" -> "bio"
            scope.startsWith("T_") -> "post ${
                try {
                    scope.substring(2).toInt() + 1
                } catch (ignored: NumberFormatException) {
                    scope.substring(2)
                }
            } -> caption"
            else -> {
                val pp = scope.split("_")
                "post ${pp[0].toInt() + 1} -> slide ${pp[1].toInt() + 1}"
            }
        }
        h.b.user.text = "${c.m.candidature!![i].nominee?.user} -> $where"

        h.b.root.alpha = if (c.m.candidature!![i].rejected) Panel.DISABLED_ALPHA else 1f
        h.b.root.setBackgroundResource(
            if (c.m.candidature!![i].obscure) R.color.obscure else R.drawable.button
        )
        h.b.root.setOnClickListener { v ->
            PopupMenu(ContextThemeWrapper(c, R.style.Theme_MobinaExplorer), v).apply {
                setOnMenuItemClickListener {
                    if (c.m.candidature == null || h.layoutPosition >= c.m.candidature!!.size)
                        return@setOnMenuItemClickListener false
                    when (it.itemId) {
                        R.id.cmInstagram -> {
                            UiTools.openProfile(
                                c,
                                c.m.candidature!![h.layoutPosition].nominee!!.user
                            )
                            true; }
                        R.id.cmInstaTools -> {
                            c.startActivity(Intent().apply {
                                component = ComponentName(
                                    "ir.mahdiparastesh.instatools",
                                    "ir.mahdiparastesh.instatools.Viewer"
                                )
                                putExtra(
                                    "EXTRA_USER", c.m.candidature!![h.layoutPosition].nominee!!.user
                                )
                            })
                            true; }
                        R.id.cmObscure -> {
                            UiWork(
                                c, Panel.Action.UPDATE, c.m.candidature!![h.layoutPosition]
                                    .apply {
                                        obscure = !c.m.candidature!![h.layoutPosition].obscure
                                    }
                            ).start()
                            true; }
                        R.id.cmRepair -> {
                            UiWork(
                                c, Panel.Action.REPAIR, c.m.candidature!![h.layoutPosition].nominee
                            ).start(); true; }
                        else -> false
                    }
                }
                inflate(R.menu.candidate)
                menu.findItem(R.id.cmObscure).isChecked =
                    c.m.candidature!![h.layoutPosition].obscure
                show()
            }
        }
        h.b.root.setOnLongClickListener {
            if (c.m.candidature == null || h.layoutPosition >= c.m.candidature!!.size)
                return@setOnLongClickListener true
            UiWork(
                c, Panel.Action.UPDATE, c.m.candidature!![h.layoutPosition].apply {
                    rejected = !c.m.candidature!![h.layoutPosition].rejected
                }
            ).start()
            true
        }
    }

    override fun getItemCount() = c.m.candidature!!.size
}

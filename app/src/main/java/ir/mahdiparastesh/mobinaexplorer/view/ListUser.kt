package ir.mahdiparastesh.mobinaexplorer.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.mobinaexplorer.Fetcher
import ir.mahdiparastesh.mobinaexplorer.Panel
import ir.mahdiparastesh.mobinaexplorer.databinding.ListUserBinding
import ir.mahdiparastesh.mobinaexplorer.room.Candidate

class ListUser(private val list: List<Candidate>, private val that: Panel) :
    RecyclerView.Adapter<ListUser.ViewHolder>() {
    class ViewHolder(val b: ListUserBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ListUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: ViewHolder, i: Int) {
        h.b.name.text = "${i + 1}. ${list[i].nominee?.name}"
        h.b.user.text = list[i].nominee?.user
        h.b.root.setOnClickListener {
            val uri = Uri.parse(Fetcher.Type.PROFILE.url.format(list[h.layoutPosition].nominee?.user))
            that.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun getItemCount() = list.size
}

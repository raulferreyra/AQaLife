package com.urasweb.aqualife.ui.imc_historic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.urasweb.aqualife.R
import com.urasweb.aqualife.data.local.ImcRecordEntity
import com.urasweb.aqualife.data.local.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImcHistoricAdapter : RecyclerView.Adapter<ImcHistoricAdapter.ImcViewHolder>() {

    private val items = mutableListOf<ImcRecordEntity>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun setItems(newItems: List<ImcRecordEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImcViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imc_record, parent, false)
        return ImcViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImcViewHolder, position: Int) {
        val item = items[position]

        holder.tvDate.text = dateFormat.format(Date(item.updatedAt))
        holder.tvPesoTalla.text =
            "Peso: %.1f kg   Talla: %.2f m".format(item.pesoKg, item.tallaM)

        holder.tvPerimetro.text =
            "Perímetro abdominal: %.1f cm".format(item.perimetroAbdominalCm)

        holder.tvImc.text =
            "IMC: %.1f (%s)".format(item.imc, item.clasificacionImc)

        holder.tvSyncStatus.text = when (item.syncStatus) {
            SyncStatus.SYNCED -> "Sincronizado"
            SyncStatus.DIRTY -> "Pendiente de sincronizar"
            SyncStatus.ERROR -> "Error de sincronización"
        }
    }

    override fun getItemCount(): Int = items.size

    class ImcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPesoTalla: TextView = itemView.findViewById(R.id.tvPesoTalla)
        val tvPerimetro: TextView = itemView.findViewById(R.id.tvPerimetro)
        val tvImc: TextView = itemView.findViewById(R.id.tvImc)
        val tvSyncStatus: TextView = itemView.findViewById(R.id.tvSyncStatus)
    }
}

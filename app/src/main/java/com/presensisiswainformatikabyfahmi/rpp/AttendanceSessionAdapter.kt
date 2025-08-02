package com.presensisiswainformatikabyfahmi.rpp

import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.TextView
import android.widget.Switch
import android.widget.ImageView
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.presensisiswainformatikabyfahmi.R

class AttendanceSessionAdapter(
    private val sessions: MutableList<AttendanceSession>,
    private val listener: (AttendanceSession) -> Unit,
    private val toggleListener: (AttendanceSession, Boolean) -> Unit,
    private val editNameListener: (AttendanceSession) -> Unit,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<AttendanceSessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSessionName: TextView = view.findViewById(R.id.tvSessionName)
        val tvSessionStatus: TextView = view.findViewById(R.id.tvSessionStatus)
        val btnGenerateReport: Button = view.findViewById(R.id.btnGenerateReport)
        val switchActiveStatus: Switch = view.findViewById(R.id.switchActiveStatus)
        val ivEditSessionName: ImageView = view.findViewById(R.id.ivEditSessionName)
        val ivDeleteSession: ImageView = view.findViewById(R.id.ivDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.tvSessionName.text = "${session.name} (${session.date})"
        holder.tvSessionStatus.text = if (session.isActive) "Status: Aktif" else "Status: Nonaktif"

        holder.btnGenerateReport.setOnClickListener { listener(session) }

        holder.ivEditSessionName.visibility = if (isAdmin) View.VISIBLE else View.GONE
        holder.ivDeleteSession.visibility = if (isAdmin) View.VISIBLE else View.GONE
        holder.switchActiveStatus.visibility = if (isAdmin) View.VISIBLE else View.GONE

        holder.ivEditSessionName.setOnClickListener {
            if (isAdmin) editNameListener(session)
        }

        holder.ivDeleteSession.setOnClickListener {
            if (isAdmin) showDeleteConfirmationDialog(holder.itemView, session, position)
        }

        holder.switchActiveStatus.setOnCheckedChangeListener(null)
        holder.switchActiveStatus.isChecked = session.isActive

        holder.switchActiveStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isAdmin) {
                holder.tvSessionStatus.text = if (isChecked) "Status: Aktif" else "Status: Nonaktif"
                toggleListener(session, isChecked)
            }
        }
    }

    private fun showDeleteConfirmationDialog(view: View, session: AttendanceSession, position: Int) {
        val context = view.context
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus sesi '${session.name}'?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteSession(view, session, position)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteSession(view: View, session: AttendanceSession, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("attendance_sessions").document(session.id)
            .delete()
            .addOnSuccessListener {
                sessions.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, sessions.size)
                Toast.makeText(view.context, "Sesi berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Gagal menghapus sesi", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount() = sessions.size
}

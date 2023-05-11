package com.concius.firenote

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class NoteActivity : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private lateinit var toolbar: Toolbar
    private lateinit var noteRecyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        auth = FirebaseAuth.getInstance()
        db = Firebase.database.reference
        toolbar = findViewById(R.id.toolbar)

        toolbar.title = "My Notes"
        setSupportActionBar(toolbar)

        val addNoteButton = findViewById<Button>(R.id.add_note_button)
        addNoteButton.setOnClickListener { addNote() }

        noteRecyclerView = findViewById(R.id.note_recycler_view)
        noteRecyclerView.layoutManager = LinearLayoutManager(this)
        noteRecyclerView.adapter = NoteAdapter(this)

        syncNotes()
    }

    private fun addNote() {
        val titleEditText = findViewById<EditText>(R.id.note_title_edit_text)
        val contentEditText = findViewById<EditText>(R.id.note_content_edit_text)

        if (titleEditText.text.isEmpty() || contentEditText.text.isEmpty()) {
            Toast.makeText(this, "Please enter a title and content for the note.", Toast.LENGTH_SHORT).show()
            return
        }

        val newNote = mapOf("title" to titleEditText.text.toString(), "content" to contentEditText.text.toString())

        db.child("notes").push().setValue(newNote)

        Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show()
        titleEditText.setText("")
        contentEditText.setText("")
    }

    private fun deleteNote(noteId: String) {
        db.child("notes").child(noteId).removeValue()
        Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
    }

    private fun syncNotes() {
        db.child("notes").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val updatedNotes = dataSnapshot.children.map { noteSnapshot ->
                        noteSnapshot.key?.let { key ->
                            (noteSnapshot.value as Map<String, Any>).plus("id" to key)
                        }
                    }.filterNotNull()

                    (noteRecyclerView.adapter as NoteAdapter).updateNotes(updatedNotes)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NoteActivity, "Failed to sync notes: \${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun onNoteDeleteClick(noteId: String) {
        deleteNote(noteId)
    }

    class NoteAdapter(private val onNoteClickListener: NoteActivity) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {
        private var notes: List<Map<String, Any>> = emptyList()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val noteTitle: TextView = itemView.findViewById(R.id.note_title)
            val noteContent: TextView = itemView.findViewById(R.id.note_content)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val note = notes[position]
            holder.noteTitle.text = note["title"] as String
            holder.noteContent.text = note["content"] as String
            holder.itemView.findViewById<ImageButton>(R.id.note_delete_button).setOnClickListener {
                onNoteClickListener.onNoteDeleteClick(notes[position]["id"] as String)
            }
        }

        override fun getItemCount(): Int {
            return notes.size
        }

        fun updateNotes(newNotes: List<Map<String, Any>>) {
            notes = newNotes
            notifyDataSetChanged()
        }

        interface OnNoteClickListener {
            fun onNoteDeleteClick(noteId: String)
        }
    }
}

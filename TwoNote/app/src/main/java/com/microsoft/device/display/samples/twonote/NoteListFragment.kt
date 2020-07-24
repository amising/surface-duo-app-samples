/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.microsoft.device.display.samples.twonote.model.DataProvider
import com.microsoft.device.display.samples.twonote.model.DirEntry
import com.microsoft.device.display.samples.twonote.model.INode
import com.microsoft.device.display.samples.twonote.model.Note
import com.microsoft.device.dualscreen.layout.ScreenHelper
import java.time.LocalDateTime

class NoteListFragment : Fragment(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private var arrayAdapter: ArrayAdapter<INode>? = null
    private var listView: ListView? = null
    private lateinit var inodes: MutableList<INode>
    private var selectedItemPosition: Int = 0
    private val ROOT = ""
    private var noteSelectionListener: NoteSelectionListener? = null

    companion object {
        const val LIST_VIEW = "list view"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inodes = DataProvider.inodes
        activity?.let {
            arrayAdapter = object : ArrayAdapter<INode>(it, android.R.layout.simple_list_item_2, android.R.id.text1, inodes) {
                // Override getView function so that ArrayAdapter can be used while both text
                // views in the simple_list_item_2 format are updated
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)

                    val text1 = view.findViewById<View>(android.R.id.text1) as TextView
                    text1.text = inodes[position].title
                    text1.setTypeface(null, Typeface.BOLD)

                    val text2 = view.findViewById<View>(android.R.id.text2) as TextView
                    text2.text = inodes[position].dateModifiedString()
                    text2.setTextColor(it.getColor(R.color.colorOnBackgroundVariant))

                    return view
                }
            }
        }
        FileHandler.loadDirectory(requireContext(), ROOT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(LIST_VIEW, listView?.onSaveInstanceState())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        // writeDirEntry("", DirEntry())  // uncomment this to clear record of all root entries (use for testing)

        listView = view.findViewById(R.id.list_view)
        listView?.let {
            it.adapter = arrayAdapter
            it.onItemClickListener = this
            it.onItemLongClickListener = this
            noteSelectionListener = NoteSelectionListener(this, it, arrayAdapter!!)
            it.setMultiChoiceModeListener(noteSelectionListener)
            it.choiceMode = ListView.CHOICE_MODE_SINGLE

            // REVISIT: is this necessary
            if (savedInstanceState != null)
                listView?.onRestoreInstanceState(savedInstanceState.getParcelable(LIST_VIEW))
        }

        view.findViewById<FloatingActionButton>(R.id.add_fab).setOnClickListener {
            // Set selected item to newly created note (first element in list)
            val inode = FileHandler.addInode(ROOT)
            arrayAdapter?.notifyDataSetChanged()
            startNoteFragment(inode)
        }

        // Set up toolbar icons and actions
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.inflateMenu(R.menu.menu_note_list)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        requireContext().let {
            // TODO: switch to colored icon
            toolbar.setNavigationIcon(R.drawable.ic_icon_unfilled)
            toolbar.navigationIcon?.setTint(it.getColor(R.color.colorOnPrimary))

            // Set overflow icon color
            toolbar.overflowIcon?.setTint(it.getColor(R.color.colorOnPrimary))
        }

        // TODO: once category tabs have been implemented, connect to toolbar title here
        toolbar.title = "Category 1"

        return view
    }

    override fun onPause() {
        super.onPause()
        FileHandler.writeDirEntry(requireContext(), ROOT, DirEntry(DataProvider.inodes))
    }

    private fun setSelectedItem(position: Int) {
        listView?.setItemChecked(position, true)
        selectedItemPosition = position
    }

    override fun onItemClick(adapterView: AdapterView<*>, item: View, position: Int, rowId: Long) {
        if (listView?.choiceMode == ListView.CHOICE_MODE_SINGLE) {
            startNoteFragment(position)
        } else {
            setSelectedItem(position)
        }
    }

    private fun startNoteFragment(position: Int) {
        setSelectedItem(position)

        arrayAdapter?.getItem(position)?.let { inode ->
            var note = FileHandler.loadNote(requireContext(), "", "/n" + inode.id)
            if (note == null)
                note = Note(inode.id)

            if (ScreenHelper.isDualMode(requireActivity())) {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.dual_screen_end_container_id,
                        NoteDetailFragment.newInstance(inode, note),
                        null
                    ).commit()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.single_screen_container_id,
                        NoteDetailFragment.newInstance(inode, note),
                        null
                    ).addToBackStack(null)
                    .commit()
            }
        }
    }

    fun updateINode(inode: INode, title: String) {
        inode.title = title
        inode.dateModified = LocalDateTime.now()

        FileHandler.writeDirEntry(requireContext(), ROOT, DirEntry(DataProvider.inodes))
    }

    /**
     * Sort list of notes based on date modified (most recently edited at top)
     *
     * REVISIT: when should it be sorted? (ex: in dual-screen view, not done editing,
     * should it be at original position or pop to top as soon as editing begins?)
     * Should also adjust the listView's "selectedItemPosition" highlight feature (and the
     * index of the attribute "selectedItemPosition")
     */
    private fun sortArray() {
        arrayAdapter?.sort { one, two -> two.dateModified.compareTo(one.dateModified) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select -> {
                // TODO: toolbar doesn't show up at first, add selection bubbles/visual indication
                listView?.let {
                    it.clearChoices()
                    it.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, item: View, position: Int, rowId: Long): Boolean {
        listView?.let {
            it.clearChoices()
            it.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        }
        setSelectedItem(position)
        return true
    }

    fun updateArrayAdapter() {
        arrayAdapter?.notifyDataSetChanged()
    }
}
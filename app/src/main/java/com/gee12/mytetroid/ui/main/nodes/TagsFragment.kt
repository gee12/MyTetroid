package com.gee12.mytetroid.ui.main.nodes

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.SortHelper
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.ui.base.TetroidFragment
import com.gee12.mytetroid.ui.dialogs.tag.TagFieldsDialog
import com.gee12.mytetroid.ui.main.MainViewModel
import com.gee12.mytetroid.ui.tag.TagsListAdapter

class TagsFragment : TetroidFragment<MainViewModel> {

    private lateinit var lvTags: ListView
    private lateinit var listAdapterTags: TagsListAdapter
    private lateinit var tvTagsEmpty: TextView
    private lateinit var btnLoadStorageTags: Button

    constructor(viewModel: MainViewModel) : super() {
        this.viewModel = viewModel
    }

    constructor() : super() {}

    override fun getLayoutResourceId() = R.layout.fragment_tags

    override fun getViewModelClazz() = MainViewModel::class.java

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun createViewModel() {}


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container)

        lvTags = view.findViewById(R.id.tags_list_view)
        lvTags.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            showTagRecords(
                position
            )
        }
        lvTags.onItemLongClickListener = AdapterView.OnItemLongClickListener { _: AdapterView<*>, view: View, position: Int, _: Long ->
            val tagEntry = listAdapterTags.getItem(position)
            if (tagEntry != null) {
                showTagPopupMenu(view, tagEntry.value)
            }
            true
        }
        tvTagsEmpty = view.findViewById(R.id.tags_text_view_empty)
        lvTags.emptyView = tvTagsEmpty
        val tagsFooter = (requireActivity().getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.list_view_empty_footer, null, false)
        lvTags.addFooterView(tagsFooter, null, false)

        btnLoadStorageTags = view.findViewById(R.id.button_load_2)
        btnLoadStorageTags.visibility = View.GONE
        if (viewModel.buildInfoProvider.isFullVersion()) {
            val listener = View.OnClickListener {
                viewModel.loadAllNodes(false)
            }
            btnLoadStorageTags.setOnClickListener(listener)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createListAdapter()
    }

    fun createListAdapter() {
        listAdapterTags = TagsListAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
        )
        lvTags.adapter = listAdapterTags
    }

    fun resetListAdapter() {
        listAdapterTags.reset()
    }

    fun setTagsDataItems(tags: Map<String, TetroidTag>) {
        listAdapterTags.setDataItems(
            tags,
            SortHelper(
                CommonSettings.getTagsSortMode(requireContext(), SortHelper.byNameAsc())
            )
        )
    }

    fun updateTags() {
        setTagsDataItems(viewModel.getTagsMap())
    }

    private fun showTagRecords(position: Int) {
        val tag = listAdapterTags.getItem(position).value
        viewModel.showTag(tag)
    }

    /**
     * Переименование метки в записях.
     */
    private fun renameTag(tag: TetroidTag) {
        TagFieldsDialog(
            tag = tag
        ) { name: String ->
            viewModel.renameTag(tag, name)
        }
            .showIfPossible(parentFragmentManager)
    }

    /**
     * Копирование ссылки на метку в буфер обмена.
     */
    private fun copyTagLink(tag: TetroidTag?) {
        if (tag != null) {
            val url = tag.createUrl()
            Utils.writeToClipboard(requireContext(), getString(R.string.link_to_tag), url)
            viewModel.log(getString(R.string.title_link_was_copied) + url, true)
        } else {
            viewModel.log(getString(R.string.log_get_item_is_null), true)
        }
    }

    fun setTagsEmptyText(resId: Int) {
        setTagsEmptyText(getString(resId))
    }

    fun setTagsEmptyText(text: String) {
        tvTagsEmpty.text = text
    }

    fun setListEmptyViewState(isVisible: Boolean, @StringRes stringId: Int) {
        setListEmptyViewState(isVisible, getString(stringId))
    }

    fun setListEmptyViewState(isVisible: Boolean, string: String) {
        tvTagsEmpty.isVisible = isVisible
        tvTagsEmpty.text = string
    }

    fun setVisibleLoadStorageTags(isVisible: Boolean) {
        btnLoadStorageTags.isVisible = isVisible
    }

    /**
     * Отображение всплывающего (контексного) меню метки.
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     */
    @SuppressLint("RestrictedApi", "NonConstantResourceId")
    private fun showTagPopupMenu(v: View, tag: TetroidTag) {
        val popupMenu = PopupMenu(requireContext(), v)
        popupMenu.inflate(R.menu.tag_context)
        val menu = popupMenu.menu
//        visibleMenuItem(menu.findItem(R.id.action_rename), viewModel.buildInfoProvider.isFullVersion())
        menu.findItem(R.id.action_rename)?.isVisible = viewModel.buildInfoProvider.isFullVersion()
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open_tag -> {
                    viewModel.showTag(tag)
                    return@setOnMenuItemClickListener true
                }
                R.id.action_rename -> {
                    renameTag(tag)
                    return@setOnMenuItemClickListener true
                }
                R.id.action_copy_link -> {
                    copyTagLink(tag)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        setForceShowMenuIcons(v, popupMenu.menu as MenuBuilder)
    }

    @SuppressLint("NonConstantResourceId")
    fun showTagsSortPopupMenu(v: View) {
        val context = requireContext()
        val popupMenu = PopupMenu(context, v)
        popupMenu.inflate(R.menu.tags_sort)

        // выделяем цветом текущую сортировку
        val tagsSortMode = SortHelper(CommonSettings.getTagsSortMode(context, SortHelper.byNameAsc()))
        val isByName = tagsSortMode.isByName
        val isAscent = tagsSortMode.isAscent
        val menuItem = when {
            isByName && isAscent -> {
                popupMenu.menu.findItem(R.id.action_sort_tags_name_asc)
            }
            isByName && !isAscent -> {
                popupMenu.menu.findItem(R.id.action_sort_tags_name_desc)
            }
            !isByName && isAscent -> {
                popupMenu.menu.findItem(R.id.action_sort_tags_count_asc)
            }
            !isByName && !isAscent -> {
                popupMenu.menu.findItem(R.id.action_sort_tags_count_desc)
            }
            else -> null
        }
        menuItem?.let {
            val title = SpannableString(it.title)
            val textColor = ContextCompat.getColor(context, R.color.colorLightText)
            title.setSpan(ForegroundColorSpan(textColor), 0, title.length, 0)
            it.setTitle(title)
        }

        // выводим меню
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_sort_tags_name_asc -> {
                    listAdapterTags.sort(true, true)
                    CommonSettings.setTagsSortMode(context, SortHelper.byNameAsc())
                    true
                }
                R.id.action_sort_tags_name_desc -> {
                    listAdapterTags.sort(true, false)
                    CommonSettings.setTagsSortMode(context, SortHelper.byNameDesc())
                    true
                }
                R.id.action_sort_tags_count_asc -> {
                    listAdapterTags.sort(false, true)
                    CommonSettings.setTagsSortMode(context, SortHelper.byCountAsc())
                    true
                }
                R.id.action_sort_tags_count_desc -> {
                    listAdapterTags.sort(false, false)
                    CommonSettings.setTagsSortMode(context, SortHelper.byCountDesc())
                    true
                }
                else -> false
            }
        }
        setForceShowMenuIcons(v, popupMenu.menu as MenuBuilder)
    }

}
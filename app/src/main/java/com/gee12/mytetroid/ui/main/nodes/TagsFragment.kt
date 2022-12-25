package com.gee12.mytetroid.ui.main.nodes

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.SortHelper
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.model.enums.TagsSearchMode
import com.gee12.mytetroid.ui.base.TetroidFragment
import com.gee12.mytetroid.ui.dialogs.tag.TagFieldsDialog
import com.gee12.mytetroid.ui.main.MainViewModel
import com.gee12.mytetroid.ui.tag.SelectedTagsListAdapter
import com.gee12.mytetroid.ui.tag.TagsListAdapter

class TagsFragment : TetroidFragment<MainViewModel> {

    private lateinit var lvTags: ListView
    private lateinit var adapterTags: TagsListAdapter
    private lateinit var tvTagsEmpty: TextView
    private lateinit var btnLoadStorageTags: Button
    private lateinit var layoutSelectedTags: View
    private lateinit var lvSelectedTags: ListView
    private lateinit var adapterSelectedTags: SelectedTagsListAdapter
    private lateinit var btnApplySelectedTags: Button

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

        layoutSelectedTags = view.findViewById(R.id.layout_selected_tags) as ConstraintLayout
        lvSelectedTags = view.findViewById(R.id.selected_tags_list_view)

        btnApplySelectedTags  = view.findViewById<Button>(R.id.button_apply_selected_tags)
        btnApplySelectedTags.setOnClickListener {
            selectRecordsBySearchMode()
        }
        updateTagsSearchModeTitle(commonSettingsProvider.getTagsSearchMode())
        val btnChangeTagsSearchMode  = view.findViewById<AppCompatImageButton>(R.id.button_change_tags_search_mode)
        btnChangeTagsSearchMode.setOnClickListener { view ->
            showSelectTagsSearchModePopupMenu(view)
        }
        val btnCancelSelectedTags  = view.findViewById<AppCompatImageButton>(R.id.button_cancel_selected_tags)
        btnCancelSelectedTags.setOnClickListener {
            viewModel.unselectAllTags()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createListAdapters()
    }

    private fun createListAdapters() {
        adapterTags = TagsListAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
            checkIsSelected = { tag ->
                viewModel.isTagSelected(tag)
            },
            onClick = { _, tag ->
                viewModel.onTagClick(tag)
            },
            onLongClick = { view, tag ->
                showTagPopupMenu(view, tag)
            }
        )
        lvTags.adapter = adapterTags

        adapterSelectedTags = SelectedTagsListAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
            onCancelTag = { tag ->
                viewModel.unselectTag(tag)
            }
        )
        lvSelectedTags.adapter = adapterSelectedTags
    }

    fun resetListAdapters() {
        adapterTags.reset()
        adapterSelectedTags.reset()
    }

    fun setTagsDataItems(tags: Map<String, TetroidTag>) {
        adapterTags.setDataItems(
            data = tags,
            sortHelper = SortHelper(commonSettingsProvider.getTagsSortOrder())
        )
    }

    fun updateTags() {
        adapterTags.notifyDataSetChanged()
        adapterSelectedTags.notifyDataSetChanged()
    }

    private fun selectRecordsBySearchMode() {
        setTagsSearchMode(searchMode = commonSettingsProvider.getTagsSearchMode())
        viewModel.showTagsRecordsFromSelected()
    }

    private fun setTagsSearchMode(searchMode: TagsSearchMode) {
        updateTagsSearchModeTitle(searchMode)
        commonSettingsProvider.setTagsSearchMode(searchMode)
    }

    private fun updateTagsSearchModeTitle(searchMode: TagsSearchMode) {
        val stringModeString = searchMode.getStringValue(resourcesProvider)
        btnApplySelectedTags.text = "${resourcesProvider.getString(R.string.action_search)} ($stringModeString)"
    }

    fun updateSelectedTags(selectedTags: List<TetroidTag>, isMultiTagsMode: Boolean) {
        if (isMultiTagsMode) {
            adapterSelectedTags.setDataItems(selectedTags)
        } else {
            adapterSelectedTags.reset()
        }
        layoutSelectedTags.isVisible = isMultiTagsMode

        adapterTags.notifyDataSetChanged()
    }

    /**
     * Переименование метки в записях.
     */
    private fun renameTag(tag: TetroidTag) {
        TagFieldsDialog(
            tag = tag,
            onApply = { name ->
                viewModel.renameTag(tag, name)
            }
        ).showIfPossible(parentFragmentManager)
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
        val isFullVersion = viewModel.buildInfoProvider.isFullVersion()
        menu.findItem(R.id.action_rename)?.isVisible = isFullVersion
        menu.findItem(R.id.action_select)?.isVisible = isFullVersion
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open_tag -> {
                    viewModel.showTagRecords(tag)
                    true
                }
                R.id.action_rename -> {
                    renameTag(tag)
                    true
                }
                R.id.action_copy_link -> {
                    copyTagLink(tag)
                    true
                }
                R.id.action_select -> {
                    viewModel.selectTag(tag)
                    true
                }
                else -> false
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
        val tagsSortMode = SortHelper(commonSettingsProvider.getTagsSortOrder())
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
                    adapterTags.sort(byName = true, isAscent = true)
                    commonSettingsProvider.setTagsSortOrder(SortHelper.byNameAsc())
                    true
                }
                R.id.action_sort_tags_name_desc -> {
                    adapterTags.sort(byName = true, isAscent = false)
                    commonSettingsProvider.setTagsSortOrder(SortHelper.byNameDesc())
                    true
                }
                R.id.action_sort_tags_count_asc -> {
                    adapterTags.sort(byName = false, isAscent = true)
                    commonSettingsProvider.setTagsSortOrder(SortHelper.byCountAsc())
                    true
                }
                R.id.action_sort_tags_count_desc -> {
                    adapterTags.sort(byName = false, isAscent = false)
                    commonSettingsProvider.setTagsSortOrder(SortHelper.byCountDesc())
                    true
                }
                else -> false
            }
        }
        setForceShowMenuIcons(v, popupMenu.menu as MenuBuilder)
    }

    private fun showSelectTagsSearchModePopupMenu(v: View) {
        val context = requireContext()
        val popupMenu = PopupMenu(context, v)
        popupMenu.inflate(R.menu.tags_search_mode)

        // выводим меню
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_tags_search_mode_or -> {
                    setTagsSearchMode(TagsSearchMode.OR)
                    true
                }
                R.id.action_tags_search_mode_and -> {
                    setTagsSearchMode(TagsSearchMode.AND)
                    true
                }
                else -> false
            }
        }
        setForceShowMenuIcons(v, popupMenu.menu as MenuBuilder)
    }

}
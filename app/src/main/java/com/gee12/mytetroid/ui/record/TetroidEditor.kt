package com.gee12.mytetroid.ui.record

import android.content.Context
import android.util.AttributeSet
import com.gee12.htmlwysiwygeditor.ActionButton
import com.gee12.htmlwysiwygeditor.enums.ActionButtonSize
import com.gee12.htmlwysiwygeditor.enums.ActionType
import com.gee12.htmlwysiwygeditor.dialog.ImageDimensDialog
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.HtmlHelper
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.ui.dialogs.AskDialogs.showYesNoDialog
import com.gee12.htmlwysiwygeditor.WysiwygEditor
import com.gee12.mytetroid.domain.AppThemeHelper.setNightMode
import com.gee12.mytetroid.domain.provider.BuildInfoProvider

class TetroidEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WysiwygEditor(context, attrs, defStyle) {

    private lateinit var settingsManager: CommonSettingsManager
    private lateinit var buildInfoProvider: BuildInfoProvider

    interface IEditorListener {
        fun onIsEditedChanged(isEdited: Boolean)
    }

    var isCalledHtmlRequest = false
    protected var editorListener: IEditorListener? = null
        private set

    override var isEdited: Boolean
        get() = super.isEdited
        set(value) {
            super.isEdited = value
            editorListener?.onIsEditedChanged(isEdited)
        }

    override val toolbarButtonsSize: ActionButtonSize
        get() = settingsManager.getEditorButtonsSize()

    fun init(settingsManager: CommonSettingsManager, buildInfoProvider: BuildInfoProvider) {
        this.settingsManager = settingsManager
        this.buildInfoProvider = buildInfoProvider
        onSettingsChanged()
    }

    fun onSettingsChanged() {
        initToolbar()

        settingsManager.getAppTheme().also { appTheme ->
            settingsManager.getEditorTheme().also { editorTheme ->
                webView.setNightMode(appTheme, editorTheme)
            }
        }
    }

//    override val actionButtons: List<ActionButton>
//        get() {
//            val buttons: MutableList<ActionButton> = ArrayList()
//            for (i in 0 until toolbarActions.childCount) {
//                val view = toolbarActions.getChildAt(i)
//                if (view is ActionButton) {
//                    buttons.add(view)
//                }
//            }
//            return buttons
//        }

    /**
     * Отображение команд панели инструментов исходя из настроек приложения.
     */

    override fun initToolbar() {
        super.initToolbar()

        // TODO: реализовать хранение параметров команд в базе данных
//        List<EditorAction> actions = new ArrayList<>();//Database.getEditorActions();
//
//        this.actionButtons = new HashMap<>();
//        for (int i = 0; i < layoutButtons.getChildCount(); i++) {
//            ActionButton button = (ActionButton) layoutButtons.getChildAt(i);
//            initActionButton(button);
//            EditorAction dbAction = actions.get(i);
//            if (dbAction == null)
//                continue;
//            // отображение
//            if (!dbAction.isEnabled()) {
//                button.setVisibility(GONE);
////                layoutButtons.removeView(button);
//            } else {
//                // сортировка
//                if (button.getType() != dbAction.getType()) {
//                    layoutButtons.removeView(button);
//                    layoutButtons.addView(button, dbAction.getOrder());
//                }
//            }
//        }
    }

    override fun setupActionButton(
        button: ActionButton,
        actionType: ActionType,
        isEditable: Boolean,
        isCheckable: Boolean,
        isPopup: Boolean,
        isAction: Boolean,
        onClick: (() -> Unit)?,
    ) {
        super.setupActionButton(button, actionType, isEditable, isCheckable, isPopup, isAction, onClick)
        val proActions = arrayOf(
            ActionType.INSERT_VIDEO,
            ActionType.VOICE_INPUT
        )
        val notAvailableYetActions = arrayOf(
            ActionType.INSERT_FORMULA,
            ActionType.EDIT_IMAGE,
            // table
            ActionType.INSERT_TABLE,
            ActionType.EDIT_TABLE,
            ActionType.INSERT_TABLE_ROWS,
            ActionType.DELETE_TABLE_ROW,
            ActionType.INSERT_TABLE_COLS,
            ActionType.DELETE_TABLE_COL,
            ActionType.MERGE_TABLE_CELLS,
            ActionType.SPLIT_TABLE_CELLS,
        )
        if ((buildInfoProvider.isFreeVersion() && actionType in proActions)
            || actionType in notAvailableYetActions
        ) {
            button.setIsAllowed(false)
        }
    }

    override fun onClickActionButton(button: ActionButton) {
        if (!button.isAllowed) {
            when (button.type) {
                ActionType.INSERT_FORMULA -> showToastNotAvailableYet()
                ActionType.VOICE_INPUT -> showToastNotAvailableInFree()
                ActionType.EDIT_IMAGE,
                // table
                ActionType.INSERT_TABLE,
                ActionType.EDIT_TABLE,
                ActionType.INSERT_TABLE_ROWS,
                ActionType.DELETE_TABLE_ROW,
                ActionType.INSERT_TABLE_COLS,
                ActionType.DELETE_TABLE_COL,
                ActionType.MERGE_TABLE_CELLS,
                ActionType.SPLIT_TABLE_CELLS -> {
                    showToastNotAvailableYet()
                }
                else -> Unit
            }
        } else {
            super.onClickActionButton(button)
        }
    }

        /**
     * Вызывается перед сохранением текста записи в файл.
     */
    fun beforeSaveAsync(deleteStyleEmpty: Boolean) {
        if (deleteStyleEmpty) {
            deleteStyleEmpty()
        }
        isCalledHtmlRequest = true
        webView.makeEditableHtmlRequest()
    }

    /**
     * Специально для совместимости с MyTetroid:
     *
     * удаление стиля "-qt-paragraph-type:empty;" в абзацах "p", содержащих
     * какой-то текст, а не просто элемент "br".
     */
    private fun deleteStyleEmpty() {
        val script = """
            |var paragraphs = document.getElementsByTagName("p");
            |for(var i = 0; i < paragraphs.length; i++) {
            |    var elem = paragraphs[i];
            |    if (elem.hasAttribute("style")) {
            |        var styleAttr = elem.getAttribute("style");
            |        if (!(elem.children.length == 1 && elem.children[0].tagName == "BR")
            |            && styleAttr.indexOf("-qt-paragraph-type:empty;") > -1) {
            |            var newStyleAttr = styleAttr.replace("-qt-paragraph-type:empty;","");
            |            elem.setAttribute("style", newStyleAttr);
            |        }
            |    }
            |}
            """.trimMargin()
        webView.execJavascript(script, true)
    }

    fun insertImage(image: TetroidImage) {
        showEditImageDialog(image.name, image.width, image.height, false)
    }

    /**
     * Вставка выбранных изображений.
     * @param images
     */
    fun insertImages(images: List<TetroidImage>) {
        when (images.size) {
            0 -> {
                return
            }
            1 -> {
                // выводим диалог установки размера
                insertImage(images[0])
            }
            else -> {
                // спрашиваем о необходимости изменения размера
                showYesNoDialog(
                    context = context,
                    isCancelable = true,
                    messageResId = R.string.ask_change_image_dimens,
                    onApply = {
                        createImageDimensDialog(images, 0)
                    },
                    onCancel = {
                        for (fileName in images) {
                            webView.insertImage(url = fileName.name, alt = null)
                        }
                    }
                )
                setIsEdited()
            }
        }
    }

    private fun createImageDimensDialog(images: List<TetroidImage>, pos: Int) {
        if (pos < 0 || pos >= images.size) {
            return
        }
        val image = images[pos]
        // выводим диалог установки размера
        val isSeveral = pos < images.size - 1
        ImageDimensDialog(
            context = context,
            srcWidth = image.width,
            srcHeight = image.height,
            isSeveral = isSeveral,
            onApply = { width, height, similar ->
                webView.insertImage(image.name, width, height)
                if (!similar) {
                    // вновь выводим диалог установки размера
                    createImageDimensDialog(images, pos + 1)
                } else {
                    // устанавливаем "сохраненный" размер
                    for (i in pos + 1 until images.size) {
                        webView.insertImage(images[i].name, width, height)
                    }
                }
            }
        ).show()
    }

    private fun showToastNotAvailableInFree() {
        showToast(R.string.title_available_in_pro)
    }

    private fun showToastNotAvailableYet() {
        showToast(R.string.title_not_available_yet)
    }

    val documentHtml: String
        get() = getDocumentHtml(webView.editableHtml)

    fun setHtmlRequestHandled() {
        isCalledHtmlRequest = false
    }

    fun setEditorListener(listener: IEditorListener) {
        editorListener = listener
    }

    companion object {

        fun getDocumentHtml(bodyHtml: String?): String {
            return buildString(capacity = 3) {
                append(HtmlHelper.HTML_START_WITH)
                bodyHtml?.let { append(it) }
                append(HtmlHelper.HTML_END_WITH)
            }
        }

    }

}
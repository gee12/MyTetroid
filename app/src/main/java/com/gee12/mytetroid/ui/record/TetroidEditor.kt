package com.gee12.mytetroid.ui.record

import android.content.Context
import android.util.AttributeSet
import com.gee12.htmlwysiwygeditor.ActionButton
import com.gee12.htmlwysiwygeditor.ActionButtonSize
import com.gee12.htmlwysiwygeditor.ActionType
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.App.isFreeVersion
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.HtmlHelper
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.ui.dialogs.AskDialogs.showYesNoDialog
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor

class TetroidEditor : WysiwygEditor {

    private var settingsManager: CommonSettingsManager? = null

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
        get() = settingsManager?.getEditorButtonsSize() ?: ActionButtonSize.MEDIUM

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    protected fun init() {}

    fun init(settingsManager: CommonSettingsManager) {
        this.settingsManager = settingsManager
        onSettingsChanged()
    }

    fun onSettingsChanged() {
        initToolbar()
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
    /*    @Override
    protected void initToolbarActions() {
        super.initToolbarActions();

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
    }*/

    override fun initActionButton(
        button: ActionButton,
        type: ActionType,
        isCheckable: Boolean,
        isPopup: Boolean
    ) {
        super.initActionButton(button, type, isCheckable, isPopup)
        if ((isFreeVersion() // TODO: в качестве исключения
                    || type !== ActionType.INSERT_VIDEO)
            && !type.isFree
        ) {
            button.isEnabled = false
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
        webView.execJavascript(script)
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
                            webView.insertImage(fileName.name, null)
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
        Dialogs.createImageDimensDialog(
            context, image.width, image.height, isSeveral
        ) { width: Int, height: Int, similar: Boolean ->
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
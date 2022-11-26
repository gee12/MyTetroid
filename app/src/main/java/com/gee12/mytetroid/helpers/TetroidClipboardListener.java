package com.gee12.mytetroid.helpers;

import android.net.Uri;

import com.gee12.mytetroid.ui.activities.RecordActivity;
import com.gee12.mytetroid.ui.dialogs.ClipboardDialogs;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.common.utils.UriUtils;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;

/**
 * Вспомогательный класс для получения данных из буфера обмена и вывода списка вариантов их обработки
 *  пользователю.
 */
public class TetroidClipboardListener implements EditableWebView.IClipboardListener {

    private RecordActivity mActivity;

    public TetroidClipboardListener(RecordActivity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onReceiveSelectedText(String text, String html) {
        ClipboardHelper.writeToClipboard(mActivity, "", text, html);
    }

    @Override
    public void pasteClipboardContent(boolean textOnly) {
        // вызываем в главном потоке, т.к. метод может вызываться из потока Javascript у WebView
        mActivity.runOnUiThread(() -> {
            ClipboardHelper.readFromClipboard(mActivity, textOnly, new ClipboardHelper.IClipboardResult() {

                @Override
                public void pasteText(String text) {
                    mActivity.getEditor().getWebView().insertTextWithPreparation(text);
                }

                @Override
                public void pasteHtml(String html) {
                    mActivity.getEditor().getWebView().insertHtmlWithPreparation(html);
                }

                @Override
                public void pasteImage(Uri uri, boolean isLocal) {
                    //  * вставить изображение (если isLocal)
                    //      / скачать и вставить изображение (если не isLocal)
                    //  * прикрепить изображение (если isLocal)
                    //      / скачать и прикрепить изображение (если не isLocal)
                    //  * вставить гиперссылку на изображение
                    //  * вставить URL изображение как текст
                    ClipboardDialogs.createImageDialog(mActivity, uri, isLocal,
                            new ClipboardDialogs.IDialogImageURLResult() {
                                @Override
                                public void insertLocalImage(Uri uri) {
                                    mActivity.saveImage(uri, false);
                                }

                                @Override
                                public void downloadAndInsertWebImage(Uri uri) {
                                    mActivity.downloadImage(uri.toString());
                                }

                                @Override
                                public void attachLocalFile(Uri uri) {
                                    mActivity.attachFile(uri, false);
                                }

                                @Override
                                public void downloadAndAttachWebFile(Uri uri) {
                                    mActivity.downloadAndAttachFile(uri);
                                }

                                @Override
                                public void insertHyperlink(Uri uri) {
                                    hyperlink(uri);
                                }

                                @Override
                                public void insertAsText(Uri uri) {
                                    asText(uri);
                                }
                            });
                }

                @Override
                public void pasteAudio(Uri uri, boolean isLocal) {
                    // Спросить у юзера в диалоге:
                    //  * TODO: вставить аудио (как проигрывать ?)
                    //  * прикрепить аудиоофайл (если isLocal)
                    //      / скачать и прикрепить аудиоофайл (если не isLocal)
                    //  * вставить гиперссылку на аудиоофайл
                    //  * вставить URL аудиоофайла как текст
                    onPasteFile(uri, isLocal);
                }

                @Override
                public void pasteVideo(Uri uri, boolean isLocal) {
                    // Спросить у юзера в диалоге:
                    //  * TODO: вставить Youtube-видео, если это Youtube URL
                    //  * прикрепить видеофайл (если isLocal)
                    //      / скачать и прикрепить видеофайл (если не isLocal)
                    //  * вставить гиперссылку на видеофайл
                    //  * вставить URL видеофайла как текст
//                ClipboardDialogs.createSimpleURLDialog(mActivity, uri,
//                        new ClipboardDialogs.IDialogResult() {
//                            @Override
//                            public void insertHyperlink(Uri uri) {
//                                hyperlink(uri);
//                            }
//                            @Override
//                            public void insertAsText(Uri uri) {
//                                asText(uri);
//                            }
//                        });
                    onPasteFile(uri, isLocal);
                }

                @Override
                public void pasteFile(Uri uri, boolean isLocal) {
                    //  * прикрепить файл (если isLocal)
                    //      / скачать и прикрепить файл (если не isLocal)
                    //  * (?) прикрепить файл (/) и вставить ссылку
                    //  * вставить гиперссылку на файл
                    //  * вставить URL файла как текст
                    onPasteFile(uri, isLocal);
                }

                @Override
                public void pasteUri(Uri uri/*, boolean isLocal*/) {
                    // Спросить у юзера в диалоге:
                    //  * скачать и вставить содержимое web-страницы (если не isLocal)
                    //  * скачать и прикрепить web-страницу как html-файл (если не isLocal)
                    //  * скачать и вставить содержимое web-страницы как текст (если не isLocal)
                    //  * ---вставить гиперссылку на файл (если isLocal)
                    //      / вставить гиперссылку на web-страницу (если не isLocal)
                    //  * ---вставить URL объекта как текст (если isLocal)
                    //      / вставить URL web-страницы как текст (если не isLocal)
                    ClipboardDialogs.createURLDialog(mActivity, uri,
                            new ClipboardDialogs.IDialogURLResult() {
                                @Override
                                public void downloadAndInsertWebPage(Uri uri) {
                                    mActivity.downloadWebPageContent(uri.toString(), false);
                                }

                                @Override
                                public void downloadAndInsertWebPageAsText(Uri uri) {
                                    mActivity.downloadWebPageContent(uri.toString(), true);
                                }

                                @Override
                                public void downloadAndAttachWebPage(Uri uri) {
                                    mActivity.downloadAndAttachFile(uri);
                                }

                                @Override
                                public void insertHyperlink(Uri uri) {
                                    hyperlink(uri);
                                }

                                @Override
                                public void insertAsText(Uri uri) {
                                    asText(uri);
                                }
                            });
                }

                @Override
                public void pasteTetroidObject(Uri uri, TetroidObject obj) {
                    // Спросить у юзера в диалоге:
                    //  * TODO: прикрепить, если это файл
                    //  * TODO: вставить содержимое, если это ссылка на запись
                    //  * вставить гиперссылку на объект
                    //  * вставить URL объекта как текст
                    ClipboardDialogs.createTetroidObjectURLDialog(mActivity, uri, obj,
                            new ClipboardDialogs.IDialogTetroidObjectURLResult() {
                                @Override
                                public void insertRecordContent(Uri uri, TetroidObject record) {
                                    // TODO:
                                }

                                @Override
                                public void attachFile(Uri uri, TetroidObject attach) {
                                    // TODO:
                                }

                                @Override
                                public void insertHyperlink(Uri uri) {
                                    hyperlink(uri);
                                }

                                @Override
                                public void insertAsText(Uri uri) {
                                    asText(uri);
                                }
                            });
                }
            });
        });
    }

    private void onPasteFile(Uri uri, boolean isLocal) {
        ClipboardDialogs.createFileDialog(mActivity, uri, isLocal,
                new ClipboardDialogs.IDialogFileURLResult() {
                    @Override
                    public void attachLocalFile(Uri uri) {
                        mActivity.attachFile(uri, false);
                    }
                    @Override
                    public void downloadAndAttachWebFile(Uri uri) {
                        mActivity.downloadAndAttachFile(uri);
                    }
                    @Override
                    public void insertHyperlink(Uri uri) {
                        hyperlink(uri);
                    }
                    @Override
                    public void insertAsText(Uri uri) {
                        asText(uri);
                    }
                });
    }

    private void hyperlink(Uri uri) {
        String title = UriUtils.getFileName(uri);
        mActivity.getEditor().showInsertLinkDialog(uri.toString(), title, true);
    }

    private void asText(Uri uri) {
        mActivity.getEditor().getWebView().insertTextWithPreparation(uri.toString());
    }
}

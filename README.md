# MyTetroid

**MyTetroid** — неофициальная Android версия программы [MyTetra](https://github.com/xintrea/mytetra_dev), полнофункционального кроссплатформенного менеджера заметок, персональная ***база знаний*** на телефоне.

<a href="https://apps.rustore.ru/app/com.gee12.mytetroid" target="_blank" rel="noopener"><img class="alignnone" src="https://gee12.space/wp-content/uploads/2023/06/rustore1.png" alt="Скачать из RuStore" width="200" /></a>
<a href='https://play.google.com/store/apps/details?id=com.gee12.mytetroid' target="_blank"><img alt='Доступно в Google Play' src='https://gee12.space/wp-content/uploads/2024/06/gp_logo.png' width='200'/></a>

Позволяет ***упорядоченно*** хранить информацию в виде заметок, рассортированных по веткам и снабженных тегами.  
В визуальном редакторе можно выполнить любое ***форматирование*** текста, создавать списки, вставлять изображения и прочее.  
Благодаря гибкому глобальному ***поиску*** можно всегда найти нужное.  
Шифрование веток позволяет ***защитить*** важную информацию от раскрытия.  
К записям можно прикрепить любые ***файлы***.  
С помощью виджета на рабочем столе можно легко создавать ***быстрые*** заметки.

Если вам надоело выискивать ранее сохраненную информацию среди ***100500*** разрозненных закладок в браузере, текстовых файлов, разбросанных по всему жесткому диску, заметок в социальных сетях и из других еще менее очевидных источников, то эта программа для вас.

***

**Основные возможности MyTetroid:**
* использование ***существующего*** хранилища Mytetra или создание нового
* древовидная структура веток (шторка слева), списки записей в ветках и прикрепленных файлов к записям, как в оригинальной программе
* быстрое создание записей с помощью ***виджета*** на рабочем столе
* ***шифрование*** и расшифровка закрытых веток, записей и файлов
* отдельный общий список ***меток*** записей (шторка справа)
* глобальный ***поиск*** по всем объектам хранилища
* использование внутренних ***ссылок*** на ветки, записи и метки
* экспорт текста заметок в ***pdf***
* ***полноэкранный режим*** и блокировка отключения экрана при чтении записей
* ***синхронизация*** хранилища с помощью сторонних приложений
* темная тема

***

**Визуальный редактор текста заметок поддерживает:**
* форматирование ***шрифта*** (толщина, курсив, размер, цвет, фон и т.д.)
* форматирование ***абзацев*** (отступ, список, выравнивание и т.д.)
* вставка гиперссылок, ***изображений*** и захват фото с камеры
* работу с ***таблицами***
* команды для работы с выделением и ***буфером обмена***
* редактирование исходного html-кода заметки
* запуск локальных javascript-сценариев (скриптов)

***

**В версии Pro добавлено:**
* использование ***нескольких*** хранилищ
* список ***избранных*** записей
* дата последнего изменения содержимого записи
* запрос ***ПИН-кода*** для расшифровки веток при локальном хранении пароля
* переименование меток (у всех записей, в которых метка добавлена)
* ***голосовой ввод*** текста записей
* использование пользовательских ***Javascript-сценариев*** (скриптов)

***

Хранилище данных реализовано в виде множества ***html-файлов***, структура которых хранится в файле xml, а настройки в файлах ini. Подробнее о формате хранения данных в MyTetra [здесь](https://webhamster.ru/site/page/index/articles/projectcode/184). 

Благодаря простому устройству хранения данных, легко нстроить их синхронизацию через интернет с помощью любых систем облачного хранения или систем контроля версий. О синхронизации данных через интернет подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/170) (а также [здесь](https://gee12.space/sinhronizacija-dannyh-mytetra/) и [здесь](https://gee12.space/sinhronizacija-mytetroid-na-android-git/)).

Для шифрования используется открытая библиотека RC5-Simple, реализующая алгоритм ***RC5-32/12/16*** c CBC-режимом сцепления. Также используется реализация алгоритма хеширования Pbkdf2 для хорошего перемешивания бит пароля и сильного замедления перебора. О системе шифрования в MyTetra подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/530) и [здесь](https://webhamster.ru/site/page/index/articles/projectcode/157).

***

**Использовались сторонние библиотеки:**
* Многоуровневый список веток - [open-rnd/android-multi-level-listview](https://github.com/open-rnd/android-multi-level-listview) (мой [форк](https://github.com/gee12/android-multi-level-listview))
* Работа с файловой системой - [anggrayudi/SimpleStorage](https://github.com/anggrayudi/SimpleStorage) (мой [форк](https://github.com/gee12/SimpleStorage))
* Загрузка xml - [hunterhacker/jdom](https://github.com/hunterhacker/jdom)
* WYSIWYG html-редактор - [lumyjuwon/Android-Rich-WYSIWYG-Editor](https://github.com/lumyjuwon/Android-Rich-WYSIWYG-Editor) (мой [форк](https://github.com/gee12/Android-HTML-WYSIWYG-Editor)), [wasabeef/richeditor-android](https://github.com/wasabeef/richeditor-android)
* Панель ввода ПИН-кода - [aritraroy/PinLockView](https://github.com/aritraroy/PinLockView) (мой [форк](https://github.com/gee12/PinLockView))
* Выбор изображений - [esafirm/android-image-picker](https://github.com/esafirm/android-image-picker) (мой [форк](https://github.com/gee12/android-image-picker))
* Разворачиваемая панель - [cachapa/ExpandableLayout](https://github.com/cachapa/ExpandableLayout)
* Выбор цвета (ColorPicker) - [yukuku/ambilwarna](https://github.com/yukuku/ambilwarna)
* Чтение SVG-картинок - [thinkingcow/svg-android-2](https://github.com/thinkingcow/svg-android-2) (мой [форк](https://github.com/gee12/svg-android-2))
* Парсинг html - [jsoup](https://github.com/jhy/jsoup)
* TextViewUndoRedo - [from google](https://issuetracker.google.com/issues/36913735#c123)

***

Для проверки работы приложения можно использовать [тестовое хранилище](https://github.com/gee12/MyTetraTestData).

***Более подробное описание проекта:*** *https://gee12.space/mytetroid*<br>
***Оригинальный проект MyTetra:*** *https://webhamster.ru/site/page/index/articles/projectcode/105*


Обнаруженные ошибки или пожелания оформляйте в виде [issue](https://github.com/gee12/MyTetroid/issues).


## Лицензия
```
GNU General Public License v3.0

Permissions of this strong copyleft license are conditioned on making available complete source code 
of licensed works and modifications, which include larger works using a licensed work, under the same license. 
Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.
```

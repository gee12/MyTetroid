# MyTetroid

[![Gitter](https://badges.gitter.im/mytetroid/community.svg)](https://gitter.im/mytetroid/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

**MyTetroid** — Android-просмотрщик хранилища данных десктопной версии программы [MyTetra](https://github.com/xintrea/mytetra_dev), полнофункционального кроссплатформенного менеджера заметок (PIM-manager).

<a href='https://play.google.com/store/apps/details?id=com.gee12.mytetroid'><img alt='Доступно в Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/ru_badge_web_generic.png' width='200'/></a>

Хранилище данных реализованно в виде множества html-файлов, структура которого хранится в xml, а настройки в файлах ini. Подробнее о формате хранения данных в MyTetra [здесь](https://webhamster.ru/site/page/index/articles/projectcode/184). 

Благодаря простому устройству хранения данных, легко нстроить их синхронизацию через интернет с помощью любых систем облачного хранения или систем контроля версий. О синхронизации данных через интернет подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/170).

Для шифрования используется открытая библиотека RC5-Simple, реализующая алгоритм RC5-32/12/16 c CBC-режимом сцепления. Также используется реализация алгоритма хеширования Pbkdf2 для хорошего перемешивания бит пароля и сильного замедления перебора. О системе шифрования в MyTetra подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/530) и [здесь](https://webhamster.ru/site/page/index/articles/projectcode/157).

**Возможности MyTetroid:**
* древовидная структура веток, как в оригинальной программе
* список записей в ветках и прикрепленных файлов к записям
* отдельный список меток по записям
* просмотр, изменение, добавление и удаление веток и записей
* расшифровка закрытых веток, записей и файлов
* открытие прикрепленных файлов и каталога записи
* глобальный поиск по всем объектам хранилища
* фильтрация списков веток, записей и меток
* просмотр статистических данных о хранилище
* использование ссылок на ветки, записи и метки
* некоторые опциональные возможности:
   * расшифровка прикрепленных файлов во временный каталог
   * запись логов в файл
   * полноэкранный режим
   * блокировка отключения экрана при просмотре записи
   * отправка команды синхронизации сторонним приложениям (частично)

Для проверки работы приложения можно использовать [тестовое хранилище](https://github.com/gee12/MyTetraTestData).

**Синхронизация хранилища**

Начиная с версии 1.14 возможен вызов стороннего приложения для синхронизации хранилища. Это удобно, например, когда нужно автоматически обновить данные перед их загрузкой, или сохранить изменения при выходе из приложения. Но функционал ПРИЕМА команд синхронизации (запроса) нужно реализовывать в каждом конкретном приложении-синхронизаторе отдельно.

На данный момент прием команд на синхронизацию от MyTetroid поддерживается в:
* git-клиенте [MGit](https://play.google.com/store/apps/details?id=com.manichord.mgit) (пока в [моем форке](https://github.com/gee12/MGit/tree/make-ext-command), но ожидается pull request в основную ветку приложения). Кому нужен скомпиллированный apk с нововведением, пока оно не внесено в официальную версию, его можно скачать [отсюда](https://yadi.sk/d/F7aNnR-7Ta495w).

Планируется поддержка в:
* клиенте [NextCloud](https://play.google.com/store/apps/details?id=com.nextcloud.client) ([github](https://github.com/nextcloud/android))
* с помощью нативного git в [Termux](https://play.google.com/store/apps/details?id=com.termux)

С официальными клиентами облаков, такими как Google Drive, Яндекс.Диск, Dropbox и прочими, ничего не выйдет, т.к. они закрыты (так что при их использовании запускайте синхронизацию "вручную").

**Разрабатывается в данный момент:**
* копирование и перемещение веток и записей
* зашифровка открытых и удаление шифровки (расшифровка) веток
* прикрепление новых файлов
* отправка текста записи в другое приложение
* установка иконок у веток
* добавление картинок, таблиц, формул в текст записи


**Использовались сторонние библиотеки:**
* Многоуровневый список веток - [open-rnd/android-multi-level-listview](https://github.com/open-rnd/android-multi-level-listview)
* Разворачиваемая панель - [cachapa/ExpandableLayout](https://github.com/cachapa/ExpandableLayout)
* Выбор каталога (FolderPicker) - [kashifo/android-folder-picker-library](https://github.com/kashifo/android-folder-picker-library) (мой [форк](https://github.com/gee12/android-folder-picker-library))
* Выбор цвета (ColorPicker) - [yukuku/ambilwarna](https://github.com/yukuku/ambilwarna)
* Чтение SVG-картинок - [thinkingcow/svg-android-2](https://github.com/thinkingcow/svg-android-2) (мой [форк](https://github.com/gee12/svg-android-2))
* Парсинг html - [jsoup](https://github.com/jhy/jsoup)
* WYSIWYG html-редактор - [lumyjuwon/Android-Rich-WYSIWYG-Editor](https://github.com/lumyjuwon/Android-Rich-WYSIWYG-Editor) (мой [форк](https://github.com/gee12/Android-WYSIWYG-Editor)), [wasabeef/richeditor-android](https://github.com/wasabeef/richeditor-android)
* TextViewUndoRedo - [from google](https://issuetracker.google.com/issues/36913735#c123)


***Более подробное описание проекта:*** *https://gee12.space/mytetroid*<br>
***Оригинальный проект MyTetra:*** *https://webhamster.ru/site/page/index/articles/projectcode/105*


## Лицензия
```
GNU General Public License v3.0

Permissions of this strong copyleft license are conditioned on making available complete source code 
of licensed works and modifications, which include larger works using a licensed work, under the same license. 
Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.
```

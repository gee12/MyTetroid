# MyTetroid

[![Gitter](https://badges.gitter.im/mytetroid/community.svg)](https://gitter.im/mytetroid/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

**MyTetroid** — Android-клиент хранилища данных десктопной версии программы [MyTetra](https://github.com/xintrea/mytetra_dev), полнофункционального кроссплатформенного менеджера заметок (PIM-manager).

<a href='https://play.google.com/store/apps/details?id=com.gee12.mytetroid'><img alt='Доступно в Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/ru_badge_web_generic.png' width='200'/></a>

Хранилище данных реализованно в виде множества html-файлов, структура которого хранится в xml, а настройки в файлах ini. Подробнее о формате хранения данных в MyTetra [здесь](https://webhamster.ru/site/page/index/articles/projectcode/184). 

Благодаря простому устройству хранения данных, легко нстроить их синхронизацию через интернет с помощью любых систем облачного хранения или систем контроля версий. О синхронизации данных через интернет подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/170) (а также [здесь](https://gee12.space/sinhronizacija-dannyh-mytetra/) и [здесь](https://gee12.space/sinhronizacija-mytetroid-na-android-git/)).

Для шифрования используется открытая библиотека RC5-Simple, реализующая алгоритм RC5-32/12/16 c CBC-режимом сцепления. Также используется реализация алгоритма хеширования Pbkdf2 для хорошего перемешивания бит пароля и сильного замедления перебора. О системе шифрования в MyTetra подробнее [здесь](https://webhamster.ru/site/page/index/articles/projectcode/530) и [здесь](https://webhamster.ru/site/page/index/articles/projectcode/157).

**Возможности MyTetroid:**
* выбор существующего хранилища или создание нового
* древовидная структура веток, списки записей в ветках и прикрепленных файлов к записям, как в оригинальной программе
* просмотр, добавление, изменение, копирование, вырезание и удаление веток, записей и прикрепленных файлов
* расшифровка закрытых веток, записей и файлов
* сохранение прикрепленных файлов по выбранному пути
* отдельный список меток по записям
* открытие каталога записи (в стороннем файловом менеджере)
* глобальный поиск по всем объектам хранилища
* фильтрация в списках веток, записей и меток
* получение ссылок на ветки, записи и метки
* просмотр статистических данных о хранилище
* некоторые опциональные возможности:
  * установка нового или измение текущего пароля хранилища с перешифровкой данных
  * хранение пароля локально на устройстве или ввод при необходимости
  * расшифровка прикрепленных файлов во временный каталог
  * запуск с открытием ветки, выбранной в прошлый раз
  * запись логов в файл
  * полноэкранный режим
  * блокировка отключения экрана при просмотре записи
  * отправка команды синхронизации сторонним приложениям (частично)
  * отслеживание изменений дерева записей внешней программой (пока в режиме тестирования)
  
**Визуальный редактор текста заметок поддерживает:**
  * форматирование шрифта (толщина, курсив, размер, цвет, фон и т.д.)
  * форматирование абзацев (отступ, список, выравнивание и т.д.)
  * вставка ссылок, изображений и захват фото с камеры
  * команды для работы с выделением и буфером обмена

Для проверки работы приложения можно использовать [тестовое хранилище](https://github.com/gee12/MyTetraTestData).

**Использовались сторонние библиотеки:**
* Многоуровневый список веток - [open-rnd/android-multi-level-listview](https://github.com/open-rnd/android-multi-level-listview) (мой [форк](https://github.com/gee12/android-multi-level-listview))
* Разворачиваемая панель - [cachapa/ExpandableLayout](https://github.com/cachapa/ExpandableLayout)
* Выбор каталога (FolderPicker) - [kashifo/android-folder-picker-library](https://github.com/kashifo/android-folder-picker-library) (мой [форк](https://github.com/gee12/android-folder-picker-library))
* Выбор цвета (ColorPicker) - [yukuku/ambilwarna](https://github.com/yukuku/ambilwarna)
* Чтение SVG-картинок - [thinkingcow/svg-android-2](https://github.com/thinkingcow/svg-android-2) (мой [форк](https://github.com/gee12/svg-android-2))
* Парсинг html - [jsoup](https://github.com/jhy/jsoup)
* WYSIWYG html-редактор - [lumyjuwon/Android-Rich-WYSIWYG-Editor](https://github.com/lumyjuwon/Android-Rich-WYSIWYG-Editor) (мой [форк](https://github.com/gee12/Android-HTML-WYSIWYG-Editor)), [wasabeef/richeditor-android](https://github.com/wasabeef/richeditor-android)
* Панель запроса ПИН-кода - [aritraroy/PinLockView](https://github.com/aritraroy/PinLockView) (мой [форк](https://github.com/gee12/PinLockView))
* Выбор изображений - [esafirm/android-image-picker](https://github.com/esafirm/android-image-picker) (мой [форк](https://github.com/gee12/android-image-picker))
* Загрузка xml - [hunterhacker/jdom](https://github.com/hunterhacker/jdom) (мой [форк](https://github.com/gee12/jdom))
* TextViewUndoRedo - [from google](https://issuetracker.google.com/issues/36913735#c123)


***Более подробное описание проекта:*** *https://gee12.space/mytetroid*<br>
***Оригинальный проект MyTetra:*** *https://webhamster.ru/site/page/index/articles/projectcode/105*


Обнаруженные ошибки или пожелания оформляйте в виде issue.


## Лицензия
```
GNU General Public License v3.0

Permissions of this strong copyleft license are conditioned on making available complete source code 
of licensed works and modifications, which include larger works using a licensed work, under the same license. 
Copyright and license notices must be preserved. Contributors provide an express grant of patent rights.
```

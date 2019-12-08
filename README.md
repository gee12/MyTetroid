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
* ПРОСМОТР содержимого записей
* расшифровка закрытых веток, записей и файлов
* открытие прикрепленных файлов и каталога записи
* глобальный поиск по всем объектам хранилища
* фильтрация списков веток, записей и меток
* просмотр статистических данных о хранилище
* некоторые опциональные возможности:
    * хранение пароля локально на устройстве или спрашивать постоянно
    * выбор подсветки записей с файлами
    * формат отображения даты/времени создания записей
    * использование временного каталога для расшифровки файлов
    * запись логов в файл
    * полноэкранный режим
    * отмена отключения экрана при просмотре записи

**Синхронизация хранилища**

Начиная с версии 1.14 возможен вызов стороннего приложения для синхронизации хранилища. Это удобно, например, когда нужно автоматически обновить данные перед их загрузкой, или сохранить изменения при выходе из приложения. Но функционал ПРИЕМА команд синхронизации (запроса) нужно реализовывать в каждом конкретном приложении-синхронизаторе отдельно.

На данный момент прием команд на синхронизацию от MyTetroid поддерживается в:
* git-клиенте [MGit](https://play.google.com/store/apps/details?id=com.manichord.mgit) (пока в [моем форке](https://github.com/gee12/MGit/tree/make-ext-command), но ожидается pull request в основную ветку приложения). Кому нужен скомпиллированный apk с изменениями, пока они не внесены в официальную версию, пишите в комментариях.

Планируется поддержка в:
* клиенте [NextCloud](https://play.google.com/store/apps/details?id=com.nextcloud.client) ([github](https://github.com/nextcloud/android))
* с помощью нативного git в [Termux](https://play.google.com/store/apps/details?id=com.termux)

С официальными клиентами облаков, такими как Google Drive, Яндекс.Диск, Dropbox и прочими, ничего не выйдет, т.к. они закрыты (так что при их использовании запускайте синхронизацию ручками).

**Планируется реализовать:**
* поиск по тексту записи как в браузере (с навигацией по найденным совпадениям)
* функционал использования сразу нескольких баз и переключения между ними
* ну и конечно же возможность РЕДАКТИРОВАНИЯ записей
* в т.ч. визуальный WYSIWYG редактор

**Возможно будет реализовано:**
* возможность сохранения прикрепленных файлов в новом расположении (Сохранить как..)
* регулируемый перечень отображаемых полей записей в списке (теги, автор, url, дата создания и др.)
* выбор активной в прошлый раз ветки
* список авторов (наподобие списка веток и меток)
* ввод ПИН-кода для доступа к сохраненному хешу пароля как компромисс между вводом пароля каждый раз и использованием его сохраненного кэша (опционально)

Также остается открытым вопрос о синхронизации хранилища с удаленным репозиторием (на данный момент я использую сторонние git-клиенты, например, [MGit](https://play.google.com/store/apps/details?id=com.manichord.mgit)). Можно использовать клиенты для синхронизации с облачными дисками (Яндекс.Диск, Google.Drive и др.).

***Более подробное описание проекта:*** *https://gee12.space/mytetroid*<br>
***Оригинальный проект MyTetra:*** *https://webhamster.ru/site/page/index/articles/projectcode/105*

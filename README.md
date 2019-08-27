# MyTetroid

MyTetroid — Android-просмотрщик хранилища данных десктопной версии программы [MyTetra](https://github.com/xintrea/mytetra_dev), полнофункционального кроссплатформенного менеджера заметок (PIM-manager).

**Возможности MyTetroid:**
* древовидная структура веток, как в оригинальной программе
* список записей в ветках и прикрепленных файлов к записям
* ПРОСМОТР содержимого записей
* расшифровка закрытых веток, записей и файлов
* открытие прикрепленных файлов и каталога записи
* глобальный поиск по всем объектам хранилища
* фильтрация списков веток, записей и меток
* некоторые опциональные возможности:
    * выбор пути к хранилищу данных
    * хранение пароля локально на устройстве или спрашивать постоянно
    * выбор подсветки записей с файлами
    * формат отображения даты/времени создания записей
    * использование временного каталога для расшифровки файлов
    * запись логов в файл

**Необходимо исправить:**
* падение приложения при попытке расшифровки хранилища на некоторых устройствах
* отображение svg-иконок веток на устройствах с API>=23

**Планируется реализовать:**
* поиск по тексту записи как в браузере (с навигацией по найденным совпадениям)
* функционал использования сразу нескольких баз и переключения между ними
* ну и конечно же возможность РЕДАКТИРОВАНИЯ записей
* в т.ч. визуальный редактор

**Возможно будет реализовано:**
* возможность сохранения прикрепленных файлов в новом расположении (Сохранить как..)
* регулируемый перечень отображаемых полей записей в списке (теги, автор, url, дата создания и др.)
* выбор активной в прошлый раз ветки
* список авторов (наподобие списка веток и меток)
* ввод ПИН-кода для доступа к сохраненному хешу пароля как компромисс между вводом пароля каждый раз и использованием его сохраненного кэша (опционально)

Также остается открытым вопрос о синхронизации хранилища с удаленным репозиторием (на данный момент я использую сторонние git-клиенты, например, [MGit](https://play.google.com/store/apps/details?id=com.manichord.mgit)). Можно использовать клиенты для синхронизации с облачными дисками (Яндекс.Диск, Google.Drive и др.).

***Более подробное описание проекта: https://gee12.space/mytetroid***<br>
***Оригинальный проект MyTetra: https://webhamster.ru/site/page/index/articles/projectcode/105***

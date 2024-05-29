package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.domain.repo.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object RepositoriesModule {
    val repositoriesModule = module {

        single {
            CommonSettingsRepo(
                context = androidContext()
            )
        }

        single {
            StoragesRepo(
                context = androidContext()
            )
        }

        single {
            FavoritesRepo(
                context = androidContext()
            )
        }

        single {
            ScriptsDbRepo(
                context = androidContext()
            )
        }

        single {
            ScriptsToObjectsDbRepo(
                context = androidContext()
            )
        }

        single {
            HistoryDbRepo(
                context = androidContext()
            )
        }

    }
}
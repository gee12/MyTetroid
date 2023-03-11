package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.domain.repo.CommonSettingsRepo
import com.gee12.mytetroid.domain.repo.FavoritesRepo
import com.gee12.mytetroid.domain.repo.StoragesRepo
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

    }
}
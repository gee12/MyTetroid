package com.gee12.mytetroid.di

import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope

class ScopeSource: KoinScopeComponent {
    override val scope: Scope by lazy { createScope(this) }

    companion object {
        val current = ScopeSource()
    }

}
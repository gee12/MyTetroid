package com.gee12.mytetroid.common.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import java.net.URL
import java.net.URLConnection
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkHelper {

    fun Context.installGooglePlayServicesRepairIfNeed() {
        if (Build.VERSION.SDK_INT in 17..20) {
            try {
                ProviderInstaller.installIfNeeded(applicationContext)
                val sslContext = SSLContext.getInstance("TLSv1.2")
                sslContext.init(null, null, null)
                sslContext.createSSLEngine()
            } catch (e: GooglePlayServicesRepairableException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }
        }
    }

    fun createURLConnection(url: String, connectTimeout: Int? = null): URLConnection {
        return URL(url).openConnection().also { connection ->
            connectTimeout?.also {
                connection.connectTimeout = it
            }
        }
    }

    fun disableSSLCertificateChecking() {
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOf()
                }
            }
        )
        try {
            //1
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            sc.init(null, trustAllCerts, SecureRandom())

            //2
            //val sc = SSLContext.getInstance("TLSv1")
            //sc.init(null, null, null)

            val noSSLv3Factory = NoSSLv3SocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultSSLSocketFactory(noSSLv3Factory)

            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (ex: KeyManagementException) {
            ex.printStackTrace()
        } catch (ex: NoSuchAlgorithmException) {
            ex.printStackTrace()
        }
    }
}

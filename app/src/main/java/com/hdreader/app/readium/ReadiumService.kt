package com.hdreader.app.readium

import android.content.Context
import android.net.Uri
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.publication.Publication
import java.io.File

/**
 * Thin facade around Readium open/parse. Body rendering stays in EpubNavigatorFragment.
 */
class ReadiumService(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(appContext.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = appContext,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null
        )
    )

    suspend fun openFromUri(uri: Uri): Try<Publication, Exception> {
        return try {
            val absoluteUrl = uri.toAbsoluteUrl()
                ?: return Try.failure(IllegalArgumentException("无法识别的文件地址"))
            open(absoluteUrl)
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    suspend fun openFromFile(file: File): Try<Publication, Exception> {
        return try {
            open(file.toUrl(isDirectory = false))
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun open(url: AbsoluteUrl): Try<Publication, Exception> {
        val assetResult = assetRetriever.retrieve(url)
        val asset = assetResult.getOrElse {
            return Try.failure(Exception(it.message ?: it.toString()))
        }
        val publicationResult = publicationOpener.open(
            asset = asset,
            allowUserInteraction = false
        )
        return publicationResult.mapFailure { Exception(it.message ?: it.toString()) }
    }
}

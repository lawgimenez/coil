package coil3

import coil3.decode.BitmapFactoryDecoder
import coil3.fetch.AssetUriFetcher
import coil3.fetch.BitmapFetcher
import coil3.fetch.ContentUriFetcher
import coil3.fetch.DrawableFetcher
import coil3.fetch.ResourceUriFetcher
import coil3.key.AndroidResourceUriKeyer
import coil3.map.AndroidUriMapper
import coil3.map.ResourceIntMapper
import coil3.map.ResourceUriMapper
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.OneShotDisposable
import coil3.request.lifecycle
import coil3.request.requestManager
import coil3.request.transitionFactory
import coil3.target.Target
import coil3.target.ViewTarget
import coil3.transition.NoneTransition
import coil3.transition.TransitionTarget
import coil3.util.awaitStarted
import kotlinx.coroutines.Deferred

internal actual fun getDisposable(
    request: ImageRequest,
    job: Deferred<ImageResult>,
): Disposable {
    if (request.target is ViewTarget<*>) {
        return request.target.view.requestManager.getDisposable(job)
    } else {
        return OneShotDisposable(job)
    }
}

internal actual suspend fun awaitLifecycleStarted(
    request: ImageRequest,
) {
    request.lifecycle?.awaitStarted()
}

internal actual inline fun transition(
    result: ImageResult,
    target: Target?,
    eventListener: EventListener,
    setDrawable: () -> Unit,
) {
    if (target !is TransitionTarget) {
        setDrawable()
        return
    }

    val transition = result.request.transitionFactory.create(target, result)
    if (transition is NoneTransition) {
        setDrawable()
        return
    }

    eventListener.transitionStart(result.request, transition)
    transition.transition()
    eventListener.transitionEnd(result.request, transition)
}

internal actual fun ComponentRegistry.Builder.addAndroidComponents(
    options: RealImageLoader.Options,
): ComponentRegistry.Builder {
    return this
        // Mappers
        .add(AndroidUriMapper())
        .add(ResourceUriMapper())
        .add(ResourceIntMapper())
        // Keyers
        .add(AndroidResourceUriKeyer())
        // Fetchers
        .add(AssetUriFetcher.Factory())
        .add(ContentUriFetcher.Factory())
        .add(ResourceUriFetcher.Factory())
        .add(DrawableFetcher.Factory())
        .add(BitmapFetcher.Factory())
        // Decoders
        .add(
            BitmapFactoryDecoder.Factory(
                maxParallelism = options.bitmapFactoryMaxParallelism,
                exifOrientationPolicy = options.bitmapFactoryExifOrientationPolicy,
            ),
        )
}
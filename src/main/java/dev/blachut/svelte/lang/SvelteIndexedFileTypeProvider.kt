package dev.blachut.svelte.lang

import com.intellij.lang.javascript.index.IndexedFileTypeProvider
import com.intellij.openapi.fileTypes.FileType

/**
 * Quote from
 * https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000737964-Did-2017-3-break-resolveScopeEnlarger-and-or-indexedRootsProvider-
 *
 * "Alternatively you may add com.intellij.lang.javascript.index.IndexedFileTypeProvider extension,
 * in this case your files will be treated as JS files for scope evaluation, and ResolveScopeEnlargers will be called."
 */
class SvelteIndexedFileTypeProvider : IndexedFileTypeProvider {
    override fun getFileTypesToIndex(): Array<FileType> = arrayOf(SvelteFileType.INSTANCE, SvelteHtmlFileType.INSTANCE)
}

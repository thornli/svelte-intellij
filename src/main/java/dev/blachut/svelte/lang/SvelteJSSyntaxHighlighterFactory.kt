package dev.blachut.svelte.lang

import com.intellij.lang.javascript.dialects.ECMA6LanguageDialect
import com.intellij.lang.javascript.dialects.ECMA6SyntaxHighlighterFactory
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SvelteJSSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return Highlighter()
    }
}

class Highlighter :
    ECMA6SyntaxHighlighterFactory.ECMA6SyntaxHighlighter(ECMA6LanguageDialect.DIALECT_OPTION_HOLDER, false) {
    override fun getMappedKey(original: TextAttributesKey): TextAttributesKey {
        if (JS_GLOBAL_VARIABLE == original) {
            return JS_LOCAL_VARIABLE
        }
        return super.getMappedKey(original)
    }
}

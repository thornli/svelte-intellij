package dev.blachut.svelte.lang.codeInsight;

import com.intellij.lang.ecmascript6.resolve.ES6PsiUtil;
import com.intellij.lang.javascript.ecmascript6.TypeScriptReferenceExpressionResolver;
import com.intellij.lang.javascript.ecmascript6.TypeScriptResolveProcessor;
import com.intellij.lang.javascript.ecmascript6.TypeScriptSignatureChooser;
import com.intellij.lang.javascript.ecmascript6.TypeScriptUtil;
import com.intellij.lang.javascript.ecmascript6.types.JSTypeSignatureChooser;
import com.intellij.lang.javascript.ecmascript6.types.OverloadStrictness;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptModule;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypePredicate;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSSuperExpression;
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl;
import com.intellij.lang.javascript.psi.resolve.JSResolveResult;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.lang.javascript.psi.resolve.JSTypeEvaluator;
import com.intellij.lang.javascript.psi.resolve.QualifiedItemProcessor.TypeResolveState;
import com.intellij.lang.javascript.psi.resolve.ResolveResultSink;
import com.intellij.lang.typescript.TypeScriptResolveHelper;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache.PolyVariantResolver;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A hack for fixing reactive declarations
 */
public class TypeScriptReferenceExpressionResolver2 extends TypeScriptReferenceExpressionResolver implements PolyVariantResolver<JSReferenceExpressionImpl> {
    public TypeScriptReferenceExpressionResolver2(JSReferenceExpressionImpl expression, boolean ignorePerformanceLimits) {
        super(expression, ignorePerformanceLimits);
    }

    @NotNull
    public ResolveResult[] resolve(@NotNull JSReferenceExpressionImpl expression, boolean incompleteCode) {
        ResolveResult[] var10000;
        if (this.myReferencedName != null && !Registry.is("typescript.disable.ide.resolve")) {
            PsiElement currentParent = JSResolveUtil.getTopReferenceParent(this.myParent);
            if (JSResolveUtil.isSelfReference(currentParent, this.myRef)) {
                var10000 = new ResolveResult[]{new JSResolveResult(currentParent)};

                return var10000;
            } else if (currentParent instanceof TypeScriptTypePredicate) {
                var10000 = new ResolveResult[]{new JSResolveResult(((TypeScriptTypePredicate) currentParent).getParameterMatchName())};

                return var10000;
            } else {
                if (!incompleteCode) {
                    JSCallLikeExpression callLikeExpression = TypeScriptSignatureChooser.getCallLikeExpression(expression);
                    if (callLikeExpression != null) {
                        ResolveResult[] results = expression.multiResolve(true);
                        if (this.myQualifier instanceof JSSuperExpression && !TypeScriptUtil.isValidSuperQualifier(this.myQualifier)) {
                            return results;
                        }

                        var10000 = (new JSTypeSignatureChooser(callLikeExpression)).chooseOverload(results, OverloadStrictness.UNIQUE);
                        return var10000;
                    }
                }

                ResolveResult[] resolveResults = this.doResolveReference(incompleteCode);
                if (currentParent instanceof TypeScriptModule) {
                    var10000 = ArrayUtil.mergeArrays(resolveResults, new ResolveResult[]{new JSResolveResult(currentParent)});

                    return var10000;
                } else if (this.isDummyResolve(resolveResults)) {
                    var10000 = new ResolveResult[]{new JSResolveResult(this.myRef)};

                    return var10000;
                } else {
                    return resolveResults;
                }
            }
        } else {
            var10000 = ResolveResult.EMPTY_ARRAY;
            return var10000;
        }
    }

    private boolean isDummyResolve(ResolveResult[] results) {
        if (this.undefinedResolve(results)) {
            return true;
        } else {
            return results != null && results.length != 0 ? false : this.isGlobalThis();
        }
    }

    private ResolveResult[] doResolveReference(boolean incompleteCode) {
        assert this.myReferencedName != null;

        TypeScriptResolveProcessor<ResolveResultSink> localProcessor = (TypeScriptResolveProcessor) createLocalResolveProcessor(new ResolveResultSink(this.myRef, this.myReferencedName, false, incompleteCode));
        boolean isTypeContext = JSResolveUtil.isExprInTypeContext(this.myRef);
        boolean excludeJSLibsForIndices = false;
        ResolveResult[] results;
        if (!this.myUnqualifiedOrLocalResolve || this.myQualifier instanceof JSThisExpression && !TypeScriptUtil.isValidThisQualifier(this.myQualifier) || this.myQualifier instanceof JSSuperExpression && !TypeScriptUtil.isValidSuperQualifier(this.myQualifier)) {
            localProcessor.setTypeContext(isTypeContext);
            JSTypeEvaluator.evaluateTypes(this.myQualifier, this.myContainingFile, localProcessor);
            ResolveResultSink resultSink = localProcessor.getResultSink();
            JSResolveResult candidateResult = resultSink.getCandidateResult();
            if (localProcessor.resolved.isSuitableForReferenceResolve() || resultSink.getCompleteResult() != null || candidateResult != null && candidateResult.getResolveProblemKey() != null) {
                results = localProcessor.getResultsAsResolveResults();
                if (results.length > 0 || localProcessor.resolved != TypeResolveState.ResolvedAllowsExtras) {
                    return results;
                }

                excludeJSLibsForIndices = true;
            }

            ResolveResult[] resultsFromProviders = this.resolveFromProviders();
            if (resultsFromProviders != null) {
                return resultsFromProviders;
            } else {
                results = this.resolveFromIndices(localProcessor, excludeJSLibsForIndices, isTypeContext);
                return this.postProcessIndexResults(results);
            }
        } else {
            localProcessor.setToProcessHierarchy(true);
            boolean strictTypeContext = localProcessor.isStrictTypeContext();
            if (strictTypeContext) {
                this.processLocalDeclarationsByExportScopes(localProcessor, this.myRef);
            } else {
                this.processLocalDeclarations(localProcessor, this.myRef, false, null);
            }

            results = localProcessor.getResultsAsResolveResults();
            PsiElement complete = localProcessor.getResultSink().getCompleteResult();
            if (complete != null) {
                return results;
            } else {
                if (!(this.myQualifier instanceof JSThisExpression) && !(this.myQualifier instanceof JSSuperExpression)) {
                    TypeScriptResolveHelper.processGlobalThings(localProcessor, ResolveState.initial(), this.myRef);
                }

                return localProcessor.getResultsAsResolveResults();
            }
        }
    }

    private void processLocalDeclarationsByExportScopes(@NotNull TypeScriptResolveProcessor<ResolveResultSink> localProcessor, @NotNull JSElement startElement) {

        for (PsiElement scope = startElement; scope != null; scope = getExportedScopeOrContext(scope)) {
            if (!(scope instanceof JSExpressionCodeFragment) && isExportedScope(scope)) {
                if (!scope.processDeclarations(localProcessor, ResolveState.initial(), scope, this.myRef)) {
                    break;
                }
            } else {
                PsiElement context = scope;
                scope = getExportedScopeOrContext(scope);
                if (scope instanceof JSExpressionCodeFragment) {
                    scope = null;
                }

                this.processLocalDeclarations(localProcessor, context, true, scope);
                if (localProcessor.getResultSink().getCompleteResult() != null) {
                    break;
                }
            }
        }

    }

    @Nullable
    private static PsiElement getExportedScopeOrContext(@Nullable PsiElement stopAt) {
        if (stopAt == null) {
            return null;
        } else {
            PsiElement stopElement = JSResolveUtil.getContext(stopAt);
            if (stopElement != null) {
                return stopElement;
            } else if (stopAt instanceof PsiFile) {
                return null;
            } else {
                NavigatablePsiElement type;
                for (type = getParentCandidate(stopAt); type != null && !isExportedScope(type); type = getParentCandidate(type)) {
                    if (isExportedScope(type.getContext())) {
                        return type;
                    }
                }

                return type;
            }
        }
    }

    @Nullable
    private static NavigatablePsiElement getParentCandidate(@Nullable PsiElement stopAt) {
        return (NavigatablePsiElement) PsiTreeUtil.getParentOfType(stopAt, new Class[]{JSStatement.class, JSExecutionScope.class, JSClass.class, PsiFile.class});
    }

    private static boolean isExportedScope(@Nullable PsiElement type) {
        return type instanceof TypeScriptModule || type instanceof PsiFile || ES6PsiUtil.isEmbeddedModule(type);
    }

    private void processLocalDeclarations(@NotNull TypeScriptResolveProcessor<ResolveResultSink> localProcessor, @NotNull PsiElement context, boolean strictTypeContext, @Nullable PsiElement stopAt) {

        JSReferenceExpressionImpl.doProcessLocalDeclarations(context, this.myQualifier, localProcessor, false, false, strictTypeContext, stopAt);
    }
}

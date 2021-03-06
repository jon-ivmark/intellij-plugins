/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.flask.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.templateLanguages.TemplateContextProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class FlaskTemplateContextProvider implements TemplateContextProvider {

  @Override
  public Collection<LookupElement> getTemplateContext(PsiFile template) {
    if (PyPsiFacade.getInstance(template.getProject()).qualifiedNameResolver("flask").fromElement(template).firstResult() == null) {
      return null;
    }
    List<LookupElement> result = new ArrayList<LookupElement>();
    List<PyStringLiteralExpression> references = FlaskTemplateManager.findTemplateReferences(template);
    if (references.isEmpty()) {
      return null;
    }
    for (PyStringLiteralExpression reference : references) {
      PyCallExpression call = PsiTreeUtil.getParentOfType(reference, PyCallExpression.class);
      assert call != null;
      for (PyExpression arg : call.getArguments()) {
        if (arg instanceof PyKeywordArgument) {
          PyKeywordArgument keywordArgument = (PyKeywordArgument)arg;
          String keyword = keywordArgument.getKeyword();
          PsiElement value = keywordArgument.getValueExpression();
          if (keyword != null && value != null) {
            result.add(LookupElementBuilder.create(keywordArgument, keyword));
          }
        }
      }
    }
    PyPsiFacade psiFacade = PyPsiFacade.getInstance(template.getProject());
    PsiElement flaskModule = psiFacade.qualifiedNameResolver(FlaskNames.FLASK_MODULE).fromElement(template).firstResult();
    if (flaskModule instanceof PsiDirectory) {
      flaskModule = ((PsiDirectory)flaskModule).findFile(PyNames.INIT_DOT_PY);
    }
    if (flaskModule instanceof PyFile) {
      // _default_template_ctx_processor() in flask/templating.py
      addFlaskVariable(result, (PyFile) flaskModule, FlaskNames.REQUEST);
      addFlaskVariable(result, (PyFile) flaskModule, FlaskNames.SESSION);
      addFlaskVariable(result, (PyFile) flaskModule, FlaskNames.G);

      PyClass flaskClass = PyPsiFacade.getInstance(flaskModule.getProject()).findClass(FlaskNames.FLASK_FQN);
      if (flaskClass != null) {
        PyTargetExpression attribute = flaskClass.findInstanceAttribute(FlaskNames.CONFIG, false);
        if (attribute != null) {
          result.add(LookupElementBuilder.createWithIcon(attribute));
        }
      }

      // create_jinja_environment() in flask/app.py
      addFlaskVariable(result, (PyFile) flaskModule, FlaskNames.URL_FOR);
      addFlaskVariable(result, (PyFile) flaskModule, FlaskNames.GET_FLASHED_MESSAGES);
    }
    return result;
  }

  private static void addFlaskVariable(List<LookupElement> result, PyFile module, String name) {
    PsiElement named = module.getElementNamed(name);
    if (named instanceof PsiNamedElement) {
      result.add(LookupElementBuilder.createWithIcon((PsiNamedElement)named));
    }
  }
}

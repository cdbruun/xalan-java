/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.xalan.extensions;

import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.xalan.templates.ElemApplyTemplates;
import org.apache.xalan.templates.ElemCallTemplate;
import org.apache.xalan.templates.ElemChoose;
import org.apache.xalan.templates.ElemForEach;
import org.apache.xalan.templates.ElemIf;
import org.apache.xalan.templates.ElemLiteralResult;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.ElemWhen;
import org.apache.xalan.transformer.TransformerImpl;

/**
 * @version $Id: $
 */
@SuppressWarnings("serial")
public class TransformerContextException extends TransformerException {
     private ExpressionContext _exprContext;
     
     private String _xsltStack;
     public TransformerContextException(ExpressionContext ec, Throwable e) {
         super(e.getMessage(), e);
         _exprContext = ec;
         _xsltStack = TransformerContextException.xsltStackTrace(_exprContext);
     }
     
     // The ExpressionContext is not really useful when you have left the transformation.
     // You can use it in the ErrorListener on the transform
     public ExpressionContext getExpressionContext() {
         return _exprContext;
     }
     
     public String getXsltStack() {
         return _xsltStack;
     }
     
     public void setXsltStack(String xsltStack) {
         _xsltStack = xsltStack;
     }
     
     public TransformerImpl getTransformerImpl() {
         try  {
             TransformerImpl trans = (_exprContext != null) ?
                     (TransformerImpl)_exprContext.getXPathContext().getOwnerObject() : null;
                     return trans;
         }
         catch (Throwable t) {

         }
         return null; 
     }
     
     public void setExpressionContext(ExpressionContext ec) {
         _exprContext = ec;
     }
     
     
     public static String xsltStackTrace(ExpressionContext ec) {
         StringBuilder st = new StringBuilder();
         try {
             TransformerImpl t = (TransformerImpl)ec.getXPathContext().getOwnerObject();
               Vector cb = t.getElementCallstack();
                 for(int i=0; i<cb.size(); i++) {
                     /*if (i > 0) {
                         st.append("\n");
                     }*/
                     ElemTemplateElement et = (ElemTemplateElement)cb.get(i);
                     if (et.getSystemId() != null) {
                         int idx = et.getSystemId().indexOf("Transformations");
                         String lname = et.getLocalName();
                         if (et instanceof ElemTemplate) {
                             ElemTemplate template = (ElemTemplate)et;
                             StringBuilder tmp = new StringBuilder();
                             if (null != template.getName()) {
                                 tmp.append(template.getName());
                             }
                             if (null != template.getMatch()) {
                                 tmp.append("match=").append(template.getMatch().getPatternString());
                             }
                             if (null != template.getMode()) {
                                 tmp.append(" mode=").append(template.getMode());
                             }
                             
                             if (null != template.getMatch()) {
                                st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber() +":template"+ " "+tmp.toString());
                                st.append("\n");
                             }
                         }
                         else if (et instanceof ElemCallTemplate) {
                             ElemCallTemplate calltemplate = (ElemCallTemplate)et;
                             // the Root call always has a null name
                             if (null != calltemplate.getName()) {
                                 st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":call "+ calltemplate.getName());
                                 st.append("\n");
                             }
                         }
                         else if (et instanceof ElemApplyTemplates) {
                             st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":apply-template");
                             st.append("\n");
                         }                
                         else if (et instanceof ElemIf) {
                             ElemIf eIf = (ElemIf)et; 
                             st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":"+eIf.getLocalName() + " test="+eIf.getTest().getPatternString() );
                             st.append("\n");
                         }
                         else if (et instanceof ElemWhen) {
                             ElemWhen eWhen = (ElemWhen)et; 
                             st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":"+eWhen.getLocalName() + " test="+eWhen.getTest().getPatternString() );
                             st.append("\n");
                         }
                         
                         else if (et instanceof ElemLiteralResult) {
                             //
                         }
                         else if (et instanceof ElemVariable) {
                             //
                         }
                         else if (et instanceof ElemChoose) {
                             //
                         }
                         else if (et instanceof ElemForEach) {
                             ElemForEach eFor = (ElemForEach)et; 
                             st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":"+eFor.getLocalName() + " select="+eFor.getSelect().toString());
                             st.append("\n");
                         }
                         
                         else {
                             st.append(et.getSystemId().substring(idx)+":"+et.getLineNumber()  +":"+ lname + ":"+et.getClass().toString());
                             st.append("\n");
                         }
                     }
                     else {
                         //st.append("no systemid:"+ et.getNodeType() + ":" + et.getLineNumber());
                     }
                 }
             
             
         } catch (Throwable e) {
             // THIS MUST NEVER FAIL
             
         }
         return st.toString();
     }
     
}

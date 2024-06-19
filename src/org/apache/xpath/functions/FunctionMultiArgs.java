/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id$
 */
package org.apache.xpath.functions;

import java.util.Iterator;

import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XPathArray;
import org.apache.xpath.objects.XPathMap;
import org.apache.xpath.res.XPATHErrorResources;
import org.json.JSONArray;
import org.json.JSONObject;

import xml.xpath31.processor.types.XSBoolean;
import xml.xpath31.processor.types.XSDecimal;
import xml.xpath31.processor.types.XSString;

/**
 * Base class for functions that accept an undetermined number of multiple
 * arguments.
 * @xsl.usage advanced
 */
public class FunctionMultiArgs extends Function3Args
{
    static final long serialVersionUID = 7117257746138417181L;

  /** Argument expressions that are at index 3 or greater.
   *  @serial */
  Expression[] m_args;
  
  /**
   * Return an expression array containing arguments at index 3 or greater.
   *
   * @return An array that contains the arguments at index 3 or greater.
   */
  public Expression[] getArgs()
  {
    return m_args;
  }

  /**
   * Set an argument expression for a function.  This method is called by the
   * XPath compiler.
   *
   * @param arg non-null expression that represents the argument.
   * @param argNum The argument number index.
   *
   * @throws WrongNumberArgsException If a derived class determines that the
   * number of arguments is incorrect.
   */
  public void setArg(Expression arg, int argNum)
          throws WrongNumberArgsException
  {

    if (argNum < 3)
      super.setArg(arg, argNum);
    else
    {
      if (null == m_args)
      {
        m_args = new Expression[1];
        m_args[0] = arg;
      }
      else
      {

        // Slow but space conservative.
        Expression[] args = new Expression[m_args.length + 1];

        System.arraycopy(m_args, 0, args, 0, m_args.length);

        args[m_args.length] = arg;
        m_args = args;
      }
      arg.exprSetParent(this);
    }
  }
  
  /**
   * This function is used to fixup variables from QNames to stack frame 
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list 
   * should be searched backwards for the first qualified name that 
   * corresponds to the variable reference qname.  The position of the 
   * QName in the vector from the start of the vector will be its position 
   * in the stack frame (but variables above the globalsTop value will need 
   * to be offset to the current stack frame).
   */
  public void fixupVariables(java.util.Vector vars, int globalsSize)
  {
    super.fixupVariables(vars, globalsSize);
    if(null != m_args)
    {
      for (int i = 0; i < m_args.length; i++) 
      {
        m_args[i].fixupVariables(vars, globalsSize);
      }
    }
  }

  /**
   * Check that the number of arguments passed to this function is correct.
   *
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException{}

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.  This class supports an arbitrary
   * number of arguments, so this method must never be called.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
    String fMsg = XSLMessages.createXPATHMessage(
        XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION,
        new Object[]{ "Programmer's assertion:  the method FunctionMultiArgs.reportWrongNumberArgs() should never be called." });

    throw new RuntimeException(fMsg);
  }

  /**
   * Tell if this expression or it's subexpressions can traverse outside
   * the current subtree.
   *
   * @return true if traversal outside the context node's subtree can occur.
   */
  public boolean canTraverseOutsideSubtree()
  {

    if (super.canTraverseOutsideSubtree())
      return true;
    else
    {
      int n = m_args.length;

      for (int i = 0; i < n; i++)
      {
        if (m_args[i].canTraverseOutsideSubtree())
          return true;
      }

      return false;
    }
  }
  
  class ArgMultiOwner implements ExpressionOwner
  {
  	int m_argIndex;
  	
  	ArgMultiOwner(int index)
  	{
  		m_argIndex = index;
  	}
  	
    /**
     * @see ExpressionOwner#getExpression()
     */
    public Expression getExpression()
    {
      return m_args[m_argIndex];
    }


    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    public void setExpression(Expression exp)
    {
    	exp.exprSetParent(FunctionMultiArgs.this);
    	m_args[m_argIndex] = exp;
    }
  }

   
    /**
     * @see org.apache.xpath.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
     */
    public void callArgVisitors(XPathVisitor visitor)
    {
      super.callArgVisitors(visitor);
      if (null != m_args)
      {
        int n = m_args.length;
        for (int i = 0; i < n; i++)
        {
          m_args[i].callVisitors(new ArgMultiOwner(i), visitor);
        }
      }
    }
    
    /**
     * Given an object of type org.json.JSONObject or org.json.JSONArray,
     * construct either an XDM 'map', or an XDM 'array' from these objects.   
     * 
     * @param jsonObj   an input argument of type org.json.JSONObject, or 
     *                  org.json.JSONArray.  
     * @return          an XDM object of type XPathMap, or XPathArray 
     */
    protected XObject getXdmMapOrArrayFromNativeJson(Object jsonObj) {
    	
    	XObject result = null;
    	
    	if (jsonObj instanceof JSONObject) {
    		result = new XPathMap();
	    	Iterator<String> jsonKeys = ((JSONObject)jsonObj).keys();
	    	
	    	while (jsonKeys.hasNext()) {
	      	   String key = jsonKeys.next();
	      	   Object value = ((JSONObject)jsonObj).get(key);        	  
	      	   if (value instanceof String) {
	      		 ((XPathMap)result).put(new XSString(key), new XSString(String.valueOf(value)));  
	      	   }
	      	   else if (value instanceof Number) {
	      		  double doubleVal = ((Number)value).doubleValue();
	      		  ((XPathMap)result).put(new XSString(key), new XSDecimal(String.valueOf(doubleVal)));
	      	   }
	      	   else if (value instanceof Boolean) {
	      		  ((XPathMap)result).put(new XSString(key), new XSBoolean(new Boolean(value.toString()))); 
	      	   }	      	   
	      	   else if (value instanceof JSONObject) {
	      		  // Recursive call to this function
	      		  XObject value1 = getXdmMapOrArrayFromNativeJson(value);
	      		  ((XPathMap)result).put(new XSString(key), value1);
	      	   }
	      	   else if (value instanceof JSONArray) {
	      		  XPathArray xpathArr = new XPathArray();
	      		  
	      		  JSONArray jsonArr = (JSONArray)value;	      		  
	      		  int arrLen = jsonArr.length();
	      		  for (int idx = 0; idx < arrLen; idx++) {
	      			 Object arrItem = jsonArr.get(idx);
	      			 XObject xObj = null;
	      			 if (arrItem instanceof String) {
	      				xObj = new XSString(arrItem.toString());	 
	      			 }
	      			 else if (arrItem instanceof Number) {
	      				double doubleVal = ((Number)arrItem).doubleValue();
	      				xObj = new XSDecimal(String.valueOf(doubleVal)); 
	      			 }
	      			 else if (arrItem instanceof Boolean) {
	      				xObj = new XSBoolean(new Boolean(arrItem.toString())); 
	      			 }
	      			 else if ((arrItem instanceof JSONObject) || (arrItem instanceof JSONArray)) {
	      			   // Recursive call to this function
	      				xObj = getXdmMapOrArrayFromNativeJson(arrItem);
	      			 }
	      			 
	      			xpathArr.add(xObj);
	      		  }
	      		  
	      		  ((XPathMap)result).put(new XSString(key), xpathArr);
	      	   }
	        }
    	}
    	else if (jsonObj instanceof JSONArray) {
    		XPathArray xpathArr = new XPathArray();
    		
    		JSONArray jsonArr = (JSONArray)jsonObj;	      		  
    		int arrLen = jsonArr.length();
    		for (int idx = 0; idx < arrLen; idx++) {
    		   Object arrItem = jsonArr.get(idx);
    		   XObject xObj = null;
    		   if (arrItem instanceof String) {
    		      xObj = new XSString(arrItem.toString());	 
    		   }
    		   else if (arrItem instanceof Number) {
    		      double doubleVal = ((Number)arrItem).doubleValue();
    		      xObj = new XSDecimal(String.valueOf(doubleVal)); 
    		   }
    		   else if (arrItem instanceof Boolean) {
    		      xObj = new XSBoolean(new Boolean(arrItem.toString())); 
    		   }
    		   else if ((arrItem instanceof JSONObject) || (arrItem instanceof JSONArray)) {
    			  // Recursive call to this function
    		      xObj = getXdmMapOrArrayFromNativeJson(arrItem);
    		   }
    			 
    		   xpathArr.add(xObj);
    		}
    		  
    		result = xpathArr;
    	}
    	
    	return result;
    }
    
    /**
     * @see Expression#deepEquals(Expression)
     */
    public boolean deepEquals(Expression expr)
    {
      if (!super.deepEquals(expr))
            return false;

      FunctionMultiArgs fma = (FunctionMultiArgs) expr;
      if (null != m_args)
      {
        int n = m_args.length;
        if ((null == fma) || (fma.m_args.length != n))
              return false;

        for (int i = 0; i < n; i++)
        {
          if (!m_args[i].deepEquals(fma.m_args[i]))
                return false;
        }

      }
      else if (null != fma.m_args)
      {
          return false;
      }

      return true;
    }
}

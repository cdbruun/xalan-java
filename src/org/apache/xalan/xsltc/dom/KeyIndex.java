/*
 * @(#)$Id$
 *
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, Sun
 * Microsystems., http://www.sun.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * @author Morten Jorgensen
 *
 */

package org.apache.xalan.xsltc.dom;

import java.util.Vector;
import java.util.StringTokenizer;

import org.apache.xalan.xsltc.DOM;
import org.apache.xalan.xsltc.NodeIterator;
import org.apache.xalan.xsltc.runtime.Hashtable;

public class KeyIndex implements NodeIterator {
    private Hashtable _index = new Hashtable();
    private BitArray  _nodes = null;
    private int       _pos = 0;
    private int       _mark = 0;
    private int       _save = 0;
    private int       _start = 0;
    private int       _arraySize = 0;
    private int       _node = -1;

    /**
     * Creates an index for a key defined by xsl:key
     */
    public KeyIndex(int size) {
	_arraySize = size;
    }
 
    /**
     * Adds a node to the node list for a given value.
     * The BitArray object makes sure duplicate nodes are eliminated.
     */
    public void add(Object value, int node) {
	if ((_nodes = (BitArray)_index.get(value)) == null) {
	    _nodes = new BitArray(_arraySize);
	    _nodes.setMask(node & 0xff000000);
	    _index.put(value,_nodes);
	}
	_nodes.setBit(node & 0x00ffffff);

	/*
	 * TODO: A bit array can currently only hold nodes from one DOM.
	 * An index will therefore only return nodes from a single document.
	 */
    }

    /**
     * Merge this node set with nodes from another index
     */
    public void merge(KeyIndex other) {
	// Only merge if other node set is not empty
	if (other != null) {
	    if (other._nodes != null) {
		// Create new Vector for nodes if this set is empty
		if (_nodes == null)
		    _nodes = other._nodes;
		else
		    _nodes = _nodes.merge(other._nodes);
	    }
	}
    }

    /**
     * This method must be called by the code generated by the id() function
     * prior to returning the node iterator. The lookup code for key() and
     * id() differ in the way the lookup value can be whitespace separated
     * list of tokens for the id() function, but a single string for the
     * key() function.
     */
    public void lookupId(Object value) {
	if (value instanceof String) {
	    final String string = (String)value;
	    if (string.indexOf(' ') > -1) {
		StringTokenizer values = new StringTokenizer(string);
		while (values.hasMoreElements()) {
		    BitArray nodes = (BitArray)_index.get(values.nextElement());
		    if (nodes != null) {
			if (_nodes == null)
			    _nodes = nodes;
			else
			    _nodes = _nodes.merge(nodes);
		    }
		}
		return;
	    }
	}
	_nodes = (BitArray)_index.get(value);
    }

    /**
     * This method must be called by the code generated by the key() function
     * prior to returning the node iterator.
     */
    public void lookupKey(Object value) {
	_nodes = (BitArray)_index.get(value);
    }

    /** 
     * Callers should not call next() after it returns END.
     */
    public int next() {
	if (_nodes == null) return(END);
	if ((_node = _nodes.getNextBit(++_node)) == END) return(END);
	_pos++;
	return(_node | _nodes.getMask());
    }

    public int containsID(int node, Object value) { 
	if (value instanceof String) {
	    final String string = (String)value;
	    if (string.indexOf(' ') > -1) {
		StringTokenizer values = new StringTokenizer(string);
		while (values.hasMoreElements()) {
		    BitArray nodes = (BitArray)_index.get(values.nextElement());
		    if ((nodes != null) && (nodes.getBit(node))) return(1);
		}
		return(0);
	    }
	}

	BitArray nodes = (BitArray)_index.get(value);
	if ((nodes != null) && (nodes.getBit(node))) return(1);
	return(0);
    }

    public int containsKey(int node, Object value) { 
	BitArray nodes = (BitArray)_index.get(value);
	if ((nodes != null) && (nodes.getBit(node))) return(1);
	return(0);
    }

    /**
     * Resets the iterator to the last start node.
     */
    public NodeIterator reset() {
	_pos = _start;
	_node = _start - 1;
	return(this);
    }

    /**
     * Returns the number of elements in this iterator.
     */
    public int getLast() {
	if (_nodes == null)
	    return(0);
	else
	    return(_nodes.size()); // TODO: count actual nodes
    }

    /**
     * Returns the position of the current node in the set.
     */
    public int getPosition() {
	return(_pos);
    }

    /**
     * Remembers the current node for the next call to gotoMark().
     */
    public void setMark() {
	_mark = _pos;
	_save = _node;
    }

    /**
     * Restores the current node remembered by setMark().
     */
    public void gotoMark() {
	_pos = _mark;
	_node = _save;
    }

    /** 
     * Set start to END should 'close' the iterator, 
     * i.e. subsequent call to next() should return END.
     */
    public NodeIterator setStartNode(int start) {
	if (start == END) {
	    _nodes = null;
	}
	else if (_nodes != null) {
	    // Node count starts with 1, while bit arrays count from 0. Must
	    // subtract one from 'start' to initialize bit array correctly.
	    _start = _nodes.getBitNumber(start-1); 
	    _node = _start - 1;
	}
	return((NodeIterator)this);
    }

    /**
     * True if this iterator has a reversed axis.
     */
    public boolean isReverse() {
	return(false);
    }

    /**
     * Returns a deep copy of this iterator.
     */
    public NodeIterator cloneIterator() {
	return((NodeIterator)this);
    }

}

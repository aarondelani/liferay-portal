/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.kernel.xml;

import java.util.Iterator;
import java.util.List;

/**
 * <a href="Element.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public interface Element extends Branch {

	public void add(Attribute attribute);

	public void add(CDATA cdata);

	public void add(Entity entity);

	public void add(Namespace namespace);

	public void add(Text text);

	public Element addAttribute(QName qName, String value);

	public Element addAttribute(String name, String value);

	public Element addCDATA(String cdata);

	public Element addComment(String comment);

	public Element addEntity(String name, String text);

	public List<Namespace> additionalNamespaces();

	public Element addNamespace(String prefix, String uri);

	public Element addText(String text);

	public void appendAttributes(Element element);

	public Attribute attribute(int index);

	public Attribute attribute(QName qName);

	public Attribute attribute(String name);

	public int attributeCount();

	public Iterator<Attribute> attributeIterator();

	public List<Attribute> attributes();

	public String attributeValue(QName qName);

	public String attributeValue(QName qName, String defaultValue);

	public String attributeValue(String name);

	public String attributeValue(String name, String defaultValue);

	public Element createCopy();

	public Element createCopy(QName qName);

	public Element createCopy(String name);

	public List<Namespace> declaredNamespaces();

	public Element element(QName qName);

	public Element element(String name);

	public Iterator<Element> elementIterator();

	public Iterator<Element> elementIterator(QName qName);

	public Iterator<Element> elementIterator(String name);

	public List<Element> elements();

	public List<Element> elements(QName qName);

	public List<Element> elements(String name);

	public String elementText(QName qName);

	public String elementText(String name);

	public String elementTextTrim(QName qName);

	public String elementTextTrim(String name);

	public Object getData();

	public Namespace getNamespace();

	public Namespace getNamespaceForPrefix(String prefix);

	public Namespace getNamespaceForURI(String uri);

	public String getNamespacePrefix();

	public List<Namespace> getNamespacesForURI(String uri);

	public String getNamespaceURI();

	public QName getQName();

	public QName getQName(String qualifiedName);

	public String getQualifiedName();

	public String getStringValue();

	public String getText();

	public String getTextTrim();

	public Node getXPathResult(int index);

	public boolean hasMixedContent();

	public boolean isRootElement();

	public boolean isTextOnly();

	public boolean remove(Attribute attribute);

	public boolean remove(CDATA cdata);

	public boolean remove(Entity entity);

	public boolean remove(Namespace namespace);

	public boolean remove(Text text);

	public void setAttributes(List<Attribute> attributes);

	public void setData(Object data);

	public void setQName(QName qName);

}
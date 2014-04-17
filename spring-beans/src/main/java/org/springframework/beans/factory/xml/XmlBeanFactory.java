/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * <h3>���� XML ���õ� Bean ����</h3>
 * <p>
 * רΪ������� XML �� Spring Ӧ�ã����Դ�������ʵ��ʱ������Ҫ���������� Bean ����� Resource ʵ���ģ���������� XML �����ļ��С�
 * </p>
 * <p>
 * ʵ�ַ��棬���ಢû�ж����Լ������߼��ķ��������Ǹ��� DefaultListableBeanFactory �� XmlBeanDefinitionReader �Ľ�ϡ��ڲ�ֻӵ��һ��˽�е�
 * XmlBeanDefinitionReader ����������� BeanDefinitionRegistry ������Ϊ����ʵ������ ֻ�ڳ�ʼ��ʱִ�н��� XML �ļ��еĹ��� ��ע�� Bean �������ǴӸ���
 * DefaultListableBeanFactory �̳ж��������Ա����ѱ����Ϊ Deprecated �����ġ�
 * </p>
 * <h4>�ο���</h4>
 * <ul>
 * <li>���� XML �����ļ��� Resource ʵ����{@link Resource} �ӿڡ� </li>
 * <li>XML �����ļ��Ľ����ࣺ{@link XmlBeanDefinitionReader} ���� {@link XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry) XmlBeanDefinitionReader(BeanDefinitionRegistry)} ��</li>
 * <li>XML �����ļ��Ľ���������{@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource) } ��</li>
 * <li>ע�� Bean ������ࣺ {@link DefaultListableBeanFactory} ��</li>
 * <li>����Ψһ���й��ܵĵط���{@link #XmlBeanFactory(Resource, BeanFactory)} ��</li>
 * </ul>
 * <hr>
 * Convenience extension of {@link DefaultListableBeanFactory} that reads bean definitions
 * from an XML document. Delegates to {@link XmlBeanDefinitionReader} underneath;
 * effectively equivalent to using an XmlBeanDefinitionReader with a
 * DefaultListableBeanFactory.
 * 
 * <p>
 * The structure, element and attribute names of the required XML document are hard-coded
 * in this class. (Of course a transform could be run if necessary to produce this
 * format). "beans" doesn't need to be the root element of the XML document: This class
 * will parse all bean definition elements in the XML file.
 * 
 * <p>
 * This class registers each bean definition with the {@link DefaultListableBeanFactory}
 * superclass, and relies on the latter's implementation of the {@link BeanFactory}
 * interface. It supports singletons, prototypes, and references to either of these kinds
 * of bean. See {@code "spring-beans-3.x.xsd"} (or historically,
 * {@code "spring-beans-2.0.dtd"}) for details on options and configuration style.
 * 
 * <p>
 * <b>For advanced needs, consider using a {@link DefaultListableBeanFactory} with an
 * {@link XmlBeanDefinitionReader}.</b> The latter allows for reading from multiple XML
 * resources and is highly configurable in its actual XML parsing behavior.
 * 
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 15 April 2001
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see XmlBeanDefinitionReader
 * @deprecated as of Spring 3.1 in favor of {@link DefaultListableBeanFactory} and
 *             {@link XmlBeanDefinitionReader}
 */
@Deprecated
@SuppressWarnings({ "serial", "all" })
public class XmlBeanFactory extends DefaultListableBeanFactory {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

	/**
	 * <h3>��ʼ�� XmlBeanFactory</h3>
	 * <p>
	 * ��������ʵ������ָ�� {@link BeanFactory} ʱĬ��Ϊ null , Ȼ���� {@link #XmlBeanFactory(Resource, BeanFactory)} ��ʼ����
	 * </p>
	 * <hr>
	 * <p>
	 * Create a new XmlBeanFactory with the given resource, which must be parsable using
	 * DOM.
	 * </p>
	 * <p>
	 * ����һ���µĶ������ݴ��ݸ��Ĳ�����������������������ܱ������ɿ��õ� DOM ��
	 * </p>
	 * 
	 * @param resource XML resource to load bean definitions from
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * <h3>��ʼ�� XmlBeanFactory</h3>
	 * <p>
	 * ����Ψһ���й��ܵĵط���ֻ��������1������ {@link BeanFactory} �������ø���ĳ�ʼ����2���ڲ� {@link XmlBeanDefinitionReader}
	 * ʵ�����ؾ��� Bean ����� {@link Resource} ������
	 * </p>
	 * <p>
	 * ��Ҫע����ǣ��ڲ� XmlBeanDefinitionReader ʵ�����䶨��ʱͬʱ����ʵ�����ģ��������˵�ǰʵ�����������ͽ����˺͸���
	 * {@link DefaultListableBeanFactory} �Ĺ��������Գ�ʼ���в�����Ҫ��������������� Resource ���������� Bean �����ļ���
	 * Resource ʵ���ģ������ļ������� XML �����ļ���
	 * </p>
	 * <h4>�ο���</h4>
	 * <ul>
	 * <li>����ĳ�ʼ����{@link DefaultListableBeanFactory#DefaultListableBeanFactory(BeanFactory)}</li>
	 * <li>���ض��� Bean �� XML ��Դ�ļ� �� {@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource)} ��</li>
	 * </ul>
	 * 
	 * <hr>
	 * <p>
	 * Create a new XmlBeanFactory with the given input stream, which must be parsable
	 * using DOM.
	 * </p>
	 * <p>
	 * ����һ���µĶ������ݴ��ݸ��Ĳ�����������������������ܱ������ɿ��õ� DOM ��
	 * </p>
	 * 
	 * @param resource XML resource to load bean definitions from
	 * @param parentBeanFactory parent bean factory
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory)
			throws BeansException {
		// ��Ҫ�Ǹ��� AbstractAutowireCapableBeanFactory ��ʼ����
		super(parentBeanFactory);
		// ���� XML �����ļ���
		this.reader.loadBeanDefinitions(resource);
	}

}

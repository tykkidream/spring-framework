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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * <p>zh°本类能注册并加载 bean，但这些能力都是从父类
 * {@link DefaultListableBeanFactory} 继承而来的，区别在于 bean 的配置信息（资源，由 {@link Resource} 封装）的格式的要求不同，父类对此没作限定，本类要求是具有配置信息的 XML 文件。
 * 
 * <p>zh°比其父类多出来的部分是拥有一个私有的 {@link XmlBeanDefinitionReader} 对象，在初始化时使用它读取 XML 文件，将资源（ XML 配置文件）传递给 {@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource) loadBeanDefinitions()} 执行处理。
 * 
 * <hr />
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
	 * <h3>参考资料：</h3>
	 * <ul>
	 * <li>使用到父类的初始化，可参考 {@link #AbstractAutowireCapableBeanFactory() AbstractAutowireCapableBeanFactory()} 。</li>
	 * </ul>
	 * <hr>
	 * <p>en°Create a new XmlBeanFactory with the given resource, which must be parsable using DOM.</p>
	 * <p>zh°创建一个新的对象，依据传递给的参数创建，这个参数必须是能被解析成可用的 DOM 。</p>
	 * 
	 * @param resource XML resource to load bean definitions from
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * <p>zh°这是本类唯一增加业务的地方，读取 XML 文件： this.reader.loadBeanDefinitions(resource); 。
	 * <h3>zh°参考资料：</h3>
	 * <ul>
	 * <li>zh°使用到父类的初始化，可参考 {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#AbstractAutowireCapableBeanFactory() AbstractAutowireCapableBeanFactory()} 。</li>
	 * <li>zh°读取配置文件，可参考 {@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource) loadBeanDefinitions()} 。</li>
	 * </ul>
	 * <hr>
	 * <p>en°Create a new XmlBeanFactory with the given input stream, which must be parsable using DOM.</p>
	 * <p>zh°创建一个新的对象，依据传递给的参数创建，这个参数必须是能被解析成可用的 DOM 。</p>
	 * 
	 * @param resource XML resource to load bean definitions from
	 * @param parentBeanFactory parent bean factory
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory)
			throws BeansException {
		// 主要是父类 AbstractAutowireCapableBeanFactory 初始化。
		super(parentBeanFactory);
		// 加载 XML 配置文件。
		this.reader.loadBeanDefinitions(resource);
	}

}

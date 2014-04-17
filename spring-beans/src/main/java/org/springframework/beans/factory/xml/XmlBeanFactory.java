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
 * <h3>基于 XML 配置的 Bean 工厂</h3>
 * <p>
 * 专为处理基于 XML 的 Spring 应用，所以创建本类实例时，至少要传递描述了 Bean 定义的 Resource 实例的，定义必须在 XML 配置文件中。
 * </p>
 * <p>
 * 实现方面，本类并没有定义自己独有逻辑的方法，仅是父类 DefaultListableBeanFactory 和 XmlBeanDefinitionReader 的结合。内部只拥有一个私有的
 * XmlBeanDefinitionReader 对象，它所需的 BeanDefinitionRegistry 参数就为本类实例自身， 只在初始化时执行解析 XML 文件夹的功能 ；注册 Bean 的能力是从父类
 * DefaultListableBeanFactory 继承而来，所以本类已被标记为 Deprecated 废弃的。
 * </p>
 * <h4>参考：</h4>
 * <ul>
 * <li>描述 XML 配置文件的 Resource 实例：{@link Resource} 接口。 </li>
 * <li>XML 配置文件的解析类：{@link XmlBeanDefinitionReader} 及其 {@link XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry) XmlBeanDefinitionReader(BeanDefinitionRegistry)} 。</li>
 * <li>XML 配置文件的解析方法：{@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource) } 。</li>
 * <li>注册 Bean 定义的类： {@link DefaultListableBeanFactory} 。</li>
 * <li>本类唯一运行功能的地方：{@link #XmlBeanFactory(Resource, BeanFactory)} 。</li>
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
	 * <h3>初始化 XmlBeanFactory</h3>
	 * <p>
	 * 创建本类实例，不指定 {@link BeanFactory} 时默认为 null , 然后由 {@link #XmlBeanFactory(Resource, BeanFactory)} 初始化。
	 * </p>
	 * <hr>
	 * <p>
	 * Create a new XmlBeanFactory with the given resource, which must be parsable using
	 * DOM.
	 * </p>
	 * <p>
	 * 创建一个新的对象，依据传递给的参数创建，这个参数必须是能被解析成可用的 DOM 。
	 * </p>
	 * 
	 * @param resource XML resource to load bean definitions from
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * <h3>初始化 XmlBeanFactory</h3>
	 * <p>
	 * 本类唯一运行功能的地方，只有两步：1、传递 {@link BeanFactory} 参数调用父类的初始化；2、内部 {@link XmlBeanDefinitionReader}
	 * 实例加载具有 Bean 定义的 {@link Resource} 参数。
	 * </p>
	 * <p>
	 * 需要注意的是，内部 XmlBeanDefinitionReader 实例是其定义时同时声明实例化的，并传递了当前实例自身，这样就建立了和父类
	 * {@link DefaultListableBeanFactory} 的关联，所以初始化中不再需要创建。另外这里的 Resource 参数必须是 Bean 定义文件的
	 * Resource 实例的，定义文件必须是 XML 配置文件。
	 * </p>
	 * <h4>参考：</h4>
	 * <ul>
	 * <li>父类的初始化：{@link DefaultListableBeanFactory#DefaultListableBeanFactory(BeanFactory)}</li>
	 * <li>加载定义 Bean 的 XML 资源文件 ： {@link XmlBeanDefinitionReader#loadBeanDefinitions(Resource)} 。</li>
	 * </ul>
	 * 
	 * <hr>
	 * <p>
	 * Create a new XmlBeanFactory with the given input stream, which must be parsable
	 * using DOM.
	 * </p>
	 * <p>
	 * 创建一个新的对象，依据传递给的参数创建，这个参数必须是能被解析成可用的 DOM 。
	 * </p>
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

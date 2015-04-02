/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface.
 * Reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code &lt;beans&gt;} doesn't need to be the root
 * element of the XML document: This class will parse all bean definition elements
 * in the XML file, not regarding the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	/**
	 * <h3>alias元素名称</h3>
	 * <p>
	 * 在对bean定义时，除了使用id属性来指定名称外，为了提供多个名称，可以使用alias标签来指定。
	 * 而所有的这些名称都指向同一个，在某些情况下别名非常有用 ，比如为了让应用的每一个组件能
	 * 更容易地对公共组件进行引用。
	 * </p>
	 * <p>
	 * 然而，在定义bean时就指定所有的别名并不总是恰当的。有时，我们期望能在当前位置为那些在别
	 * 处定义的bean引入别名。在XML配置文件中，可用单独的&lt;alias>元素来完成bean别名的定义。如：
	 * <pre>
	 * &lt;bean id="testBean" class="com.test"/>
	 * </pre>
	 * 要给这个JavaBean增加别名，以方便不同对象来调用。我们就可以直接使用bean标签中的name属性：
	 * <pre>
	 * &lt;bean id="testBean" class="com.test" name="testBean,testBean2"/>
	 * </pre>
	 * 同样，Spring还有另外一种声明别名的方式：
	 * <pre>
	 * &lt;bean id="testBean" class="com.test"/>
	 * &lt;alias name="testBean" alias="testBean,testBean2"/>
	 * </pre>
	 * 考虑一个更为具体的例子，组件A在XML配置文件中定义了一个名为componetA的DataSource类型的bean，
	 * 但组件B却想在其XML文件中以componentB全名来引用此bean。而且在主程序MyApp的XML配置文件中，
	 * 希望以myApp的名字来引用此bean。最后容器加载3个XML来生成最终的ApplicationContext。在此情况下，
	 * 可通过在配置文件中添加下列alias元素来实现：
	 * <pre>
	 * &lt;alias name="componentA" alias="componentB"/>
	 * &lt;alias name="componentA" alias="myApp"/>
	 * </pre>
	 * 这样一来，每个组件及主程序就可通过唯一名字来引用同一个数据源而互不干扰。
	 * </p>
	 * <p>所以别名的定义有两种方式，一种是bean元素的name属性，一种是alias元素。</p>
	 */
	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	/**
	 * <h3>import元素名称</h3>
	 * <p>当项目变得庞大时，会有太多的配置文件。可以分成多个模块多个配置文件，
	 * 而import元素可将多个文件联系在一起，集中到一个上下文中。
	 * <pre>
	 * &lt;import resource="customerContext.xml" />
	 * </pre>
	 * </p>
	 */
	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	/**
	 * <h3>profile元素名称</h3>
	 * <p>profile的用法：
	 * <p>当Spring的XML配置文件中有这样的两人beans：
	 * <pre>
	 * &lt;beans profile="dev">......&lt;/beans>
	 * &lt;beans profile="pro">......&lt;/beans>
	 * </pre>
	 * 这时，在web.xml中可以这样配置：
	 * <pre>
	 * &lt;context-param>
	 * &#9;&lt;param-name>Spring.profiles.active&lt;/param-name>
	 * &#9;&lt;param-value>dev&lt;/param-value>
	 * &lt;/context-param>
	 * </pre>
	 * 表示使用Spring的XML配置中名为dev的配置，其它忽略。这样就可以配置多个版本，比如用于生产环境的、开发环境的，
	 * 能方便地切换不同配置。
	 * <p>
	 */
	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	private Environment environment;

	private XmlReaderContext readerContext;

	private BeanDefinitionParserDelegate delegate;


	/**
	 * {@inheritDoc}
	 * <p>Default value is {@code null}; property is required for parsing any
	 * {@code <beans/>} element with a {@code profile} attribute present.
	 * @see #doRegisterBeanDefinitions
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * <p>加载注册Bean。
	 * <hr>
	 * 
	 * {@inheritDoc}
	 * <p>This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");
		// 提取XML的根结点。
		Element root = doc.getDocumentElement();
		// 将根结点作为参数继续注册。这里算是真正的开始解析了。
		doRegisterBeanDefinitions(root);
	}


	/**
	 * <p>真正地解析Bean。
	 * <hr>
	 * 
	 * Register each bean definition within the given root {@code <beans/>} element.
	 * @throws IllegalStateException if {@code <beans profile="..."} attribute is present
	 * and Environment property has not been set
	 * @see #setEnvironment
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		// 一、处理profile属性。
		String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
		// 1.1 检测beans节点是否定义了profile属性。
		if (StringUtils.hasText(profileSpec)) {
			// 如果定义了profile属性。
			// 1.2 检查环境变量是否可用。
			Assert.state(this.environment != null, "Environment must be set for evaluating profiles");
			// 1.3 在web.xml中，profile是参数是可以指定多个的，这里对其拆分。
			String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
					profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			// 1.4 检查当前处理的XML结点Element的profile是否同环境变量相同。
			if (!this.environment.acceptsProfiles(specifiedProfiles)) {
				// 如果不是web.xml中指定的配置，则退出解析，不浪费性能。
				return;
			}
		}

		// Any nested <beans> elements will cause recursion in this method. In
		// order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		// 专门处理解析。
		BeanDefinitionParserDelegate parent = this.delegate;
		this.delegate = createDelegate(this.readerContext, root, parent);

		// 解析前处理，留给子类实现。
		preProcessXml(root);
		parseBeanDefinitions(root, this.delegate);
		// 解析后处理，留给子类 实现。
		postProcessXml(root);

		this.delegate = parent;
	}

	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		BeanDefinitionParserDelegate delegate = createHelper(readerContext, root, parentDelegate);
		if (delegate == null) {
			delegate = new BeanDefinitionParserDelegate(readerContext, this.environment);
			delegate.initDefaults(root, parentDelegate);
		}
		return delegate;
	}

	@Deprecated
	protected BeanDefinitionParserDelegate createHelper(
			XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

		return null;
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
	 * source metadata from the supplied {@link Element}.
	 */
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}


	/**
	 * <p>
	 * <hr>
	 * 
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		if (delegate.isDefaultNamespace(root)) {
			// 如果根节点是默认的命名空间。
			// 获取根节点的子节点。
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				// 迭代得到一个子节点。
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						// 如果根节点、子节点是默认的命名空间。使用Spring自己所了解（默认）的方式解析。
						parseDefaultElement(ele, delegate);
					}
					else {
						// 对于Spring不认识的非默认的命名空间，使用自定义的方式解析（自定义的命名空间）。
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			// 对于Spring不认识的非默认的命名空间，使用自定义的方式解析（自定义的命名空间）。
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * <p>解析Spring的默认标签。分别是import、alias、bean、beans标签。
	 * 
	 * @param ele
	 * @param delegate
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 对import标签处理。
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// 对alias标签处理。
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// 对bean标签处理。
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
		// 对beans标签处理。
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			// 递归到前面的解析beans的方法，进行另一次解析。
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * <p>对import元素进行解析。
	 * <hr>
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获取resource属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// 如果不存在resource属性则不做任何处理。
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		// 解析系统属性，格式如：“${user.dir}”。
		location = environment.resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		// 判定location是绝对URI还时相对URI。
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		// 如果是绝对UIR则直接根据地址加载对应的配置文件。
		if (absoluteLocation) {
			try {
				// 递归调用bean的解析过程，进行另一次解析。
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// No URL -> considering resource location as relative to the current file.
			// 如果是相对URI则根据相对地址计算出绝对地址。
			try {
				int importCount;
				// Resource存在多个子实现类，如VfsResource、FileSystemResource等。
				// 而每个resource和createRelative方式实现都不一样，所以这里先使用子类的方法尝试解析。
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					// 递归调用bean的解析过程，进行另一次解析。
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					// 如果解析不成功，则使用默认的解析器ResourcePatternResolver进行解析。
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		// 解析进行监听器激活处理。
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * <p>对alias标签进行解析。
	 * <p>alias标签是别名元素，是定义别名的其中一种方式。
	 * 另外还有一种方式是在bean元素的name属性中定义，
	 * 而这种的解析是在对bean元素的解析中完成的。
	 * <hr>
	 * 
	 * Process the given alias element, registering the alias with the registry.
	 */
	protected void processAliasRegistration(Element ele) {
		// 获取beanName
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// 获取alias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				// 注册alias
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			// 别名注册后通知监听器做相应处理。
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * <p>对bean标签处理解析。
	 * <hr>
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// （一）对元素进行解析，得到bdHolder，包含配置中的各自属性，如class、name、id、alias等。
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// （二）当bdHolder不为空的情况下，子节点下若存在自定义属性，还需要对自定义标签进行解析。
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// （三）解析完成后，需要对解析后的bdHolder进行注册。
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// （四）发出事件，通知相关的监听器，这个bean已经加载完成。
			// Send registration event.
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * <p>解析前处理，留给子类实现。模版方法模式。
	 * <hr>
	 * 
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * <p>解析后处理，留给子类 实现。模版方法模式。
	 * <hr>
	 * 
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
